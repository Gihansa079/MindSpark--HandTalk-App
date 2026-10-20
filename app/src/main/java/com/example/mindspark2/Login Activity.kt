package com.example.mindspark2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.example.mindspark2.PreferencesManager

/**
 * LoginActivity: පරිශීලකයා පද්ධතියට ඇතුළු වීම (Login Verification),
 * Session සටහන් තබා ගැනීම සහ Local Database (Room) Update කිරීම සිදුකරන Activity එක.
 */
class LoginActivity : ComponentActivity() {

    // Network API ඇමතුම් සඳහා ApiService instance එක සාදා ගැනීම
    private val apiService by lazy { ApiService.create() }

    // Encrypted Preferences සහ Local Room Database සඳහා Variables (Lazy Initialization)
    private lateinit var prefsManager: PreferencesManager
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FR 39: පරිශීලක දත්ත ආරක්ෂිතව ගබඩා කිරීමට EncryptedSharedPreferences (AES-256) භාවිතා කරන PreferencesManager එක Initialize කිරීම
        prefsManager = PreferencesManager(this)

        setContent {
            Mindspark2Theme {
                LoginScreen(
                    // Back button එක එබූ විට Activity එක වසා දැමීම
                    onBackClick = { finish() },

                    // Forgot Password ක්ලික් කළ විට ForgotPasswordActivity වෙත යොමු වීම
                    onForgotPasswordClick = {
                        startActivity(Intent(this, ForgotPasswordActivity::class.java))
                    },

                    // Register ක්ලික් කළ විට RegisterActivity වෙත යොමු වීම
                    onRegisterClick = {
                        startActivity(Intent(this, RegisterActivity::class.java))
                    },

                    // Login Form එක Submit කළ විට ක්‍රියාත්මක වන ප්‍රධාන සත්‍යාපන ක්‍රියාවලිය (Login Submission)
                    onLoginSubmit = { email, password, onLoadingChange, onError ->
                        onLoadingChange(true) // Loading Animation එක සක්‍රිය කිරීම

                        // Background Thread (IO Dispatcher) එකක Network සහ Database Calls ක්‍රියාත්මක කිරීම
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val response = apiService.loginUser(LoginRequest(email, password))

                                if (response.isSuccessful) {
                                    // FR 02 & FR 39: Encrypted Preferences හි Session Data සුරැකීම

                                    val userId = response.body()?.userId

                                    if (userId == null || userId <= 0) {
                                        withContext(Dispatchers.Main) {
                                            onLoadingChange(false)
                                            onError("User ID not received from server")
                                        }
                                        return@launch
                                    }
                                    val sessionToken = response.body()?.sessionToken

                                    if (sessionToken.isNullOrBlank()) {
                                        withContext(Dispatchers.Main) {
                                            onLoadingChange(false)
                                            onError("Session token not received from server")
                                        }
                                        return@launch
                                    }


                                    prefsManager.userEmail = email
                                    prefsManager.userId = userId
                                    prefsManager.sessionToken = sessionToken
                                    prefsManager.lastLoginTime = System.currentTimeMillis()


                                    // Username එක API එකෙන් නොලැබුනහොත් Email එකෙන් සාදා ගැනීම
                                    val userName = response.body()?.userName ?: email.substringBefore("@")

                                    // පරිශීලක දත්ත Local Room Database හි Insert/Update කිරීම (Background Thread එකේම සිදු වේ)
                                    val userEntity = UserProfileEntity(
                                        id = 1,
                                        userName = userName,
                                        email = email,
                                        mobileNumber = "",
                                        age = ""
                                    )
                                    database.userDao().insertOrUpdateUser(userEntity)

                                    // UI Navigation සහ Toast සඳහා Main Thread එකට මාරු වීම
                                    withContext(Dispatchers.Main) {
                                        onLoadingChange(false)
                                        Toast.makeText(this@LoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()


                                        val intent = Intent(
                                            this@LoginActivity,
                                            HomeActivity::class.java
                                        ).apply {
                                            putExtra("USER_NAME", userName)
                                            putExtra("USER_ID", userId)
                                        }

                                        startActivity(intent)
                                        finish() // LoginActivity එක පැමිණි Stack එකෙන් ඉවත් කිරීම
                                    }
                                } else {
                                    // FR 02: Backend එකෙන් ලැබෙන Error Codes (201, 202, 203) පරීක්ෂා කිරීම
                                    val errorJson = response.errorBody()?.string()
                                    val message = try {
                                        val jsonObj = JSONObject(errorJson ?: "")
                                        val code = jsonObj.optInt("errorCode", 0)
                                        when (code) {
                                            201 -> "Invalid email address"
                                            202 -> "Incorrect password"
                                            203 -> "Account locked due to 3 failed attempts. Try again in 15 minutes."
                                            else -> jsonObj.optString("message", "Login failed")
                                        }
                                    } catch (e: Exception) {
                                        "Invalid credentials or server error"
                                    }

                                    withContext(Dispatchers.Main) {
                                        onLoadingChange(false)
                                        onError(message)
                                    }
                                }
                            } catch (e: Exception) {
                                // ජාල සම්බන්ධතා දෝෂ (Network/Connection Errors) පාලනය කිරීම
                                withContext(Dispatchers.Main) {
                                    onLoadingChange(false)
                                    onError("Connection Error: ${e.message}")
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

/** Modern HandTalk login UI. Authentication remains in LoginActivity. */
@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSubmit: (String, String, (Boolean) -> Unit, (String) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isAccountLocked by remember { mutableStateOf(false) }

    val navy = Color(0xFF08346D)
    val yellow = Color(0xFFFFD54F)
    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF064A96), Color(0xFF1787D6), Color(0xFF65CBF6))
    )

    Box(Modifier.fillMaxSize().background(gradient)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.05f, size.height * 0.95f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = size.width * 0.65f,
                center = Offset(size.width * 1.08f, size.height * 0.52f)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "←",
                fontSize = 28.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start).clickable { onBackClick() }
                    .padding(8.dp)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.size(94.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("🤟", fontSize = 57.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text("HandTalk", color = yellow, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Text("Welcome Back", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Login to continue", color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp)
            Spacer(Modifier.height(26.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    errorMessage?.let { msg ->
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                msg, color = Color(0xFFB71C1C), fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email") },
                        placeholder = { Text("Enter your email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading && !isAccountLocked,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        placeholder = { Text("Enter your password") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Text(if (isPasswordVisible) "Hide" else "Show", color = navy)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading && !isAccountLocked,
                        shape = RoundedCornerShape(16.dp)
                    )
                    TextButton(
                        onClick = onForgotPasswordClick,
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?", color = Color(0xFF1554B7))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val cleanEmail = email.trim()
                            val cleanPassword = password.trim()
                            when {
                                cleanEmail.isEmpty() || cleanPassword.isEmpty() ->
                                    errorMessage = "Please enter both Email and Password"
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() ->
                                    errorMessage = "Please enter a valid email address"
                                cleanPassword.length < 8 || cleanPassword.length > 64 ->
                                    errorMessage = "Password length must be between 8 and 64 characters"
                                else -> {
                                    errorMessage = null
                                    onLoginSubmit(cleanEmail, cleanPassword,
                                        { loading -> isLoading = loading },
                                        { err ->
                                            errorMessage = err
                                            if (err.contains("locked", ignoreCase = true) || err.contains("203")) {
                                                isAccountLocked = true
                                            }
                                        })
                                }
                            }
                        },
                        enabled = !isLoading && !isAccountLocked,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = yellow,
                            contentColor = navy,
                            disabledContainerColor = Color.LightGray
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp), color = navy, strokeWidth = 3.dp
                            )
                        } else {
                            Text(if (isAccountLocked) "Account Locked" else "Login",
                                fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Don't have an account?", color = Color(0xFF53617B), fontSize = 14.sp)
                        TextButton(onClick = onRegisterClick, enabled = !isLoading) {
                            Text("Sign Up", color = Color(0xFF1554B7), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Breaking Communication Barriers",
                color = Color.White, fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

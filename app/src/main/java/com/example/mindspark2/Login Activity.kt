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
                                    prefsManager.userEmail = email
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

                                        val intent = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                                            putExtra("USER_NAME", userName)
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

/**
 * LoginScreen: Jetpack Compose මගින් Login Screen එකේ UI එක, Animations,
 * TextFields සහ Client-side Validations සකස් කිරීම.
 */
@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSubmit: (String, String, (Boolean) -> Unit, (String) -> Unit) -> Unit
) {
    // Input Fields සහ Screen States පාලනය කරන Variables
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) } // Password එක Show/Hide කිරීම
    var errorMessage by remember { mutableStateOf<String?>(null) } // Error Messages පෙන්වීමට
    var isLoading by remember { mutableStateOf(false) } // Progress Indicator පාලනයට
    var isAccountLocked by remember { mutableStateOf(false) } // ගිණුම Lock වී ඇත්දැයි බලන State එක

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // පසුබිමේ ඇති මෝස්තර රවුම් (Background Canvas Circles) නිර්මාණය කිරීම
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // වම් පස පහළ රවුම
            drawCircle(
                color = Color(0xFFD0E3FA),
                radius = 300f,
                center = Offset(0f, size.height)
            )

            // දකුණු පස ඉහළ රවුම
            drawCircle(
                color = Color(0xFFE6F1FD),
                radius = 240f,
                center = Offset(size.width, 0f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(25.dp)
        ) {
            Spacer(modifier = Modifier.height(25.dp))

            // Back Arrow Button (ආපසු යාමට)
            Text(
                text = "←",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(15.dp))

            // Screen Header Title
            Text(
                text = "Login",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // FR 30: Error Alert Message පෙන්වන කොටස
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(15.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Email Label & Input Field
            Text(
                text = "Email",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null // Type කරන විට Error Message එක ඉවත් කිරීම
                },
                placeholder = { Text("Enter your email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading && !isAccountLocked, // Account locked හෝ Loading නම් Disable වේ
                shape = RoundedCornerShape(22.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Password Label & Input Field (Show/Hide Toggle සමඟ)
            Text(
                text = "Password",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = { Text("Enter your password") },
                // Password එක පෙනෙන/නොපෙනෙන ලෙස සැකසීම
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(
                            text = if (isPasswordVisible) "Hide" else "Show",
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading && !isAccountLocked,
                shape = RoundedCornerShape(22.dp)
            )

            Spacer(modifier = Modifier.height(15.dp))

            // Forgot Password Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onForgotPasswordClick,
                    enabled = !isLoading
                ) {
                    Text(
                        "Forgot Password?",
                        color = Color(0xFF1565C0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Login Submit Button
            Button(
                onClick = {
                    // FR 01 & FR 02: Client-side Input Validations පරීක්ෂා කිරීම
                    val cleanEmail = email.trim()
                    val cleanPassword = password.trim()

                    when {
                        // Empty Inputs Check
                        cleanEmail.isEmpty() || cleanPassword.isEmpty() -> {
                            errorMessage = "Please enter both Email and Password"
                        }
                        // Email Format Check
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                            errorMessage = "Please enter a valid email address"
                        }
                        // Password Length Check (අක්ෂර 8-64 අතර)
                        cleanPassword.length < 8 || cleanPassword.length > 64 -> {
                            errorMessage = "Password length must be between 8 and 64 characters"
                        }
                        // Validations සාර්ථක නම් Submit කිරීම
                        else -> {
                            errorMessage = null
                            onLoginSubmit(
                                cleanEmail,
                                cleanPassword,
                                { loading -> isLoading = loading },
                                { err ->
                                    errorMessage = err
                                    // 203 Code එකෙන් හෝ Lock Message එකෙන් Account එක Lock වූ බව හඳුනා ගැනීම
                                    if (err.contains("locked", ignoreCase = true) || err.contains("203")) {
                                        isAccountLocked = true
                                    }
                                }
                            )
                        }
                    }
                },
                enabled = !isLoading && !isAccountLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAccountLocked) Color.Gray else Color(0xFF1565C0)
                )
            ) {
                // Request එක සිදු වන විට Progress Spinner එක පෙන්වීම
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = if (isAccountLocked) "Account Locked" else "Login",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Register Activity වෙත යාමට Navigation Link එක
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Don't have an account? ")

                TextButton(
                    onClick = onRegisterClick,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Register",
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // App Footer Label
            Text(
                text = "HandTalk",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
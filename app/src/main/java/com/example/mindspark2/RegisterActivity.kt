package com.example.mindspark2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * RegisterActivity: නව පරිශීලකයන් පද්ධතියට ලියාපදිංචි කිරීම (Registration) පාලනය කරන Activity එක.
 */
class RegisterActivity : ComponentActivity() {

    // Network API ඇමතුම් ලබා ගැනීම සඳහා ApiService හි instance එකක් සාදා ගැනීම
    private val apiService by lazy { ApiService.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-Edge UI layout එක සක්‍රිය කිරීම (StatusBar/NavigationBar දක්වා UI එක පෙන්වීමට)
        enableEdgeToEdge()

        setContent {
            Mindspark2Theme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RegisterScreen(
                        // Login link එක ක්ලික් කළ විට LoginActivity වෙත යොමු වීම
                        onLoginClick = {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        },
                        // Registration form එක Submit කළ විට ක්‍රියාත්මක වන Backend Call එක
                        onRegisterSubmit = { name, email, password, onResult ->
                            // Background thread (IO Dispatcher) එකක API request එක ක්‍රියාත්මක කිරීම
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val response = apiService.registerUser(
                                        RegisterRequest(name = name, email = email, password = password)
                                    )
                                    // UI වෙනස්කම් සිදු කිරීමට Main Thread එක වෙත මාරු වීම
                                    withContext(Dispatchers.Main) {
                                        if (response.isSuccessful) {
                                            Toast.makeText(
                                                this@RegisterActivity,
                                                "Account created successfully! Please login.",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            // FR 31: සාර්ථක ලියාපදිංචියෙන් පසු පරිශීලකයා LoginActivity වෙත යොමු කිරීම
                                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                                            // පැරණි Activity Stack එක ඉවත් කිරීම
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            // FR 01: Backend Error Codes (101, 102, 103) පරීක්ෂා කිරීම සහ අදාළ Error Message එක සකස් කිරීම
                                            val errorJson = response.errorBody()?.string()
                                            val message = try {
                                                val jsonObj = JSONObject(errorJson ?: "")
                                                val code = jsonObj.optInt("errorCode", 0)
                                                when (code) {
                                                    101 -> "Email already exists (Error Code: 101)"
                                                    102 -> "Weak password. Require uppercase, lowercase, number, special char (Error Code: 102)"
                                                    103 -> "Invalid email address format (Error Code: 103)"
                                                    else -> jsonObj.optString("message", "Registration failed.")
                                                }
                                            } catch (e: Exception) {
                                                "Registration failed. Email might already exist."
                                            }
                                            onResult(message) // Error එක UI එකට යැවීම
                                        }
                                    }
                                } catch (e: Exception) {
                                    // ජාල සම්බන්ධතා ගැටළු (Network Errors) පාලනය කිරීම
                                    withContext(Dispatchers.Main) {
                                        onResult("Connection Error: ${e.localizedMessage ?: "Network unreachable"}")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * RegisterScreen: Jetpack Compose මගින් ලියාපදිංචි වීමේ Screen එකේ UI එක සහ User Input Validations නිර්මාණය කිරීම.
 */
@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit,
    onRegisterSubmit: (String, String, String, (String) -> Unit) -> Unit
) {
    // User Input දත්ත මතකයේ තබා ගැනීමට අවශ්‍ය Variable States
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var agreeTerms by remember { mutableStateOf(false) }

    // Passwords පෙන්වීම/සැඟවීම පාලනය කරන States (Show/Hide Password)
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Error Message සහ Loading Spinner පාලනය කරන States
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Text field අතර Focus එක මාරු කිරීමට LocalFocusManager යොදා ගැනීම
    val focusManager = LocalFocusManager.current

    /**
     * Client-side User Input Validations පරීක්ෂා කර Form එක Submit කරන Function එක
     */
    fun triggerSubmit() {
        focusManager.clearFocus() // Soft keyboard එක සහ Focus එක ඉවත් කිරීම
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        // FR 01: Password එකේ ශක්තිමත්භාවය පරීක්ෂා කරන Regex Pattern එක (Uppercase, Lowercase, Number, Special Char)
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,64}\$")

        when {
            // සියලුම Fields පුරවා තිබේදැයි බලයි
            cleanName.isEmpty() || cleanEmail.isEmpty() ||
                    cleanPassword.isEmpty() || confirmPassword.trim().isEmpty() -> {
                errorMessage = "All fields are mandatory"
            }
            // Email එක නිවැරදි Format එකට තිබේදැයි බලයි
            !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() -> {
                errorMessage = "Please enter a valid email address"
            }
            // Password එකේ දිග අක්ෂර 8-64 අතරදැයි බලයි
            cleanPassword.length < 8 || cleanPassword.length > 64 -> {
                errorMessage = "Password must be between 8 and 64 characters"
            }
            // Password එක සංකීර්ණතා කොන්දේසි සපුරාලයිදැයි බලයි
            !passwordPattern.matches(cleanPassword) -> {
                errorMessage = "Password must contain uppercase, lowercase, number, and special character"
            }
            // Passwords දෙක එකිනෙකට සමානදැයි බලයි
            cleanPassword != confirmPassword.trim() -> {
                errorMessage = "Passwords do not match"
            }
            // Terms & Conditions වලට එකඟ වී ඇත්දැයි බලයි
            !agreeTerms -> {
                errorMessage = "Please accept the Terms & Conditions"
            }
            // Validations සියල්ල සාර්ථක නම් Backend එකට Data යැවීම
            else -> {
                errorMessage = null
                isLoading = true
                onRegisterSubmit(cleanName, cleanEmail, cleanPassword) { err ->
                    isLoading = false
                    errorMessage = err
                }
            }
        }
    }

    // Gradient Background එක සහිත ප්‍රධාන Container එක
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF042F62),
                        Color(0xFF2682CC),
                        Color(0xFFD3E9FA)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // සුදු පැහැති Input Form Card එක
        Card(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .statusBarsPadding(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier
                    .padding(25.dp)
                    .verticalScroll(rememberScrollState()), // Screen එක Scroll කිරීමට ඉඩ සලසයි
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "🤟",
                    fontSize = 55.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF042F62)
                )

                Text(
                    text = "Join HandTalk community",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                // FR 30: Error Alert එකක් පෙන්වීමේ පද්ධතිය
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

                Spacer(modifier = Modifier.height(20.dp))

                // FR 01: Full Name Input Field (උපරිම අක්ෂර 100)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 100) name = it
                        errorMessage = null
                    },
                    label = { Text("Full Name") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // FR 01: Email Input Field (උපරිම අක්ෂර 254)
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        if (it.length <= 254) email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // FR 01: Password Input Field (Show/Hide Toggle සමඟ)
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (it.length <= 64) password = it
                        errorMessage = null
                    },
                    label = { Text("Password (Min 8 characters)") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            enabled = !isLoading
                        ) {
                            Text(
                                text = if (isPasswordVisible) "Hide" else "Show",
                                color = Color(0xFF2682CC),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Confirm Password Input Field (Show/Hide Toggle සමඟ)
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        if (it.length <= 64) confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Password") },
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                            enabled = !isLoading
                        ) {
                            Text(
                                text = if (isConfirmPasswordVisible) "Hide" else "Show",
                                color = Color(0xFF2682CC),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { triggerSubmit() } // Soft Keyboard එකේ Done එබූ විට Submit වීම
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Terms & Conditions Checkbox කොටස
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = agreeTerms,
                        onCheckedChange = {
                            agreeTerms = it
                            errorMessage = null
                        },
                        enabled = !isLoading
                    )

                    Text(
                        text = buildAnnotatedString {
                            append("I agree to the ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFF2682CC),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Privacy Policy")
                            }
                            append(" and ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFF2682CC),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Terms & Conditions")
                            }
                        },
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Registration Submit Button එක
                Button(
                    onClick = { triggerSubmit() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF042F62)
                    )
                ) {
                    // Request එකක් යවන අතරතුර Loading Indicator එකක් පෙන්වීම
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Login Screen එකට යාම සඳහා වන Text Button එක
                TextButton(
                    onClick = onLoginClick,
                    enabled = !isLoading
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Already have an account? ")
                            withStyle(
                                style = SpanStyle(
                                    color = Color(0xFF2682CC),
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("Login")
                            }
                        }
                    )
                }
            }
        }
    }
}
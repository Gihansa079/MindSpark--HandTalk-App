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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : ComponentActivity() {

    private val apiService by lazy { ApiService.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Mindspark2Theme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    RegisterScreen(
                        onLoginClick = {
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        },
                        onRegisterSubmit = { name, email, password, onError ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val response = apiService.registerUser(
                                        RegisterRequest(name = name, email = email, password = password)
                                    )
                                    withContext(Dispatchers.Main) {
                                        if (response.isSuccessful) {
                                            Toast.makeText(this@RegisterActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                                            finish()
                                        } else {
                                            onError("Registration failed or email already exists!")
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
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
}

@Composable
fun RegisterScreen(
    onLoginClick: () -> Unit,
    onRegisterSubmit: (String, String, String, (String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var agreeTerms by remember { mutableStateOf(false) }

    // Error message එක සටහන් කරගන්නා variable එක
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    .verticalScroll(rememberScrollState()),
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

                // 🔴 Error Message Banner (Screen/Card එකේ උඩින්ම පෙනෙන පරිදි)
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

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null // Type කරද්දී Error එක අයින් වේ
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (it.length <= 10) password = it
                        errorMessage = null
                    },
                    label = { Text("Password (Max 10)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        if (it.length <= 10) confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = agreeTerms,
                        onCheckedChange = {
                            agreeTerms = it
                            errorMessage = null
                        }
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

                Button(
                    onClick = {
                        when {
                            name.trim().isEmpty() || email.trim().isEmpty() ||
                                    password.trim().isEmpty() || confirmPassword.trim().isEmpty() -> {
                                errorMessage = "Please fill in all fields"
                            }
                            password.length > 10 -> {
                                errorMessage = "Password cannot be longer than 10 characters"
                            }
                            password != confirmPassword -> {
                                errorMessage = "Passwords do not match"
                            }
                            !agreeTerms -> {
                                errorMessage = "Please accept the Terms & Conditions"
                            }
                            else -> {
                                errorMessage = null
                                onRegisterSubmit(name.trim(), email.trim(), password) { err ->
                                    errorMessage = err
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF042F62)
                    )
                ) {
                    Text(
                        text = "Create Account",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onLoginClick
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
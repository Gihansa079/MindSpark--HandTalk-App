package com.example.mindspark2

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

class ForgotPasswordActivity : ComponentActivity() {

    private val apiService by lazy { ApiService.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Mindspark2Theme {
                ForgotPasswordScreen(
                    onBackClick = { finish() },
                    onSubmit = { email, newPassword, onError, setLoading ->
                        setLoading(true)
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val response = apiService.resetPassword(ForgotPasswordRequest(email, newPassword))
                                withContext(Dispatchers.Main) {
                                    setLoading(false)
                                    if (response.isSuccessful) {
                                        Toast.makeText(this@ForgotPasswordActivity, "Password reset successfully!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                                        finish()
                                    } else {
                                        // FR 03: Handle Backend Error Code 301
                                        val errorJson = response.errorBody()?.string()
                                        val message = try {
                                            val jsonObj = JSONObject(errorJson ?: "")
                                            val code = jsonObj.optInt("errorCode", 0)
                                            when (code) {
                                                301 -> "Email address not found in system (Error 301)"
                                                else -> jsonObj.optString("message", "Reset failed. Email not found!")
                                            }
                                        } catch (e: Exception) {
                                            "Email not found or server error"
                                        }
                                        onError(message)
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    setLoading(false)
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

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onSubmit: (email: String, newPassword: String, onError: (String) -> Unit, setLoading: (Boolean) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Password visibility states
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    fun triggerSubmit() {
        focusManager.clearFocus()
        val trimmedEmail = email.trim()
        val trimmedPassword = newPassword.trim()
        val trimmedConfirm = confirmPassword.trim()

        // Improved Regex supporting standard symbols including #
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,64}$")

        when {
            trimmedEmail.isEmpty() || trimmedPassword.isEmpty() || trimmedConfirm.isEmpty() -> {
                errorMessage = "Please fill all fields"
            }
            !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> {
                errorMessage = "Please enter a valid email address"
            }
            trimmedPassword.length < 8 || trimmedPassword.length > 64 -> {
                errorMessage = "Password must be between 8 and 64 characters"
            }
            !passwordPattern.matches(trimmedPassword) -> {
                errorMessage = "Password must contain uppercase, lowercase, number, and special character"
            }
            trimmedPassword != trimmedConfirm -> {
                errorMessage = "Passwords do not match"
            }
            else -> {
                errorMessage = null
                onSubmit(
                    trimmedEmail,
                    trimmedPassword,
                    { err -> errorMessage = err },
                    { loading -> isLoading = loading }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(25.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(25.dp))

        TextButton(onClick = onBackClick, enabled = !isLoading) {
            Text(text = "← Back to Login", fontSize = 16.sp, color = Color(0xFF1565C0))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Reset Password",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF042F62)
        )

        // FR 30: Error Notification Feedback Alert
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
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        // FR 03 Constraint: Email Length <= 254
        OutlinedTextField(
            value = email,
            onValueChange = {
                if (it.length <= 254) email = it
                errorMessage = null
            },
            label = { Text("Enter your registered Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(15.dp))

        // FR 03 Constraint: Password Max Length <= 64 with Show/Hide Toggle
        OutlinedTextField(
            value = newPassword,
            onValueChange = {
                if (it.length <= 64) newPassword = it
                errorMessage = null
            },
            label = { Text("New Password (Min 8 characters)") },
            visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                    Text(
                        text = if (isNewPasswordVisible) "Hide" else "Show",
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(15.dp))

        // Confirm Password with Show/Hide Toggle
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                if (it.length <= 64) confirmPassword = it
                errorMessage = null
            },
            label = { Text("Confirm New Password") },
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Text(
                        text = if (isConfirmPasswordVisible) "Hide" else "Show",
                        color = Color(0xFF1565C0),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { triggerSubmit() }
            )
        )

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = { triggerSubmit() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Reset Password", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
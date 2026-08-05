package com.example.mindspark2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : ComponentActivity() {

    private val apiService by lazy { ApiService.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Mindspark2Theme {
                ForgotPasswordScreen(
                    onBackClick = { finish() },
                    onSubmit = { email, newPassword, onError ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val response = apiService.resetPassword(ForgotPasswordRequest(email, newPassword))
                                withContext(Dispatchers.Main) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(this@ForgotPasswordActivity, "Password reset successfully!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                                        finish()
                                    } else {
                                        onError("Email not found!")
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

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onSubmit: (String, String, (String) -> Unit) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(25.dp)
    ) {
        Spacer(modifier = Modifier.height(25.dp))

        TextButton(onClick = onBackClick) {
            Text(text = "← Back to Login", fontSize = 16.sp, color = Color(0xFF1565C0))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Reset Password",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF042F62)
        )

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

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Enter your registered Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { if (it.length <= 10) newPassword = it; errorMessage = null },
            label = { Text("New Password (Max 10)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {
                if (email.trim().isEmpty() || newPassword.trim().isEmpty()) {
                    errorMessage = "Please fill all fields"
                } else {
                    onSubmit(email.trim(), newPassword) { err -> errorMessage = err }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            Text("Reset Password", fontSize = 18.sp, color = Color.White)
        }
    }
}
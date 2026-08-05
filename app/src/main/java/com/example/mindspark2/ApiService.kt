package com.example.mindspark2

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Register Request Model
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

// Login Request Model
data class LoginRequest(
    val email: String,
    val password: String
)

// Forgot Password Request Model
data class ForgotPasswordRequest(
    val email: String,
    val newPassword: String
)

interface ApiService {
    @POST("api/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<Map<String, String>>

    // Login Endpoint
    @POST("api/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<Map<String, String>>

    // Forgot Password Endpoint
    @POST("api/forgot-password")
    suspend fun resetPassword(@Body request: ForgotPasswordRequest): Response<Map<String, String>>

    companion object {
        // Emulator එකේ සිට Laptop එකේ Localhost එකට Connect වීමට 10.0.2.2 යොදා ගනී
        private const val BASE_URL = "http://10.0.2.2:3000/"

        fun create(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
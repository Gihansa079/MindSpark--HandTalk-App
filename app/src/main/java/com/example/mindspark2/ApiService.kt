package com.example.mindspark2

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Models
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ForgotPasswordRequest(
    val email: String,
    val newPassword: String
)

// PostgreSQL එකෙන් ලැබෙන Translation Model එක
data class TranslationResponse(
    val id: Int? = null,
    val english_text: String? = null,
    val sinhala_text: String? = null,
    val created_at: String? = null
)

// Translation Save කිරීමට යවන Request Model එක
data class TranslationRequest(
    val english_text: String,
    val sinhala_text: String
)

interface ApiService {
    @POST("api/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<Map<String, String>>

    @POST("api/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<Map<String, String>>

    @POST("api/forgot-password")
    suspend fun resetPassword(@Body request: ForgotPasswordRequest): Response<Map<String, String>>

    // Sinhala text එකෙන් English translation එක ලබා ගැනීම (Search Endpoint)
    @GET("api/translations/search")
    suspend fun getTranslation(
        @Query("text") text: String
    ): Response<TranslationResponse>

    // PostgreSQL Backend එකෙන් Latest Translation එක ගැනීම
    @GET("api/translations/latest")
    suspend fun getLatestTranslation(): Response<TranslationResponse>

    // New Translation එකක් DB එකට Save කිරීම
    @POST("api/translations")
    suspend fun saveTranslation(@Body translation: TranslationRequest): Response<Void>

    // Database එකේ ඇති සියලුම Translations ලබා ගැනීම
    @GET("api/translations")
    suspend fun getAllTranslations(): Response<List<TranslationResponse>>

    companion object {
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
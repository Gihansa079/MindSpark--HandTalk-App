package com.example.mindspark2

import android.os.Build
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Dynamic Base URL Settings ---
private const val LOCAL_IP = "10.49.179.122"
private const val PORT = "3000"

fun isEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("google_sdk"))
}

fun getBaseUrl(): String {
    return if (isEmulator()) {
        "http://10.0.2.2:$PORT/"      // Emulator එක සඳහා
    } else {
        "http://$LOCAL_IP:$PORT/"     // Real Mobile Device එක සඳහා
    }
}

// --- Generic / Auth Models ---
data class ApiResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("userName", alternate = ["user_name", "name"]) val userName: String? = null
)

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

// --- User Profile Models ---
data class UserProfileResponse(
    val id: Int? = null,
    @SerializedName("user_name", alternate = ["userName", "name"])
    val user_name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("mobile_number", alternate = ["mobileNumber", "mobile", "phone"])
    val mobile_number: String? = null,
    @SerializedName("age")
    val age: String? = null
)

data class UserProfileUpdateRequest(
    val user_name: String,
    val email: String,
    val mobile_number: String,
    val age: String
)

// --- Translation Models ---
data class TranslationResponse(
    val id: Int? = null,
    @SerializedName("english_text") val english_text: String? = null,
    @SerializedName("sinhala_text") val sinhala_text: String? = null,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("created_at") val created_at: String? = null,
    @SerializedName("show_in_history") val show_in_history: Boolean? = true
)

data class TranslationRequest(
    @SerializedName("english_text") val english_text: String,
    @SerializedName("sinhala_text") val sinhala_text: String
)

// --- Favorites Models ---
data class FavoriteRequest(
    @SerializedName("english_text") val english_text: String,
    @SerializedName("sinhala_text") val sinhala_text: String
)

data class FavoriteItemResponse(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("english_text") val english_text: String? = null,
    @SerializedName("sinhala_text") val sinhala_text: String? = null,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("created_at") val created_at: String? = null
)

// --- API Service Interface ---
interface ApiService {

    // Auth Endpoints
    @POST("api/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<ApiResponse>

    @POST("api/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<ApiResponse>

    @POST("api/forgot-password")
    suspend fun resetPassword(@Body request: ForgotPasswordRequest): Response<ApiResponse>

    // User Profile Endpoints
    @GET("api/user/profile")
    suspend fun getUserProfile(@Query("email") email: String): Response<UserProfileResponse>

    @GET("api/user/profile/{email}")
    suspend fun getUserProfileByPath(@Path("email") email: String): Response<UserProfileResponse>

    @PUT("api/user/profile")
    suspend fun updateUserProfile(@Body request: UserProfileUpdateRequest): Response<ApiResponse>

    // Translation Endpoints
    // 'word' සහ 'text' යන parameters දෙකටම alternate support ඇති ලෙස සකසා ඇත
    @GET("api/translations/search")
    suspend fun getTranslation(
        @Query("text") text: String
    ): Response<TranslationResponse>

    @GET("api/translations/latest")
    suspend fun getLatestTranslation(): Response<TranslationResponse>

    @POST("api/translations")
    suspend fun saveTranslation(@Body translation: TranslationRequest): Response<ApiResponse>

    @GET("api/translations")
    suspend fun getAllTranslations(): Response<List<TranslationResponse>>

    // Media Translation Endpoint (Image / Video)
    @Multipart
    @POST("api/translations/media")
    suspend fun translateMedia(
        @Part media: MultipartBody.Part
    ): Response<TranslationResponse>

    // History Actions
    @PUT("api/translations/hide/{id}")
    suspend fun hideFromHistory(@Path("id") id: Int): Response<ApiResponse>

    @PUT("api/translations/hide-all")
    suspend fun hideAllFromHistory(): Response<ApiResponse>

    // Favorites Endpoints
    @GET("api/favorites")
    suspend fun getFavorites(): Response<List<FavoriteItemResponse>>

    @POST("api/favorites")
    suspend fun addFavorite(@Body request: FavoriteRequest): Response<ApiResponse>

    @DELETE("api/favorites/{id}")
    suspend fun removeFavorite(@Path("id") id: Int): Response<ApiResponse>

    companion object {
        fun create(baseUrl: String = getBaseUrl()): ApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
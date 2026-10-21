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
import retrofit2.http.Header
// BASE URL SETTINGS

// Change this IP to your computer's current IPv4 address
// when using a real Android phone.
private const val LOCAL_IP = "10.40.15.138"
private const val PORT = "3000"

fun isEmulator(): Boolean {
    return (
            Build.FINGERPRINT.startsWith("generic") ||
                    Build.FINGERPRINT.startsWith("unknown") ||
                    Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK built for x86") ||
                    Build.MANUFACTURER.contains("Genymotion") ||
                    Build.HARDWARE.contains("goldfish") ||
                    Build.HARDWARE.contains("ranchu") ||
                    Build.PRODUCT.contains("sdk") ||
                    Build.PRODUCT.contains("google_sdk")
            )
}

fun getBaseUrl(): String {
    return if (isEmulator()) {
        "http://10.0.2.2:$PORT/"
    } else {
        "http://$LOCAL_IP:$PORT/"
    }
}
fun getMediaUrl(imagePath: String?): String? {
    if (imagePath.isNullOrBlank()) return null

    val path = imagePath.trim()

    if (path.startsWith("http://") ||
        path.startsWith("https://")) {
        return path
    }

    return getBaseUrl().trimEnd('/') + "/" + path.trimStart('/')
}
// AUTHENTICATION MODELS

data class ApiResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName(
        value = "userName",
        alternate = ["user_name", "name"]
    )
    val userName: String? = null,

    @SerializedName(
        value = "userId",
        alternate = ["user_id", "id"]
    )
    val userId: Int? = null,

    @SerializedName("sessionToken")
    val sessionToken: String? = null,

    @SerializedName("expiresAt")
    val expiresAt: String? = null
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

// USER PROFILE MODELS

data class UserProfileResponse(
    val id: Int? = null,

    @SerializedName(
        value = "user_name",
        alternate = ["userName", "name"]
    )
    val user_name: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName(
        value = "mobile_number",
        alternate = ["mobileNumber", "mobile", "phone"]
    )
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
// STATIC AND DYNAMIC GESTURE MODELS

data class GestureResponse(
    @SerializedName("gesture_id")
    val gestureId: Int,

    @SerializedName("gesture_name")
    val gestureName: String,

    @SerializedName("sinhala_text")
    val sinhalaText: String,

    @SerializedName("english_text")
    val englishText: String,

    @SerializedName("class_label")
    val classLabel: Int? = null,

    @SerializedName("image_path")
    val imagePath: String? = null,

    @SerializedName("video_path")
    val videoPath: String? = null
)

// RECOGNITION RESULT MODELS
// translation_new table

data class RecognitionSaveRequest(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("gesture_id")
    val gestureId: Int,

    @SerializedName("gesture_type")
    val gestureType: String,

    @SerializedName("confidence_score")
    val confidenceScore: Double? = null,

    @SerializedName("image_path")
    val imagePath: String? = null,

    @SerializedName("video_path")
    val videoPath: String? = null,

    @SerializedName("sinhala_voice_path")
    val sinhalaVoicePath: String? = null,

    @SerializedName("english_voice_path")
    val englishVoicePath: String? = null
)

data class RecognitionResponse(
    @SerializedName("translation_id")
    val translationId: Int? = null,

    @SerializedName("user_id")
    val userId: Int? = null,

    @SerializedName("gesture_id")
    val gestureId: Int? = null,

    @SerializedName("gesture_type")
    val gestureType: String? = null,

    @SerializedName("confidence_score")
    val confidenceScore: Double? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

data class RecognitionSaveResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("translation")
    val translation: RecognitionResponse? = null
)
// LEGACY TRANSLATION MODELS
// Retained temporarily for existing Kotlin callers.
// Old server endpoints are no longer available.

data class TranslationResponse(
    val id: Int? = null,

    @SerializedName("english_text")
    val english_text: String? = null,

    @SerializedName("sinhala_text")
    val sinhala_text: String? = null,

    @SerializedName("image_url")
    val image_url: String? = null,

    @SerializedName("created_at")
    val created_at: String? = null,

    @SerializedName("show_in_history")
    val show_in_history: Boolean? = true
)

data class TranslationRequest(
    @SerializedName("english_text")
    val english_text: String,

    @SerializedName("sinhala_text")
    val sinhala_text: String
)

// HISTORY MODELS

data class HistoryRequest(
    @SerializedName("user_id")
    val userId: Int,

    // Keep null until history is migrated
    // away from the old translations foreign key.
    @SerializedName("translation_id")
    val translationId: Int? = null,

    @SerializedName("english_text")
    val englishText: String,

    @SerializedName("sinhala_text")
    val sinhalaText: String,

    @SerializedName("recognition_type")
    val recognitionType: String,

    @SerializedName("input_type")
    val inputType: String,

    @SerializedName("confidence_score")
    val confidenceScore: Double? = null
)

data class HistoryResponse(
    @SerializedName("history_id")
    val historyId: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("translation_id")
    val translationId: Int? = null,

    @SerializedName("english_text")
    val englishText: String? = null,

    @SerializedName("sinhala_text")
    val sinhalaText: String? = null,

    @SerializedName("recognition_type")
    val recognitionType: String,

    @SerializedName("input_type")
    val inputType: String? = null,

    @SerializedName("confidence_score")
    val confidenceScore: Double? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

// LEGACY FAVORITES MODELS
// Retained for existing Kotlin callers.

data class FavoriteRequest(
    @SerializedName("english_text")
    val english_text: String,

    @SerializedName("sinhala_text")
    val sinhala_text: String
)

data class FavoriteItemResponse(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("english_text")
    val english_text: String? = null,

    @SerializedName("sinhala_text")
    val sinhala_text: String? = null,

    @SerializedName("image_url")
    val image_url: String? = null,

    @SerializedName("created_at")
    val created_at: String? = null
)

// API SERVICE

interface ApiService {

    // ----------------------------------------
    // AUTHENTICATION
    // ----------------------------------------

    @POST("api/register")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<ApiResponse>

    @POST("api/login")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): Response<ApiResponse>

    @POST("api/forgot-password")
    suspend fun resetPassword(
        @Body request: ForgotPasswordRequest
    ): Response<ApiResponse>

    // ----------------------------------------
    // USER PROFILE
    // ----------------------------------------

    @GET("api/user/profile")
    suspend fun getUserProfile(
        @Query("email") email: String
    ): Response<UserProfileResponse>

    @GET("api/user/profile/{email}")
    suspend fun getUserProfileByPath(
        @Path("email") email: String
    ): Response<UserProfileResponse>

    @PUT("api/user/profile")
    suspend fun updateUserProfile(
        @Body request: UserProfileUpdateRequest
    ): Response<ApiResponse>

    // ----------------------------------------
    // STATIC GESTURES
    // ----------------------------------------

    @GET("api/static-gestures")
    suspend fun getStaticGestures():
            Response<List<GestureResponse>>

    // ----------------------------------------
    // DYNAMIC GESTURES
    // ----------------------------------------

    @GET("api/dynamic-gestures")
    suspend fun getDynamicGestures():
            Response<List<GestureResponse>>

    // ----------------------------------------
    // TRANSLATE RECOGNIZED GESTURE
    // ----------------------------------------

    @GET("api/gestures/translate")
    suspend fun translateGesture(
        @Query("type") type: String,
        @Query("label") label: Int
    ): Response<GestureResponse>

    // ----------------------------------------
    // SAVE RECOGNITION TO translation_new
    // ----------------------------------------

    @POST("api/translation-new")
    suspend fun saveRecognition(
        @Body request: RecognitionSaveRequest
    ): Response<RecognitionSaveResponse>

    // ----------------------------------------
    // HISTORY
    // ----------------------------------------

    @POST("api/history")
    suspend fun saveHistory(
        @Body request: HistoryRequest
    ): Response<ApiResponse>

    @GET("api/history")
    suspend fun getUserHistory(
        @Query("user_id") userId: Int
    ): Response<List<HistoryResponse>>

    // This endpoint must also be implemented
    // in server.js before it can be used.
    @DELETE("api/history/{id}")
    suspend fun deleteHistory(
        @Path("id") historyId: Int,
        @Query("user_id") userId: Int
    ): Response<ApiResponse>

    @DELETE("api/history")
    suspend fun clearUserHistory(
        @Query("user_id") userId: Int
    ): Response<ApiResponse>

    // ----------------------------------------
    // LEGACY TRANSLATION ENDPOINTS
    // Temporary compatibility only.
    // Remove after updating all callers.
    // ----------------------------------------

    @GET("api/translations/search")
    suspend fun getTranslation(
        @Query("text") text: String
    ): Response<TranslationResponse>

    @GET("api/translations/latest")
    suspend fun getLatestTranslation(
        @Query("user_id") userId: Int
    ): Response<TranslationResponse>

    @POST("api/translations")
    suspend fun saveTranslation(
        @Body translation: TranslationRequest
    ): Response<ApiResponse>

    @GET("api/translations")
    suspend fun getAllTranslations():
            Response<List<TranslationResponse>>

    @Multipart
    @POST("api/translations/media")
    suspend fun translateMedia(
        @Part media: MultipartBody.Part
    ): Response<TranslationResponse>

    @PUT("api/translations/hide/{id}")
    suspend fun hideFromHistory(
        @Path("id") id: Int
    ): Response<ApiResponse>

    @PUT("api/translations/hide-all")
    suspend fun hideAllFromHistory():
            Response<ApiResponse>

    // ----------------------------------------
    // LEGACY FAVORITES ENDPOINTS
    // Not implemented in the shared server.js.
    // ----------------------------------------

    @GET("api/favorites")
    suspend fun getFavorites():
            Response<List<FavoriteItemResponse>>

    @POST("api/favorites")
    suspend fun addFavorite(
        @Body request: FavoriteRequest
    ): Response<ApiResponse>

    @POST("api/logout")
    suspend fun logoutUser(
        @Header("Authorization") authorization: String
    ): Response<ApiResponse>

    @GET("api/session")
    suspend fun validateSession(
        @Header("Authorization") authorization: String
    ): Response<ApiResponse>

    @DELETE("api/favorites/{id}")
    suspend fun removeFavorite(
        @Path("id") id: Int
    ): Response<ApiResponse>

    // ----------------------------------------
    // RETROFIT CLIENT
    // ----------------------------------------

    companion object {

        fun create(
            baseUrl: String = getBaseUrl()
        ): ApiService {

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()
                .create(ApiService::class.java)
        }
    }
}

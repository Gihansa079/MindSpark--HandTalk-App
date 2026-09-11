package com.example.mindspark2.camera

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.mindspark2.PreferencesManager
import com.example.mindspark2.TTSManager
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class CameraActivity : ComponentActivity() {

    private lateinit var ttsManager: TTSManager
    private lateinit var prefsManager: PreferencesManager

    // Google Cloud Translate API Key එක මෙතැනට යොදන්න
    private val apiKey = "YOUR_GOOGLE_TRANSLATE_API_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ttsManager = TTSManager(this)
        prefsManager = PreferencesManager(this)

        setContent {
            Mindspark2Theme {
                val navController = rememberNavController()

                CameraScreen(
                    navController = navController,
                    onGestureRecognized = ::onGestureRecognized
                )
            }
        }
    }

    private fun onGestureRecognized(translatedText: String) {
        // Network Request එකක් නිසා Background Thread (Dispatchers.IO) එකක ක්‍රියාත්මක කරයි
        lifecycleScope.launch(Dispatchers.IO) {

            // 1. Model එකෙන් එන Sinhala Text එක English වලට Translate කර ගැනීම
            val englishText = translateSinhalaToEnglishDirect(translatedText)

            // 2. UI සහ Audio සකස් කිරීමට නැවත Main Thread එකට මාරු වීම
            withContext(Dispatchers.Main) {
                if (prefsManager.autoPlayAudio) {
                    // සිංහල හඬ වාදනය කිරීම
                    ttsManager.speakAndVibrate(translatedText)

                    // translation එක සාර්ථක නම් ඉංග්‍රීසි හඬද වාදනය කිරීම
                    if (englishText.isNotEmpty()) {
                        ttsManager.speakAndVibrate(englishText)
                    }
                }
            }
        }
    }

    // වෙනම Files කිසිවක් නැතුව Direct HTTP Call එකක් මගින් Translate කරන Function එක
    private fun translateSinhalaToEnglishDirect(sinhalaText: String): String {
        return try {
            val urlString = "https://translation.googleapis.com/language/translate/v2?key=$apiKey"
            val url = URL(urlString)

            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            urlConnection.doOutput = true
            urlConnection.connectTimeout = 5000
            urlConnection.readTimeout = 5000

            // Request Payload එක සකස් කිරීම (Sinhala "si" -> English "en")
            val jsonInput = JSONObject().apply {
                put("q", sinhalaText)
                put("source", "si")
                put("target", "en")
                put("format", "text")
            }

            val os: OutputStream = urlConnection.outputStream
            val input = jsonInput.toString().toByteArray(charset("utf-8"))
            os.write(input, 0, input.size)
            os.close()

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(urlConnection.inputStream, "utf-8"))
                val response = StringBuilder()
                var responseLine: String?
                while (reader.readLine().also { responseLine = it } != null) {
                    response.append(responseLine!!.trim { it <= ' ' })
                }

                val jsonResponse = JSONObject(response.toString())
                val translations: JSONArray = jsonResponse.getJSONObject("data").getJSONArray("translations")

                // Translate වූ English Text එක Return කරයි
                translations.getJSONObject(0).getString("translatedText")
            } else {
                Log.e("Translation", "API Response Error: $responseCode")
                ""
            }
        } catch (e: Exception) {
            Log.e("Translation", "Network Error: ${e.message}")
            "" // Internet නැතිවිට App එක Crash නොවී හිස් string එකක් ලබා දෙයි (Fallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::ttsManager.isInitialized) {
            ttsManager.shutdown()
        }
    }
}
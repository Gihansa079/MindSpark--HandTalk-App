package com.example.mindspark2.legal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Two offline legal-information screens. Review all statements before release. */
class LegalPagesActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE = "legal_page"
        const val PAGE_PRIVACY = "privacy"
        const val PAGE_TERMS = "terms"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isPrivacy = intent.getStringExtra(EXTRA_PAGE) != PAGE_TERMS
        setContent {
            MaterialTheme {
                LegalPageScreen(isPrivacy = isPrivacy, onBack = { finish() })
            }
        }
    }
}

private val navy = Color(0xFF042F62)

@Composable
private fun LegalPageScreen(isPrivacy: Boolean, onBack: () -> Unit) {
    val title = if (isPrivacy) "Privacy Policy" else "Terms of Service"
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF3F7FC))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(navy)
                .statusBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("MindSpark / HandTalk", color = navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Draft for project review • 20 October 2026", color = Color.DarkGray, fontSize = 12.sp)
            if (isPrivacy) PrivacyContent() else TermsContent()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(heading: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(heading, color = navy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(content, color = Color(0xFF263238), fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun PrivacyContent() {
    Section("1. About this app", "MindSpark / HandTalk is a Sinhala sign-language recognition and translation application developed as a university research project. This notice describes features that may process information. Confirm actual data handling before public release.")
    Section("2. Camera and media", "The app uses camera images to detect hand landmarks and recognize signs. Users can also choose images or videos from their device. Captured photos or recordings may be saved to device media storage when the user chooses to capture them.")
    Section("3. Account and recognition history", "Some features use a user account or user ID. Recognition results, translations, input type and related information may be sent to the application's backend to provide translation and history features. The exact stored fields and retention period should be confirmed with the backend implementation.")
    Section("4. Microphone and audio", "Video or audio-related features may request microphone permission when required. Text-to-speech features produce spoken translations. Check whether any audio is recorded, uploaded or stored before publishing this policy.")
    Section("5. Permissions", "Camera permission is used for sign recognition. Access to selected media is used when you choose an image or video. You can review or revoke app permissions in Android Settings; some features may then be unavailable.")
    Section("6. Sharing and security", "The app may communicate with a backend service to translate or save recognition results. Confirm the server operator, hosting location, security controls and any third-party service providers before releasing the app.")
    Section("7. Retention and deletion", "The app may keep recognition history and saved media. The project team should specify how long server records are kept and how users can request access or deletion. Device-saved photos and videos can generally be managed using the device's media tools.")
    Section("8. Contact", "For privacy questions or requests, contact the project team using the verified contact details supplied with the application. [ADD VERIFIED PROJECT CONTACT BEFORE RELEASE]")
}

@Composable
private fun TermsContent() {
    Section("1. Purpose", "MindSpark / HandTalk is intended for educational and research use, including experimentation with Sinhala sign-language recognition and translation.")
    Section("2. Recognition limitations", "Predictions are generated by machine-learning models and may be inaccurate. Do not rely on the app as the sole means of communication in medical, emergency, legal or other high-stakes situations.")
    Section("3. Appropriate use", "Use the app lawfully and respect the privacy and consent of people whose hands, images, voices or videos may be recorded or uploaded.")
    Section("4. Media and permissions", "You are responsible for ensuring that you have permission to use selected or captured media. Camera, media and other relevant permissions can be managed through Android Settings.")
    Section("5. Accounts and history", "If account and history features are available, keep your login information secure and do not attempt to access other users' information.")
    Section("6. Service availability", "Some features depend on network connectivity or a backend service and may not always be available. The project may update features or models during development.")
    Section("7. Feedback", "You may report errors or provide feedback through the contact method offered by the project team. [ADD VERIFIED PROJECT CONTACT BEFORE RELEASE]")
    Section("8. Project status", "These are draft terms for a university project. Review the final wording, project ownership and applicable requirements before public distribution.")
}

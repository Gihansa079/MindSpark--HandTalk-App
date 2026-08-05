package com.example.mindspark2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var database: AppDatabase
    private var selectedVoiceState = "Female"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefsManager = PreferencesManager(this)
        database = AppDatabase.getDatabase(applicationContext)
        selectedVoiceState = intent.getStringExtra("SELECTED_VOICE") ?: prefsManager.voiceGender

        setContent {
            val savedProfile by database.userDao().getUserProfile().collectAsStateWithLifecycle(initialValue = null)
            val isDark = remember { mutableStateOf(prefsManager.theme == "Dark") }

            Mindspark2Theme(darkTheme = isDark.value) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProfileScreen(
                        prefsManager = prefsManager,
                        userProfileEntity = savedProfile,
                        onVoiceChanged = { newVoice ->
                            selectedVoiceState = newVoice
                            returnVoiceResult(newVoice)
                        },
                        onBackClick = {
                            returnVoiceResult(selectedVoiceState)
                            finish()
                        },
                        onLogoutClick = { performLogout() },
                        onThemeChanged = { newTheme ->
                            prefsManager.theme = newTheme
                            isDark.value = newTheme == "Dark"
                        },
                        onSaveProfile = { uName, uEmail, uMobile, uAge ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val userEntity = UserProfileEntity(
                                    id = 1,
                                    userName = uName,
                                    email = uEmail,
                                    mobileNumber = uMobile,
                                    age = uAge
                                )
                                database.userDao().insertOrUpdateUser(userEntity)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@ProfileActivity, "Profile Saved", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun returnVoiceResult(voice: String) {
        val resultIntent = Intent().apply {
            putExtra("SELECTED_VOICE", voice)
        }
        setResult(Activity.RESULT_OK, resultIntent)
    }

    private fun performLogout() {
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun ProfileScreen(
    prefsManager: PreferencesManager,
    userProfileEntity: UserProfileEntity?,
    onVoiceChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onThemeChanged: (String) -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }

    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    LaunchedEffect(userProfileEntity) {
        userProfileEntity?.let {
            userName = it.userName
            email = it.email
            mobileNumber = it.mobileNumber
            age = it.age
        }
    }

    var autoPlayAudio by remember { mutableStateOf(prefsManager.autoPlayAudio) }
    var hapticFeedback by remember { mutableStateOf(prefsManager.hapticFeedback) }
    var selectedVoice by remember { mutableStateOf(prefsManager.voiceGender) }
    var selectedTheme by remember { mutableStateOf(prefsManager.theme) }

    val isDark = selectedTheme == "Dark"
    val mainBgColor = if (isDark) Color(0xFF121212) else Color(0xFFF8FAFC)
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1E293B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainBgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0066CC))
                .statusBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = "←",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Settings",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF90CAF9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🧑‍💼", fontSize = 36.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = userName.ifEmpty { "User Name" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = email.ifEmpty { "No Email Set" },
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { isEditing = !isEditing },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEditing) "✔" else "✏️",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            AnimatedVisibility(visible = isEditing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Edit Personal Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066CC)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = { Text("User Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it },
                            label = { Text("Mobile Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isEditing = false
                                onSaveProfile(userName, email, mobileNumber, age)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))
                        ) {
                            Text("Save Changes", color = Color.White)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferences",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066CC)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-play Voice",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                            Text(
                                text = "Play audio output after recognition",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = autoPlayAudio,
                            onCheckedChange = {
                                autoPlayAudio = it
                                prefsManager.autoPlayAudio = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0066CC)
                            )
                        )
                    }

                    HorizontalDivider(
                        color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Haptic Feedback",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                            Text(
                                text = "Vibrate on sign detection",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = hapticFeedback,
                            onCheckedChange = {
                                hapticFeedback = it
                                prefsManager.hapticFeedback = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0066CC)
                            )
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SettingsRowItem(
                        icon = "👤",
                        title = "Account",
                        textColor = primaryTextColor,
                        onClick = { isEditing = true }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9))

                    SettingsRowItem(
                        icon = "🌐",
                        title = "Language",
                        value = "English",
                        textColor = primaryTextColor,
                        onClick = {
                            prefsManager.language = "English"
                            Toast.makeText(context, "Language set to English", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9))

                    SettingsRowItem(
                        icon = "🔊",
                        title = "Voice Gender",
                        value = if (selectedVoice == "Female") "👩 Female" else "👨 Male",
                        textColor = primaryTextColor,
                        onClick = {
                            selectedVoice = if (selectedVoice == "Female") "Male" else "Female"
                            prefsManager.voiceGender = selectedVoice
                            onVoiceChanged(selectedVoice)
                            Toast.makeText(context, "Voice changed to $selectedVoice", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9))

                    SettingsRowItem(
                        icon = "🎨",
                        title = "Theme",
                        value = selectedTheme,
                        textColor = primaryTextColor,
                        onClick = {
                            val newTheme = if (selectedTheme == "Light") "Dark" else "Light"
                            selectedTheme = newTheme
                            onThemeChanged(newTheme)
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9))

                    SettingsRowItem(
                        icon = "❓",
                        title = "Help & Support",
                        textColor = primaryTextColor,
                        onClick = {
                            Toast.makeText(context, "Help & Support clicked", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9))

                    SettingsRowItem(
                        icon = "ℹ️",
                        title = "About Us",
                        textColor = primaryTextColor,
                        onClick = {
                            Toast.makeText(context, "MindSpark2 v1.0", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLogoutClick() }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "↪️", fontSize = 20.sp, color = Color.Red)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Logout",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: String,
    title: String,
    value: String? = null,
    textColor: Color = Color(0xFF1E293B),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "›",
                fontSize = 22.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
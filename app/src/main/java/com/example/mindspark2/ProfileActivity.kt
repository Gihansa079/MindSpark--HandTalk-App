package com.example.mindspark2

import android.app.Activity
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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

/**
 * ProfileActivity: පරිශීලකයාගේ Profile සහ Settings කළමනාකරණය කරන ප්‍රධාන Activity එක.
 */
class ProfileActivity : ComponentActivity() {

    // SharedPreferences සහ Local Room Database කළමනාකරණය සඳහා variables
    private lateinit var prefsManager: PreferencesManager
    private lateinit var database: AppDatabase

    // Lazy initialization භාවිතයෙන් ApiService එක අවශ්‍ය වූ විට පමණක් සාදා ගනී
    private val apiService by lazy { ApiService.create() }
    private var selectedVoiceState = "Female"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen edge-to-edge UI එක ලබා දීම
        enableEdgeToEdge()

        // Helper classes initialize කිරීම
        prefsManager = PreferencesManager(this)
        database = AppDatabase.getDatabase(applicationContext)

        // Intent එකෙන් ලැබෙන Voice එක හෝ Preference වල ඇති Voice එක ලබා ගැනීම
        selectedVoiceState = intent.getStringExtra("SELECTED_VOICE") ?: prefsManager.voiceGender

        // Backend එකෙන් Profile Data ලබා ගැනීමට Call එකක් දීම
        fetchProfileDataFromBackend()

        // Jetpack Compose UI එක සක්‍රිය කිරීම
        setContent {
            // Room Database එකෙන් User Profile එක Live Data / Flow ලෙස Observe කිරීම
            val savedProfile by database.userDao().getUserProfile().collectAsStateWithLifecycle(initialValue = null)

            // Theme එක Light ද Dark ද යන්න Preferences මත පදනම්ව තීරණය කිරීම
            val isDark = remember { mutableStateOf(prefsManager.theme == "Dark") }

            Mindspark2Theme(darkTheme = isDark.value) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Profile UI Composable එක Call කිරීම
                    ProfileScreen(
                        prefsManager = prefsManager,
                        userProfileEntity = savedProfile,
                        onVoiceChanged = { newVoice ->
                            selectedVoiceState = newVoice
                            returnVoiceResult(newVoice)
                        },
                        onBackClick = {
                            returnVoiceResult(selectedVoiceState)
                            finish() // Activity එක වසා දැමීම
                        },
                        onLogoutClick = { performLogout() },
                        onThemeChanged = { newTheme ->
                            prefsManager.theme = newTheme
                            isDark.value = newTheme == "Dark"
                        },
                        onSaveProfile = { uName, uEmail, uMobile, uAge ->
                            saveUserProfileData(uName, uEmail, uMobile, uAge)
                        },
                        onHelpSupportClick = {
                            openHelpSupportEmail()
                        }
                    )
                }
            }
        }
    }

    /**
     * Customer Support සඳහා Email App එක Open කරන Intent එකක් ක්‍රියාත්මක කිරීම.
     */
    private fun openHelpSupportEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@handtalk.com")
                putExtra(Intent.EXTRA_SUBJECT, "HandTalk App Support & Query")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * User Details මුලින්ම Local Database (Room) එකේ Save කර පසුව Remote API එකට Sync කිරීම.
     */
    private fun saveUserProfileData(uName: String, uEmail: String, uMobile: String, uAge: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Local Room DB එකට Save කිරීම (Offline Support)
            val userEntity = UserProfileEntity(
                id = 1,
                userName = uName,
                email = uEmail,
                mobileNumber = uMobile,
                age = uAge
            )
            database.userDao().insertOrUpdateUser(userEntity)

            // 2. Network එක හරහා Server එකට Data Send කිරීම
            try {
                val response = apiService.updateUserProfile(
                    UserProfileUpdateRequest(
                        user_name = uName,
                        email = uEmail,
                        mobile_number = uMobile,
                        age = uAge
                    )
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "Profile Synced Successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Saved Locally", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Saved Locally (Offline)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Server එකෙන් User Profile Data Fetch කර Local Database එක Update කිරීම.
     */
    private fun fetchProfileDataFromBackend() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUserEmail = prefsManager.userEmail
                if (currentUserEmail.isNotEmpty()) {
                    var response = apiService.getUserProfile(currentUserEmail)

                    // පළමු EndPoint එක Fail වුවහොත් Alternate Path එක භාවිත කිරීම
                    if (!response.isSuccessful) {
                        response = apiService.getUserProfileByPath(currentUserEmail)
                    }

                    if (response.isSuccessful && response.body() != null) {
                        val remoteData = response.body()!!
                        val userEntity = UserProfileEntity(
                            id = 1,
                            userName = remoteData.user_name ?: "",
                            email = if (!remoteData.email.isNullOrEmpty()) remoteData.email!! else currentUserEmail,
                            mobileNumber = remoteData.mobile_number ?: "",
                            age = remoteData.age ?: ""
                        )
                        database.userDao().insertOrUpdateUser(userEntity)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * තෝරාගත් Voice Option එක කලින් Activity එකට Intent Result එකක් ලෙස යැවීම.
     */
    private fun returnVoiceResult(voice: String) {
        val resultIntent = Intent().apply {
            putExtra("SELECTED_VOICE", voice)
        }
        setResult(Activity.RESULT_OK, resultIntent)
    }

    /**
     * Logout වීමේදී Preferences සහ Local Database Clear කර Login Screen එකට Redirect කිරීම.
     */
    private fun performLogout() {
        prefsManager.clearSession()
        lifecycleScope.launch(Dispatchers.IO) {
            database.userDao().clearUserProfile()
        }
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

/**
 * Profile Screen එක සදහා වන ප්‍රධාන Jetpack Compose Layout එක.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    prefsManager: PreferencesManager,
    userProfileEntity: UserProfileEntity?,
    onVoiceChanged: (String) -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onThemeChanged: (String) -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit,
    onHelpSupportClick: () -> Unit
) {
    val context = LocalContext.current

    // Edit Form එක Toggle කිරීමට State එකක්
    var isEditing by remember { mutableStateOf(false) }

    // Text Fields වල Data රඳවා ගන්නා States
    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    // Database එකෙන් Data ලැබෙන විට Text Fields වල අගයන් Update කිරීම
    LaunchedEffect(userProfileEntity) {
        if (!isEditing) {
            userProfileEntity?.let {
                userName = it.userName
                email = if (it.email.isNotEmpty()) it.email else prefsManager.userEmail
                mobileNumber = it.mobileNumber
                age = it.age
            }
        }
    }

    // Settings / Preferences States
    var autoPlayAudio by remember { mutableStateOf(prefsManager.autoPlayAudio) }
    var hapticFeedback by remember { mutableStateOf(prefsManager.hapticFeedback) }
    var selectedVoice by remember { mutableStateOf(prefsManager.voiceGender) }
    var selectedTheme by remember { mutableStateOf(prefsManager.theme) }

    // Dynamic Colors (Dark / Light Theme අනුව වෙනස් වේ)
    val isDark = selectedTheme == "Dark"
    val cardBgColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1E293B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0066CC))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()) // Screen එක Scroll කිරීමට හැකි කිරීම
        ) {
            // Header Section - User Avatar සහ Name/Email පෙන්වන ස්ථානය
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0066CC))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF90CAF9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = userName.ifEmpty { "User Name" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = email.ifEmpty { "No Email Set" },
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Edit Profile Button (Toggle switch for isEditing)
                    IconButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color.White
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // Edit Profile Form - isEditing = true වූ විට පමණක් පෙන්වයි
                AnimatedVisibility(visible = isEditing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Edit Personal Details",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0066CC)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Input Fields
                            OutlinedTextField(
                                value = userName,
                                onValueChange = { if (it.length <= 100) userName = it },
                                label = { Text("User Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { if (it.length <= 254) email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = mobileNumber,
                                onValueChange = { if (it.length <= 15) mobileNumber = it },
                                label = { Text("Mobile Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = age,
                                onValueChange = { if (it.length <= 3) age = it },
                                label = { Text("Age") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Save Button
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

                // Preferences Section - Switch Toggle Buttons සහිත Card එක
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Preferences",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0066CC)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto-play Voice Toggle
                        PreferenceToggleRow(
                            title = "Auto-play Voice",
                            subtitle = "Play audio output after recognition",
                            checked = autoPlayAudio,
                            textColor = primaryTextColor,
                            onCheckedChange = {
                                autoPlayAudio = it
                                prefsManager.autoPlayAudio = it
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Haptic Feedback Toggle
                        PreferenceToggleRow(
                            title = "Haptic Feedback",
                            subtitle = "Vibrate on sign detection",
                            checked = hapticFeedback,
                            textColor = primaryTextColor,
                            onCheckedChange = {
                                hapticFeedback = it
                                prefsManager.hapticFeedback = it
                            }
                        )
                    }
                }

                // Options List Section - Account, Language, Voice Gender, Theme, etc.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBgColor)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        // Account
                        SettingsRowItem(
                            icon = "👤",
                            title = "Account",
                            textColor = primaryTextColor,
                            onClick = { isEditing = true }
                        )
                        HorizontalDivider()

                        // Language
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
                        HorizontalDivider()

                        // Voice Gender Toggle
                        SettingsRowItem(
                            icon = "🔊",
                            title = "Voice Gender",
                            value = if (selectedVoice == "Female") "Female" else "Male",
                            textColor = primaryTextColor,
                            onClick = {
                                selectedVoice = if (selectedVoice == "Female") "Male" else "Female"
                                prefsManager.voiceGender = selectedVoice
                                onVoiceChanged(selectedVoice)
                                Toast.makeText(context, "Voice changed to $selectedVoice", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()

                        // Theme Toggle (Light/Dark)
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
                        HorizontalDivider()

                        // Help & Support Email Link
                        SettingsRowItem(
                            icon = "❓",
                            title = "Help & Support",
                            textColor = primaryTextColor,
                            onClick = {
                                onHelpSupportClick()
                            }
                        )
                        HorizontalDivider()

                        // About Us
                        SettingsRowItem(
                            icon = "ℹ️",
                            title = "About Us",
                            textColor = primaryTextColor,
                            onClick = {
                                Toast.makeText(context, "MindSpark2 v1.0", Toast.LENGTH_SHORT).show()
                            }
                        )
                        HorizontalDivider()

                        // Logout Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLogoutClick() }
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = Color(0xFFE53935)
                            )
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
}

/**
 * Switch Toggle එකක් සහිත Single Preference Row එකක් සෑදීම සඳහා වන Reusable Composable එකක්.
 */
@Composable
fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    textColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0066CC)
            )
        )
    }
}

/**
 * Icon එකක්, මාතෘකාවක්, අගයක් (Optional) සහ ඊතල සලකුණක් සහිත Clickable Settings Item එකක්.
 */
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
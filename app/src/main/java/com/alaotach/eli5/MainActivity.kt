package com.alaotach.eli5

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alaotach.eli5.assistant.CircleSnipActivity
import com.alaotach.eli5.assistant.Eli5AssistantManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alaotach.eli5.ui.theme.Eli5Theme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val PurpleAccent = Color(0xFF7C3AED)
val PurpleAccentDark = Color(0xFF8B5CF6)
val PurpleLightBg = Color(0xFFF5F3FF)
val CardDarkBg = Color(0xFF1C1826)

class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsManager.getLanguage(newBase)
        val langObj = LanguageManager.getLanguageByEnglishName(lang)
        val locale = java.util.Locale.forLanguageTag(langObj.code)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themePref = SettingsManager.getTheme(this)
        val initialDark = when (themePref) {
            "dark" -> true
            "light" -> false
            else -> {
                val nightMask = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightMask == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        window.setBackgroundDrawable(ColorDrawable(if (initialDark) 0xFF13101C.toInt() else 0xFFF9FAFB.toInt()))

        enableEdgeToEdge()

        checkForAppUpdates()

        setContent {
            val context = LocalContext.current
            var themePreference by remember {
                mutableStateOf(SettingsManager.getTheme(this@MainActivity))
            }
            val isDark = when (themePreference) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            val prefs = context.getSharedPreferences("eli5_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("onboarding_done", false)) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
                return@setContent
            }

            Eli5Theme(darkTheme = isDark, dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (isDark) Color(0xFF13101C) else Color(0xFFF9FAFB)
                ) { innerPadding ->
                    MainScreen(
                        themePref = themePreference,
                        isDark = isDark,
                        onThemeChange = {
                            SettingsManager.setTheme(this@MainActivity, it)
                            themePreference = it
                        },
                        onCheckUpdates = { triggerManualUpdateCheck() },
                        onShowTutorial = {
                            startActivity(Intent(this@MainActivity, HowToUseActivity::class.java))
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkForAppUpdates() {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installListener)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager.startUpdateFlow(
                        appUpdateInfo,
                        this,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                } catch (_: Exception) {}
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
            }
        }
    }

    private fun triggerManualUpdateCheck() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val updateType = if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    AppUpdateType.FLEXIBLE
                } else {
                    AppUpdateType.IMMEDIATE
                }
                try {
                    appUpdateManager.startUpdateFlow(
                        appUpdateInfo,
                        this,
                        AppUpdateOptions.newBuilder(updateType).build()
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not start update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "ELI5 is up to date!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Check failed: Play Store not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    appUpdateManager.completeUpdate()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(installListener)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themePref: String,
    isDark: Boolean,
    onThemeChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onShowTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var history by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var itemsToShow by remember { mutableStateOf(10) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val accent = if (isDark) PurpleAccentDark else PurpleAccent
    val cardBg = if (isDark) CardDarkBg else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2C243B) else Color(0xFFE9E5F2)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1C1826)
    val secondaryTextColor = if (isDark) Color(0xFF9E9AA8) else Color(0xFF6B6678)

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var tryItText by remember { mutableStateOf("") }
    var isStatusDismissed by remember { mutableStateOf(SettingsManager.isStatusBannerDismissed(context)) }
    var isTryItDismissed by remember { mutableStateOf(SettingsManager.isTryItDismissed(context)) }
    var currentLanguage by remember { mutableStateOf(SettingsManager.getLanguage(context)) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        history = HistoryManager.getHistory(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "ELI5 Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF261F36) else PurpleLightBg)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ELI5",
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick Language Switcher Pill
                val currentLangObj = LanguageManager.getLanguageByEnglishName(currentLanguage)
                Surface(
                    modifier = Modifier.clickable { showLanguagePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF261F36) else PurpleLightBg,
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF3B2D54) else Color(0xFFDDD6FE))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currentLangObj.flag, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentLangObj.nativeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(R.string.header_change_lang),
                            tint = secondaryTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.header_settings),
                        tint = primaryTextColor
                    )
                }
            }
        }

        // 2. Status Banner: "ELI5 is on" or "Permission needed"
        if (!isStatusDismissed) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable {
                        if (!hasOverlayPermission) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                color = if (hasOverlayPermission) {
                    if (isDark) Color(0xFF13271B) else Color(0xFFE8F5E9)
                } else {
                    if (isDark) Color(0xFF332014) else Color(0xFFFFF3E0)
                },
                border = BorderStroke(
                    1.dp,
                    if (hasOverlayPermission) {
                        if (isDark) Color(0xFF1E462B) else Color(0xFFA5D6A7)
                    } else {
                        if (isDark) Color(0xFF593318) else Color(0xFFFFCC80)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasOverlayPermission) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (hasOverlayPermission) stringResource(R.string.status_on) else stringResource(R.string.status_permission_needed),
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (hasOverlayPermission) stringResource(R.string.status_ready) else stringResource(R.string.status_tap_to_allow),
                                color = secondaryTextColor,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!hasOverlayPermission) {
                            Text(
                                text = "Fix →",
                                color = Color(0xFFEF6C00),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                isStatusDismissed = true
                                SettingsManager.setStatusBannerDismissed(context, true)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.status_dismiss),
                                tint = if (hasOverlayPermission) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else Color(0xFFEF6C00),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Circle to ELI5 (Screen Snip / Assistant Feature)
        CircleToEli5Card(
            isDark = isDark,
            cardBg = cardBg,
            cardBorder = cardBorder,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            accent = accent
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3b. Explanation Language Feature Card (Prominently showcasing multi-language support)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🌍", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.lang_card_title),
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                fontSize = 15.sp
                            )
                            val langObj = LanguageManager.getLanguageByEnglishName(currentLanguage)
                            Text(
                                text = stringResource(R.string.lang_card_default_prefix, langObj.displayName),
                                color = accent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.clickable { showLanguagePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF261F36) else PurpleLightBg
                    ) {
                        Text(
                            text = stringResource(R.string.lang_card_btn_more),
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Popular Language Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("English", "Hindi", "Spanish", "French").forEach { langName ->
                        val lang = LanguageManager.getLanguageByEnglishName(langName)
                        val isSelected = currentLanguage.equals(langName, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    currentLanguage = langName
                                    SettingsManager.setLanguage(context, langName)
                                    AnalyticsManager.logLanguageChanged(context, langName, "home_chip")
                                    (context as? ComponentActivity)?.recreate()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) accent else if (isDark) Color(0xFF261F36) else PurpleLightBg,
                            border = if (isSelected) null else BorderStroke(1.dp, cardBorder)
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.nativeName}",
                                color = if (isSelected) Color.White else primaryTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Try-It Text Box
        if (!isTryItDismissed) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.try_card_title),
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            fontSize = 15.sp
                        )
                        IconButton(
                            onClick = {
                                isTryItDismissed = true
                                SettingsManager.setTryItDismissed(context, true)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = secondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.try_card_desc),
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = tryItText,
                        onValueChange = { tryItText = it },
                        placeholder = { Text(stringResource(R.string.try_card_placeholder), fontSize = 13.sp, color = secondaryTextColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val sampleText = stringResource(R.string.try_card_sample_text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            tryItText = sampleText
                        }) {
                            Text(stringResource(R.string.try_card_insert_sample), color = accent, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (tryItText.isNotBlank()) {
                                    val intent = Intent(context, ExplainOverlayActivity::class.java).apply {
                                        putExtra(ExplainOverlayActivity.EXTRA_TEXT, tryItText.trim())
                                    }
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Please enter some text first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(stringResource(R.string.try_card_btn_explain), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 4. Recent Activity Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_title),
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                style = MaterialTheme.typography.titleMedium
            )

            if (history.isNotEmpty()) {
                TextButton(onClick = {
                    HistoryManager.clearHistory(context)
                    history = emptyList()
                }) {
                    Text(stringResource(R.string.history_clear_all), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }

        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.history_empty_title),
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.history_empty_desc),
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onShowTutorial,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.history_interactive_guide), color = Color.White)
                }
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                history.take(itemsToShow).forEach { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                val intent = Intent(context, ExplainOverlayActivity::class.java).apply {
                                    putExtra(ExplainOverlayActivity.EXTRA_TEXT, item.originalText)
                                    putExtra(ExplainOverlayActivity.EXTRA_RESULT, item.resultText)
                                    putExtra(ExplainOverlayActivity.EXTRA_MODE, item.mode)
                                }
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF261F36) else PurpleLightBg
                                ) {
                                    Text(
                                        text = item.mode,
                                        color = accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        HistoryManager.deleteItem(context, item)
                                        history = HistoryManager.getHistory(context)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.originalText.replace('\n', ' '),
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.resultText.replace('\n', ' '),
                                color = secondaryTextColor,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (history.size > itemsToShow) {
                    TextButton(
                        onClick = { itemsToShow += 10 },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 12.dp)
                    ) {
                        Text(stringResource(R.string.history_load_more), color = accent)
                    }
                }
            }
        }

        // 4b. Visual Tutorial Guide Section
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.guide_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = primaryTextColor
                )
                TextButton(onClick = onShowTutorial) {
                    Text(stringResource(R.string.guide_tour_btn), color = accent, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val guideSteps = listOf(
                Triple(R.drawable.tutorial_img_6, stringResource(R.string.guide_step1_title), stringResource(R.string.guide_step1_desc)),
                Triple(R.drawable.tutorial_img_7, stringResource(R.string.guide_step2_title), stringResource(R.string.guide_step2_desc)),
                Triple(R.drawable.tutorial_img_4, stringResource(R.string.guide_step3_title), stringResource(R.string.guide_step3_desc)),
                Triple(R.drawable.tutorial_img_8, stringResource(R.string.guide_step4_title), stringResource(R.string.guide_step4_desc))
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(guideSteps) { (imageRes, titleText, descText) ->
                    Surface(
                        modifier = Modifier
                            .width(200.dp)
                            .clickable { onShowTutorial() },
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Image(
                                painter = painterResource(imageRes),
                                contentDescription = titleText,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = titleText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = primaryTextColor
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = descText,
                                fontSize = 11.5.sp,
                                color = secondaryTextColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 5. Settings Bottom Sheet
    if (showSettingsDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var defaultMode by remember { mutableStateOf(SettingsManager.getDefaultMode(context)) }
        var language by remember { mutableStateOf(SettingsManager.getLanguage(context)) }
        var proofreadTone by remember { mutableStateOf(SettingsManager.getProofreadDefaultTone(context)) }
        var historyEnabled by remember { mutableStateOf(SettingsManager.isHistoryEnabled(context)) }
        var currentTheme by remember { mutableStateOf(SettingsManager.getTheme(context)) }

        ModalBottomSheet(
            onDismissRequest = { showSettingsDialog = false },
            sheetState = sheetState,
            containerColor = if (isDark) CardDarkBg else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = primaryTextColor
                    )
                    IconButton(
                        onClick = { showSettingsDialog = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Default Mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_default_mode),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = primaryTextColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ELI5", "Direct", "Detailed").forEach { mode ->
                            val isSelected = defaultMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        defaultMode = mode
                                        SettingsManager.setDefaultMode(context, mode)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accent else if (isDark) Color(0xFF261F36) else PurpleLightBg
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) Color.White else primaryTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                // Explanation & App Language
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.settings_exp_lang),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = primaryTextColor
                        )
                        Text(
                            text = "35+ available",
                            color = secondaryTextColor,
                            fontSize = 11.sp
                        )
                    }

                    val activeLang = LanguageManager.getLanguageByEnglishName(language)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguagePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF261F36) else PurpleLightBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = activeLang.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = activeLang.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_exp_lang_desc),
                                        color = secondaryTextColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                text = "Change ▾",
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Proofread Tone
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_proofread_tone),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = primaryTextColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Casual", "Formal", "Urgent", "Translate").forEach { tone ->
                            val isSelected = proofreadTone == tone
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        proofreadTone = tone
                                        SettingsManager.setProofreadDefaultTone(context, tone)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accent else if (isDark) Color(0xFF261F36) else PurpleLightBg
                            ) {
                                Text(
                                    text = tone,
                                    color = if (isSelected) Color.White else primaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Theme
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_theme),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = primaryTextColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("System" to "system", "Light" to "light", "Dark" to "dark").forEach { (label, valStr) ->
                            val isSelected = currentTheme == valStr
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        currentTheme = valStr
                                        onThemeChange(valStr)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accent else if (isDark) Color(0xFF261F36) else PurpleLightBg
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else primaryTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                // History Privacy Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_save_history),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = primaryTextColor
                        )
                        Text(
                            stringResource(R.string.settings_save_history_desc),
                            fontSize = 11.5.sp,
                            color = secondaryTextColor
                        )
                    }
                    Switch(
                        checked = historyEnabled,
                        onCheckedChange = {
                            historyEnabled = it
                            SettingsManager.setHistoryEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                HorizontalDivider(
                    color = cardBorder,
                    thickness = 1.dp
                )

                // Actions List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isStatusDismissed || isTryItDismissed) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SettingsManager.setStatusBannerDismissed(context, false)
                                    SettingsManager.setTryItDismissed(context, false)
                                    isStatusDismissed = false
                                    isTryItDismissed = false
                                    Toast.makeText(context, context.getString(R.string.settings_restored_toast), Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF261F36) else PurpleLightBg
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.settings_restore_cards),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accent
                                )
                                Text("↺", fontSize = 14.sp, color = accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSettingsDialog = false
                                Eli5AssistantManager.openAssistantSettings(context)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF261F36) else PurpleLightBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.settings_assistant_setup),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accent
                            )
                            Text("⚙", fontSize = 14.sp, color = accent)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCheckUpdates() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF261F36) else PurpleLightBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.settings_check_updates),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                            Text("→", fontSize = 13.sp, color = accent)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SettingsManager.PRIVACY_URL))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF261F36) else PurpleLightBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.settings_privacy_policy),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryTextColor
                            )
                            Text("↗", fontSize = 13.sp, color = secondaryTextColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { showSettingsDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.settings_btn_done), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = currentLanguage,
            onSelectLanguage = { selected ->
                currentLanguage = selected
                SettingsManager.setLanguage(context, selected)
                AnalyticsManager.logLanguageChanged(context, selected, "home_picker_dialog")
                showLanguagePicker = false
                (context as? ComponentActivity)?.recreate()
            },
            onDismiss = { showLanguagePicker = false },
            isDark = isDark,
            accent = accent,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            cardBg = if (isDark) CardDarkBg else Color.White,
            cardBorder = cardBorder
        )
    }
}

@Composable
fun CircleToEli5Card(
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    accent: Color
) {
    val context = LocalContext.current
    var isAssistant by remember { mutableStateOf(Eli5AssistantManager.isDefaultAssistant(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAssistant = Eli5AssistantManager.isDefaultAssistant(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(
            1.dp,
            if (isAssistant) (if (isDark) Color(0xFF1E462B) else Color(0xFFA5D6A7)) else cardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.circle_card_title),
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isAssistant) stringResource(R.string.circle_card_active) else stringResource(R.string.circle_card_inactive),
                            color = if (isAssistant) Color(0xFF10B981) else secondaryTextColor,
                            fontSize = 12.sp,
                            fontWeight = if (isAssistant) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAssistant) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF3B0764) else Color(0xFFF3E8FF)),
                    border = BorderStroke(1.dp, if (isAssistant) Color(0xFF10B981) else Color(0xFF8B5CF6))
                ) {
                    Text(
                        text = if (isAssistant) stringResource(R.string.circle_card_badge_active) else stringResource(R.string.circle_card_badge_new),
                        color = if (isAssistant) Color(0xFF10B981) else Color(0xFF8B5CF6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.circle_card_desc),
                color = secondaryTextColor,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, CircleSnipActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF4C1D95) else Color(0xFFDDD6FE)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accent
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(stringResource(R.string.circle_card_btn_demo), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        Eli5AssistantManager.openAssistantSettings(context)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAssistant) (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)) else accent,
                        contentColor = if (isAssistant) primaryTextColor else Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = if (isAssistant) stringResource(R.string.circle_card_btn_settings) else stringResource(R.string.circle_card_btn_set_assistant),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerDialog(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean,
    accent: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    cardBg: Color,
    cardBorder: Color
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery) {
        LanguageManager.searchLanguages(searchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cardBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.picker_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = primaryTextColor
                    )
                    Text(
                        text = stringResource(R.string.picker_subtitle),
                        color = secondaryTextColor,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.picker_search_hint), color = secondaryTextColor, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = accent, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = primaryTextColor,
                    unfocusedTextColor = primaryTextColor,
                    cursorColor = accent
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Language List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLanguages) { lang ->
                    val isSelected = currentLanguage.equals(lang.englishName, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLanguage(lang.englishName) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE)) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.5.dp, accent) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = lang.flag, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = lang.nativeName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = if (isSelected) accent else primaryTextColor
                                    )
                                    Text(
                                        text = lang.englishName,
                                        fontSize = 12.sp,
                                        color = secondaryTextColor
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

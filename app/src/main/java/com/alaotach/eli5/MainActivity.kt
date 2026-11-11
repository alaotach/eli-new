package com.alaotach.eli5

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.text.TextUtilsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alaotach.eli5.assistant.CircleSnipActivity
import com.alaotach.eli5.assistant.Eli5AssistantManager
import com.alaotach.eli5.ui.Eli5OrganicBackground
import com.alaotach.eli5.ui.MarkdownTextFormatter
import com.alaotach.eli5.ui.theme.AccentOrange
import com.alaotach.eli5.ui.theme.CardBorderDark
import com.alaotach.eli5.ui.theme.CardBorderLight
import com.alaotach.eli5.ui.theme.Eli5Theme
import com.alaotach.eli5.ui.theme.LavenderLight
import com.alaotach.eli5.ui.theme.PurplePrimary
import com.alaotach.eli5.ui.theme.PurpleSecondary
import com.alaotach.eli5.ui.theme.SuccessGreen
import com.alaotach.eli5.ui.theme.SurfaceDark
import com.alaotach.eli5.ui.theme.SurfaceWhite
import com.alaotach.eli5.ui.theme.TextPrimary
import com.alaotach.eli5.ui.theme.TextPrimaryDark
import com.alaotach.eli5.ui.theme.TextSecondary
import com.alaotach.eli5.ui.theme.TextSecondaryDark
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch

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
        config.setLayoutDirection(locale)
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val lang = SettingsManager.getLanguage(this)
        val langObj = LanguageManager.getLanguageByEnglishName(lang)
        val locale = java.util.Locale.forLanguageTag(langObj.code)
        newConfig.setLocale(locale)
        newConfig.setLayoutDirection(locale)
        try {
            resources.updateConfiguration(newConfig, resources.displayMetrics)
        } catch (_: Exception) {}
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
        window.setBackgroundDrawable(ColorDrawable(if (initialDark) 0xFF120E1A.toInt() else 0xFFF8F5FF.toInt()))

        enableEdgeToEdge()
        checkForAppUpdates()

        setContent {
            val context = LocalContext.current
            val lang = SettingsManager.getLanguage(context)
            val langObj = LanguageManager.getLanguageByEnglishName(lang)
            val locale = java.util.Locale.forLanguageTag(langObj.code)
            val isRtl = TextUtilsCompat.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
            val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

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

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDir
            ) {
                Eli5Theme(darkTheme = isDark, dynamicColor = false) {
                    Eli5App(
                        themePref = themePreference,
                        isDark = isDark,
                        onThemeChange = {
                            SettingsManager.setTheme(this@MainActivity, it)
                            themePreference = it
                        },
                        onCheckUpdates = { triggerManualUpdateCheck() },
                        onShowTutorial = {
                            startActivity(Intent(this@MainActivity, HowToUseActivity::class.java))
                        }
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
                        AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
                    )
                } catch (_: Exception) {}
            }
        }
    }

    private fun triggerManualUpdateCheck() {
        if (!::appUpdateManager.isInitialized) {
            appUpdateManager = AppUpdateManagerFactory.create(this)
        }
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        appUpdateManager.startUpdateFlow(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
                        )
                    } else {
                        Toast.makeText(this, "An update is available on Play Store", Toast.LENGTH_SHORT).show()
                    }
                }
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    Toast.makeText(this, "You're using the latest version of ELI5!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Unable to check updates right now", Toast.LENGTH_SHORT).show()
                }
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

// -------------------------------------------------------------------------------------------------
// ELI5 App Container & Navigation
// -------------------------------------------------------------------------------------------------

@Composable
fun Eli5App(
    themePref: String,
    isDark: Boolean,
    onThemeChange: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onShowTutorial: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ttsHelper = remember { TtsHelper(context) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: History, 2: More
    var activeExplanationItem by remember { mutableStateOf<HistoryItem?>(null) }
    var currentLanguage by remember { mutableStateOf(SettingsManager.getLanguage(context)) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Dynamic RTL/LTR support based on current selected language
    val langObj = LanguageManager.getLanguageByEnglishName(currentLanguage)
    val locale = java.util.Locale.forLanguageTag(langObj.code)
    val isRtl = TextUtilsCompat.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    val layoutDir = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    // System Back Press handling:
    // 1. If viewing an explanation -> close explanation & return to previous tab
    // 2. If on History or More tab -> return to Home tab
    // 3. If on Home tab -> double-tap back within 2 seconds to exit the app
    BackHandler(enabled = true) {
        if (activeExplanationItem != null) {
            ttsHelper.stop()
            activeExplanationItem = null
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Color tokens
    val primaryText = if (isDark) TextPrimaryDark else TextPrimary
    val secondaryText = if (isDark) TextSecondaryDark else TextSecondary
    val surfaceColor = if (isDark) SurfaceDark else SurfaceWhite
    val borderColor = if (isDark) CardBorderDark else CardBorderLight
    val accentPurple = if (isDark) PurpleSecondary else PurplePrimary
    val lightLavender = if (isDark) Color(0xFF2E2245) else LavenderLight

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                history = HistoryManager.getHistory(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ttsHelper.shutdown()
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDir
    ) {
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            if (activeExplanationItem == null) {
                Eli5BottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    isDark = isDark,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    accentPurple = accentPurple,
                    lightLavender = lightLavender
                )
            }
        }
    ) { innerPadding ->
        Eli5OrganicBackground(isDark = isDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (activeExplanationItem != null) {
                    ExplanationScreen(
                        item = activeExplanationItem!!,
                        language = currentLanguage,
                        isDark = isDark,
                        surfaceColor = surfaceColor,
                        borderColor = borderColor,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentPurple = accentPurple,
                        lightLavender = lightLavender,
                        ttsHelper = ttsHelper,
                        onBack = {
                            activeExplanationItem = null
                            history = HistoryManager.getHistory(context)
                        }
                    )
                } else {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            history = history,
                            currentLanguage = currentLanguage,
                            isDark = isDark,
                            surfaceColor = surfaceColor,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            accentPurple = accentPurple,
                            lightLavender = lightLavender,
                            onOpenLanguagePicker = { showLanguagePicker = true },
                            onOpenSettings = { selectedTab = 2 },
                            onOpenHistory = { selectedTab = 1 },
                            onSelectHistoryItem = { item ->
                                activeExplanationItem = item
                            },
                            onDeleteItem = { item ->
                                HistoryManager.deleteItem(context, item)
                                history = HistoryManager.getHistory(context)
                                Toast.makeText(context, R.string.deleted_from_history, Toast.LENGTH_SHORT).show()
                            },
                            onClearHistory = {
                                HistoryManager.clearHistory(context)
                                history = emptyList()
                                Toast.makeText(context, R.string.history_cleared, Toast.LENGTH_SHORT).show()
                            },
                            onExplainText = { text ->
                                val intent = Intent(context, ExplainOverlayActivity::class.java).apply {
                                    putExtra(ExplainOverlayActivity.EXTRA_TEXT, text)
                                    putExtra(ExplainOverlayActivity.EXTRA_MODE, "ELI5")
                                    putExtra(ExplainOverlayActivity.EXTRA_IS_SNIP, false)
                                }
                                context.startActivity(intent)
                            }
                        )

                        1 -> ActivityHistoryScreen(
                            history = history,
                            isDark = isDark,
                            surfaceColor = surfaceColor,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            accentPurple = accentPurple,
                            lightLavender = lightLavender,
                            onBack = { selectedTab = 0 },
                            onClearHistory = {
                                HistoryManager.clearHistory(context)
                                history = emptyList()
                                Toast.makeText(context, R.string.history_cleared, Toast.LENGTH_SHORT).show()
                            },
                            onSelectHistoryItem = { item ->
                                activeExplanationItem = item
                            },
                            onDeleteItem = { item ->
                                HistoryManager.deleteItem(context, item)
                                history = HistoryManager.getHistory(context)
                                Toast.makeText(context, R.string.deleted_from_history, Toast.LENGTH_SHORT).show()
                            }
                        )

                        2 -> MoreScreen(
                            themePref = themePref,
                            isDark = isDark,
                            currentLanguage = currentLanguage,
                            surfaceColor = surfaceColor,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            accentPurple = accentPurple,
                            lightLavender = lightLavender,
                            onThemeChange = onThemeChange,
                            onOpenLanguagePicker = { showLanguagePicker = true },
                            onShowTutorial = onShowTutorial,
                            onCheckUpdates = onCheckUpdates,
                            onClearAllData = {
                                HistoryManager.clearHistory(context)
                                history = emptyList()
                                Toast.makeText(context, R.string.history_cleared, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = currentLanguage,
            onSelectLanguage = { selected ->
                currentLanguage = selected
                SettingsManager.setLanguage(context, selected)
                AnalyticsManager.logLanguageChanged(context, selected, "app_picker_dialog")
                showLanguagePicker = false
                (context as? ComponentActivity)?.recreate()
            },
            onDismiss = { showLanguagePicker = false },
            isDark = isDark,
            accent = accentPurple,
            primaryTextColor = primaryText,
            secondaryTextColor = secondaryText,
            cardBg = surfaceColor,
            cardBorder = borderColor
        )
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 1: Home Screen (Follows Left Screen of Reference)
// -------------------------------------------------------------------------------------------------

@Composable
fun HomeScreen(
    history: List<HistoryItem>,
    currentLanguage: String,
    isDark: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    accentPurple: Color,
    lightLavender: Color,
    onOpenLanguagePicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onSelectHistoryItem: (HistoryItem) -> Unit,
    onDeleteItem: (HistoryItem) -> Unit,
    onClearHistory: () -> Unit,
    onExplainText: (String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val currentLangObj = remember(currentLanguage) {
        LanguageManager.getLanguageByEnglishName(currentLanguage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: ELI5 Icon + Name on left; Language Pill + Settings on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "ELI5 Logo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF261F36) else Color(0xFFEDE9FE))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ELI5",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Language pill
                Surface(
                    modifier = Modifier.clickable { onOpenLanguagePicker() },
                    shape = RoundedCornerShape(20.dp),
                    color = lightLavender.copy(alpha = if (isDark) 0.5f else 0.8f),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currentLangObj.flag, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currentLangObj.nativeName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = primaryText
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = primaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. Hero Section: "Understand anything, instantly." + Document Illustration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.hero_title_1),
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
                Text(
                    text = stringResource(R.string.hero_title_highlight),
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    color = accentPurple
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.hero_subtitle),
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = secondaryText
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.85f)
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_document_illustration),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 3. Circle to ELI5 Card
        CircleToEli5Card(
            isDark = isDark,
            surfaceColor = surfaceColor,
            borderColor = borderColor,
            primaryText = primaryText,
            secondaryText = secondaryText,
            accentPurple = accentPurple,
            lightLavender = lightLavender
        )

        // 4. Direct Explanation: Input + Explain button
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.direct_explain_placeholder),
                                color = secondaryText,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(
                                color = primaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(accentPurple),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cm.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                if (pasted.isNotBlank()) {
                                    inputText = pasted
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = "Paste",
                            tint = secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Prominent Gradient Purple "✦ Explain" button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF6A0DAD), Color(0xFF8B5CF6))
                        )
                    )
                    .clickable {
                        if (inputText.isNotBlank()) {
                            onExplainText(inputText.trim())
                        } else {
                            Toast.makeText(context, "Please enter or paste text to explain", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_explain_action),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. 2x2 Grid of Actions
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Try Gesture
                ActionCard(
                    title = stringResource(R.string.btn_try_gesture),
                    icon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.weight(1f),
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    accentPurple = accentPurple,
                    primaryText = primaryText,
                    isDark = isDark,
                    onClick = {
                        val intent = Intent(context, CircleSnipActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                // Assistant Settings
                ActionCard(
                    title = stringResource(R.string.btn_assistant_settings),
                    icon = Icons.Rounded.Tune,
                    modifier = Modifier.weight(1f),
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    accentPurple = accentPurple,
                    primaryText = primaryText,
                    isDark = isDark,
                    onClick = {
                        Eli5AssistantManager.openAssistantSettings(context)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Language
                ActionCard(
                    title = stringResource(R.string.btn_language),
                    subtitle = currentLangObj.nativeName,
                    icon = Icons.Rounded.Public,
                    hasDropdown = true,
                    modifier = Modifier.weight(1f),
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    accentPurple = accentPurple,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    isDark = isDark,
                    onClick = onOpenLanguagePicker
                )

                // Recent Activity
                ActionCard(
                    title = stringResource(R.string.btn_recent_activity),
                    icon = Icons.Rounded.Schedule,
                    modifier = Modifier.weight(1f),
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    accentPurple = accentPurple,
                    primaryText = primaryText,
                    isDark = isDark,
                    onClick = onOpenHistory
                )
            }
        }

        // 6. Recent Activity Section on Home
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.history_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                }

                Text(
                    text = stringResource(R.string.history_clear_all),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentPurple,
                    modifier = Modifier.clickable { onClearHistory() }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.take(3).forEachIndexed { idx, item ->
                    RecentHistoryItemCard(
                        item = item,
                        index = idx,
                        isDark = isDark,
                        surfaceColor = surfaceColor,
                        borderColor = borderColor,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentPurple = accentPurple,
                        onClick = { onSelectHistoryItem(item) },
                        onDelete = {
                            onDeleteItem(item)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -------------------------------------------------------------------------------------------------
// Action Card (2x2 Grid Helper)
// -------------------------------------------------------------------------------------------------

@Composable
fun ActionCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    hasDropdown: Boolean = false,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    borderColor: Color,
    accentPurple: Color,
    primaryText: Color,
    secondaryText: Color = primaryText,
    isDark: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(86.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF382654) else LavenderLight.copy(alpha = 0.65f))
                    .border(1.dp, if (isDark) Color(0xFF7C3AED).copy(alpha = 0.45f) else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFE9D5FF) else accentPurple,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    if (hasDropdown) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = secondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Circle to ELI5 Card
// -------------------------------------------------------------------------------------------------

@Composable
fun CircleToEli5Card(
    isDark: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    accentPurple: Color,
    lightLavender: Color
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
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp))
            .clickable {
                if (!isAssistant) {
                    Eli5AssistantManager.openAssistantSettings(context)
                }
            },
        shape = RoundedCornerShape(22.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hand Gesture rounded icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF382654) else lightLavender.copy(alpha = 0.85f))
                        .border(1.dp, if (isDark) Color(0xFF7C3AED).copy(alpha = 0.45f) else Color.Transparent, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = "Gesture",
                        tint = if (isDark) Color(0xFFE9D5FF) else accentPurple,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.circle_to_eli5_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isAssistant) SuccessGreen else Color(0xFF9E9AA8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAssistant) stringResource(R.string.circle_card_active) else stringResource(R.string.circle_card_inactive),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAssistant) SuccessGreen else secondaryText
                        )
                    }
                }
            }

            Switch(
                checked = isAssistant,
                onCheckedChange = {
                    Eli5AssistantManager.openAssistantSettings(context)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentPurple,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFD1D5DB)
                )
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 2: Explanation Screen (Follows Middle Screen of Reference)
// -------------------------------------------------------------------------------------------------

@Composable
fun ExplanationScreen(
    item: HistoryItem,
    language: String,
    isDark: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    accentPurple: Color,
    lightLavender: Color,
    ttsHelper: TtsHelper,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fullExplanation by remember { mutableStateOf(item.resultText) }
    var isLoading by remember { mutableStateOf(item.resultText.isBlank()) }
    var liked by remember { mutableStateOf<Boolean?>(null) }
    var deeperText by remember { mutableStateOf<String?>(null) }
    var deeperLoading by remember { mutableStateOf(false) }
    var deeperSectionTitle by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    val isProofread = remember(item.mode) { item.mode.startsWith("Proofread", ignoreCase = true) }

    DisposableEffect(ttsHelper) {
        ttsHelper.setStateListener { speaking ->
            isSpeaking = speaking
        }
        onDispose {
            ttsHelper.stop()
            ttsHelper.setStateListener(null)
        }
    }

    LaunchedEffect(item.originalText) {
        if (fullExplanation.isBlank()) {
            isLoading = true
            val generated = ExplainRepository.fetchExplanation(
                text = item.originalText,
                mode = item.mode,
                language = language,
                onChunk = { chunk ->
                    fullExplanation = chunk
                }
            )
            fullExplanation = generated
            isLoading = false
            // Save completed item to history
            val savedItem = item.copy(resultText = generated)
            HistoryManager.saveItem(context, savedItem)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar: Back arrow, "Explanation" title, Share icon, 3-dots overflow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = primaryText,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(R.string.explanation_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (fullExplanation.isNotBlank()) {
                            ShareImageGenerator.shareExplanation(
                                context,
                                item.originalText,
                                fullExplanation,
                                item.mode,
                                scope
                            )
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = primaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More",
                            tint = primaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy Original Text") },
                            onClick = {
                                showMenu = false
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("ELI5 Original", item.originalText))
                                Toast.makeText(context, "Original text copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Explanation") },
                            onClick = {
                                showMenu = false
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("ELI5 Explanation", fullExplanation))
                                Toast.makeText(context, "Explanation copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // 1. Original Selected Text Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.originalText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = primaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("ELI5 Original", item.originalText))
                        Toast.makeText(context, "Copied original text", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy",
                        tint = secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Main ELI5 Explanation Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Sparkle/Edit and Title + Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isProofread) Icons.Rounded.EditNote else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = accentPurple,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProofread) stringResource(R.string.proofread_card_title) else stringResource(R.string.eli5_explanation_card_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentPurple
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = lightLavender.copy(alpha = if (isDark) 0.35f else 0.7f)
                    ) {
                        Text(
                            text = item.mode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (isLoading && fullExplanation.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = accentPurple,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isProofread) "Polishing text..." else "Simplifying for you...",
                            fontSize = 14.sp,
                            color = secondaryText
                        )
                    }
                } else {
                    val formattedExplanation = remember(fullExplanation, primaryText, accentPurple) {
                        MarkdownTextFormatter.formatMarkdown(fullExplanation, primaryText, accentPurple)
                    }
                    Text(
                        text = formattedExplanation,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = primaryText
                    )
                }

                // Action Row at bottom: Like, Dislike, Copy, Audio/TTS, Regenerate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbs Up
                    IconButton(
                        onClick = { liked = if (liked == true) null else true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbUp,
                            contentDescription = "Like",
                            tint = if (liked == true) SuccessGreen else secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Thumbs Down
                    IconButton(
                        onClick = { liked = if (liked == false) null else false },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbDown,
                            contentDescription = "Dislike",
                            tint = if (liked == false) AccentOrange else secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Copy
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("ELI5 Result", fullExplanation))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy",
                            tint = secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Audio / TTS Toggle (Reads aloud or stops when already speaking)
                    IconButton(
                        onClick = {
                            if (fullExplanation.isNotBlank()) {
                                val langObj = LanguageManager.getLanguageByEnglishName(language)
                                val nowSpeaking = ttsHelper.toggle(fullExplanation, langObj.code) { speaking ->
                                    isSpeaking = speaking
                                }
                                if (nowSpeaking) {
                                    Toast.makeText(context, "Reading aloud...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Stopped reading", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop reading" else "Read aloud",
                            tint = if (isSpeaking) AccentOrange else accentPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Regenerate Button
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                fullExplanation = ""
                                val regenerated = ExplainRepository.fetchExplanation(
                                    text = item.originalText,
                                    mode = item.mode,
                                    language = language,
                                    onChunk = { chunk ->
                                        fullExplanation = chunk
                                    }
                                )
                                fullExplanation = regenerated
                                isLoading = false
                                val updated = item.copy(resultText = regenerated, timestamp = System.currentTimeMillis())
                                HistoryManager.saveItem(context, updated)
                                Toast.makeText(context, if (isProofread) "Proofread regenerated" else "Explanation regenerated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Regenerate",
                            tint = accentPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 3. Deeper Insights or Proofreading Tweaks Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isProofread) Icons.Rounded.Tune else Icons.Rounded.Layers,
                        contentDescription = null,
                        tint = primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProofread) stringResource(R.string.refine_tweak_title) else stringResource(R.string.go_deeper_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isProofread) {
                    // Tweak 1: Professional Tone
                    GoDeeperRow(
                        icon = Icons.Rounded.WorkOutline,
                        title = stringResource(R.string.tweak_professional),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            deeperSectionTitle = "Professional Tone"
                            deeperLoading = true
                            scope.launch {
                                val res = ExplainRepository.fetchExplanation(
                                    text = item.originalText,
                                    mode = "Proofread",
                                    language = language,
                                    extraModifier = "Rewrite this draft to be professional, polished, and executive in tone."
                                )
                                deeperText = res
                                deeperLoading = false
                            }
                        }
                    )

                    HorizontalDivider(
                        color = borderColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Tweak 2: Concise & Punchy
                    GoDeeperRow(
                        icon = Icons.Rounded.Compress,
                        title = stringResource(R.string.tweak_concise),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            deeperSectionTitle = "Concise & Punchy"
                            deeperLoading = true
                            scope.launch {
                                val res = ExplainRepository.fetchExplanation(
                                    text = item.originalText,
                                    mode = "Proofread",
                                    language = language,
                                    extraModifier = "Make this draft concise, punchy, and eliminate any filler words while preserving key facts."
                                )
                                deeperText = res
                                deeperLoading = false
                            }
                        }
                    )

                    HorizontalDivider(
                        color = borderColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Tweak 3: Friendly & Casual
                    GoDeeperRow(
                        icon = Icons.Rounded.SentimentSatisfied,
                        title = stringResource(R.string.tweak_casual),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            deeperSectionTitle = "Friendly & Casual"
                            deeperLoading = true
                            scope.launch {
                                val res = ExplainRepository.fetchExplanation(
                                    text = item.originalText,
                                    mode = "Proofread",
                                    language = language,
                                    extraModifier = "Rewrite this with a warm, casual, approachable, and friendly tone."
                                )
                                deeperText = res
                                deeperLoading = false
                            }
                        }
                    )

                    HorizontalDivider(
                        color = borderColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Tweak 4: Fix grammar only
                    GoDeeperRow(
                        icon = Icons.Rounded.Spellcheck,
                        title = stringResource(R.string.tweak_grammar),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            deeperSectionTitle = "Grammar & Spelling Fix"
                            deeperLoading = true
                            scope.launch {
                                val res = ExplainRepository.fetchExplanation(
                                    text = item.originalText,
                                    mode = "Proofread",
                                    language = language,
                                    extraModifier = "Fix only typos, punctuation, and grammar without altering vocabulary or tone."
                                )
                                deeperText = res
                                deeperLoading = false
                            }
                        }
                    )
                } else {
                    // Row 1: A simpler analogy
                    GoDeeperRow(
                        icon = Icons.Rounded.Science,
                        title = stringResource(R.string.go_deeper_analogy),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            deeperSectionTitle = "A Simpler Analogy"
                            deeperLoading = true
                            scope.launch {
                                val res = ExplainRepository.fetchExplanation(
                                    text = item.originalText,
                                    mode = "ELI5",
                                    language = language,
                                    extraModifier = "Explain this using a super clear, everyday analogy that a child would immediately understand."
                                )
                                deeperText = res
                                deeperLoading = false
                            }
                        }
                    )

                    HorizontalDivider(
                        color = borderColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Row 2: Real world applications
                    GoDeeperRow(
                        icon = Icons.Rounded.Layers,
                        title = stringResource(R.string.go_deeper_applications),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            deeperSectionTitle = "Real World Applications"
                            deeperLoading = true
                            scope.launch {
                                val res = ExplainRepository.fetchExplanation(
                                    text = item.originalText,
                                    mode = "Detailed",
                                    language = language,
                                    extraModifier = "Provide 3 concrete real-world applications or practical industry examples where this concept is actively used today."
                                )
                                deeperText = res
                                deeperLoading = false
                            }
                        }
                    )

                    HorizontalDivider(
                        color = borderColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Row 3: Key terms
                    GoDeeperRow(
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        title = stringResource(R.string.go_deeper_terms),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            deeperSectionTitle = "Key Terms Breakdown"
                            deeperLoading = true
                            scope.launch {
                                val res = ExplainRepository.fetchExplanation(
                                    text = item.originalText,
                                    mode = "Direct",
                                    language = language,
                                    extraModifier = "Break down and define the 3-4 most important technical keywords or jargon mentioned here in 1 sentence each."
                                )
                                deeperText = res
                                deeperLoading = false
                            }
                        }
                    )
                }

                // Deeper expansion view if clicked
                if (deeperLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = accentPurple,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Loading ${deeperSectionTitle ?: "insights"}...",
                            fontSize = 13.sp,
                            color = secondaryText
                        )
                    }
                } else if (!deeperText.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(lightLavender.copy(alpha = 0.4f))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = deeperSectionTitle ?: "Deep Dive",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentPurple
                            )
                            if (isProofread) {
                                TextButton(
                                    onClick = {
                                        fullExplanation = deeperText!!
                                        val updated = item.copy(resultText = deeperText!!, timestamp = System.currentTimeMillis())
                                        HistoryManager.saveItem(context, updated)
                                        Toast.makeText(context, "Applied to text", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Apply", color = accentPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedDeeper = remember(deeperText, primaryText, accentPurple) {
                            deeperText?.let { MarkdownTextFormatter.formatMarkdown(it, primaryText, accentPurple) }
                        }
                        if (formattedDeeper != null) {
                            Text(
                                text = formattedDeeper,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp,
                                color = primaryText
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun GoDeeperRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 3: Your Activity / History Screen (Follows Right Screen of Reference)
// -------------------------------------------------------------------------------------------------

@Composable
fun ActivityHistoryScreen(
    history: List<HistoryItem>,
    isDark: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    accentPurple: Color,
    lightLavender: Color,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onSelectHistoryItem: (HistoryItem) -> Unit,
    onDeleteItem: (HistoryItem) -> Unit
) {
    val streak = remember(history) { HistoryManager.calculateDayStreak(history) }
    val points = remember(history) { history.size * 25 + (if (history.isNotEmpty()) 50 else 0) }
    val topics = remember(history) { HistoryManager.calculateTopicsCount(history) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar: Back arrow, "Your Activity", bar chart icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = primaryText,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Your Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText
            )

            IconButton(
                onClick = { /* stats details */ },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = "Stats",
                    tint = primaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 1. Activity Summary Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circular Ring Gauge on Left
                CircularStatsIndicator(
                    count = history.size,
                    accentPurple = accentPurple,
                    lightLavender = lightLavender,
                    primaryText = primaryText,
                    secondaryText = secondaryText
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Stats on Right
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatRow(
                        icon = Icons.Rounded.Whatshot,
                        iconTint = Color(0xFFFF8A32),
                        iconBgColor = Color(0xFFFF8A32).copy(alpha = 0.15f),
                        countText = streak.toString(),
                        labelText = stringResource(R.string.activity_summary_streak),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        isDark = isDark,
                        borderColor = borderColor
                    )
                    StatRow(
                        icon = Icons.Rounded.Star,
                        iconTint = Color(0xFFEAB308),
                        iconBgColor = Color(0xFFEAB308).copy(alpha = 0.15f),
                        countText = points.toString(),
                        labelText = stringResource(R.string.activity_summary_points),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        isDark = isDark,
                        borderColor = borderColor
                    )
                    StatRow(
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        iconTint = Color(0xFF8B5CF6),
                        iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                        countText = topics.toString(),
                        labelText = stringResource(R.string.activity_summary_topics),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        isDark = isDark,
                        borderColor = borderColor
                    )
                }
            }
        }

        // 2. Recent Explanations Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = primaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.recent_explanations),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
            }

            if (history.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.history_clear_all),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentPurple,
                    modifier = Modifier.clickable { onClearHistory() }
                )
            }
        }

        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = secondaryText.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.history_empty_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText
                )
                Text(
                    text = stringResource(R.string.empty_state_message),
                    fontSize = 12.5.sp,
                    color = secondaryText
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                history.forEachIndexed { idx, item ->
                    RecentHistoryItemCard(
                        item = item,
                        index = idx,
                        isDark = isDark,
                        surfaceColor = surfaceColor,
                        borderColor = borderColor,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        accentPurple = accentPurple,
                        onClick = { onSelectHistoryItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -------------------------------------------------------------------------------------------------
// Circular Progress Indicator for Stats
// -------------------------------------------------------------------------------------------------

@Composable
fun CircularStatsIndicator(
    count: Int,
    accentPurple: Color,
    lightLavender: Color,
    primaryText: Color,
    secondaryText: Color
) {
    val progress = remember(count) {
        if (count == 0) 0.1f else (count % 50) / 50f
    }

    Box(
        modifier = Modifier.size(126.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating decorative sparkle stars around the ring (matching reference image)
        Text(
            text = "✦",
            fontSize = 14.sp,
            color = PurpleSecondary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 2.dp, top = 2.dp)
        )
        Text(
            text = "✦",
            fontSize = 11.sp,
            color = Color(0xFFC084FC),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp)
        )
        Text(
            text = "✦",
            fontSize = 13.sp,
            color = PurpleSecondary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp, bottom = 4.dp)
        )

        Canvas(modifier = Modifier.size(116.dp)) {
            val strokeWidth = 12.dp.toPx()

            // Background track
            drawArc(
                color = lightLavender,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            // Foreground purple arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(PurpleSecondary, PurplePrimary, PurpleSecondary)
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText
            )
            Text(
                text = "Explanations",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryText
            )
        }
    }
}

@Composable
fun StatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    countText: String,
    labelText: String,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean,
    borderColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF221733) else Color(0xFFFAF7FD),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF35234E) else Color(0xFFF0E8FA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = countText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = labelText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryText
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Recent History Item Card (Used on Home and History Screens)
// -------------------------------------------------------------------------------------------------

@Composable
fun RecentHistoryItemCard(
    item: HistoryItem,
    index: Int,
    isDark: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    accentPurple: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }

    // Color palette rotation for badges
    val badgeColors = listOf(
        Pair(Color(0xFFEDE9FE), PurplePrimary),     // Lavender
        Pair(Color(0xFFFFEDD5), Color(0xFFEA580C)), // Orange
        Pair(Color(0xFFDCFCE7), Color(0xFF16A34A)), // Green
        Pair(Color(0xFFE0E7FF), Color(0xFF4338CA))  // Blue
    )
    val (badgeBg, badgeIcon) = badgeColors[index % badgeColors.size]

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored document badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) badgeIcon.copy(alpha = 0.25f) else badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = badgeIcon,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.originalText.replace('\n', ' ').trim(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = HistoryManager.formatRelativeTime(item.timestamp),
                    fontSize = 12.sp,
                    color = secondaryText
                )
            }

            // 3-dots overflow
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = secondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy Original") },
                        onClick = {
                            showMenu = false
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("ELI5 Original", item.originalText))
                            Toast.makeText(context, "Copied original text", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Explanation") },
                        onClick = {
                            showMenu = false
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("ELI5 Explanation", item.resultText))
                            Toast.makeText(context, "Copied explanation", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.overlay_share)) },
                        onClick = {
                            showMenu = false
                            ShareImageGenerator.shareExplanation(
                                context,
                                item.originalText,
                                item.resultText,
                                item.mode,
                                scope
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_delete_item), color = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SCREEN 4: More / Settings Screen
// -------------------------------------------------------------------------------------------------

@Composable
fun MoreScreen(
    themePref: String,
    isDark: Boolean,
    currentLanguage: String,
    surfaceColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    accentPurple: Color,
    lightLavender: Color,
    onThemeChange: (String) -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onShowTutorial: () -> Unit,
    onCheckUpdates: () -> Unit,
    onClearAllData: () -> Unit
) {
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.bottom_nav_more),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = primaryText,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Preferences Group
        Text(
            text = stringResource(R.string.more_preferences),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            letterSpacing = 1.sp
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                // Language
                val langObj = LanguageManager.getLanguageByEnglishName(currentLanguage)
                MoreMenuRow(
                    icon = Icons.Rounded.Public,
                    title = stringResource(R.string.btn_language),
                    value = "${langObj.flag} ${langObj.nativeName}",
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    accentPurple = accentPurple,
                    onClick = onOpenLanguagePicker
                )

                HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 8.dp))

                // Theme
                val themeLabel = when (themePref) {
                    "dark" -> stringResource(R.string.settings_theme_dark)
                    "light" -> stringResource(R.string.settings_theme_light)
                    else -> stringResource(R.string.settings_theme_system)
                }
                MoreMenuRow(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(R.string.more_theme),
                    value = themeLabel,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    accentPurple = accentPurple,
                    onClick = { showThemeDialog = true }
                )
            }
        }

        // Assistant & Gesture Group
        Text(
            text = stringResource(R.string.more_assistant_gestures),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            letterSpacing = 1.sp
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                MoreMenuRow(
                    icon = Icons.Rounded.TouchApp,
                    title = stringResource(R.string.more_gesture_demo),
                    value = "Interactive",
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    accentPurple = accentPurple,
                    onClick = {
                        val intent = Intent(context, CircleSnipActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 8.dp))

                MoreMenuRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.more_assistant_settings),
                    value = "System",
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    accentPurple = accentPurple,
                    onClick = {
                        Eli5AssistantManager.openAssistantSettings(context)
                    }
                )

                HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 8.dp))

                MoreMenuRow(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    title = stringResource(R.string.more_tutorial),
                    value = "Guide",
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    accentPurple = accentPurple,
                    onClick = onShowTutorial
                )
            }
        }

        // About & Updates
        Text(
            text = stringResource(R.string.more_about_data),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            letterSpacing = 1.sp
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                MoreMenuRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.more_check_updates),
                    value = "v2.2.1",
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    accentPurple = accentPurple,
                    onClick = onCheckUpdates
                )

                HorizontalDivider(color = borderColor, modifier = Modifier.padding(horizontal = 8.dp))

                MoreMenuRow(
                    icon = Icons.Rounded.Delete,
                    title = stringResource(R.string.more_clear_all_history),
                    value = "",
                    primaryText = Color(0xFFEF4444),
                    secondaryText = secondaryText,
                    accentPurple = Color(0xFFEF4444),
                    onClick = onClearAllData
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showThemeDialog) {
        val sysLabel = stringResource(R.string.settings_theme_system)
        val lightLabel = stringResource(R.string.settings_theme_light)
        val darkLabel = stringResource(R.string.settings_theme_dark)
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme), fontWeight = FontWeight.Bold, color = primaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "system" to sysLabel,
                        "light" to lightLabel,
                        "dark" to darkLabel
                    ).forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(key)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, fontSize = 15.sp, color = primaryText)
                            if (themePref == key) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accentPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = accentPurple)
                }
            },
            containerColor = surfaceColor
        )
    }
}

@Composable
fun MoreMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    primaryText: Color,
    secondaryText: Color,
    accentPurple: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentPurple,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Bottom Navigation Bar
// -------------------------------------------------------------------------------------------------

@Composable
fun Eli5BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isDark: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    accentPurple: Color,
    lightLavender: Color
) {
    val items = listOf(
        Triple(0, stringResource(R.string.bottom_nav_home), Icons.Rounded.Home),
        Triple(1, stringResource(R.string.bottom_nav_history), Icons.Rounded.Schedule),
        Triple(2, stringResource(R.string.bottom_nav_more), Icons.Rounded.GridView)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (index, label, icon) ->
                val isSelected = selectedTab == index
                val itemInteraction = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(68.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) lightLavender else Color.Transparent)
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) accentPurple else (if (isDark) TextSecondaryDark else TextSecondary),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accentPurple else (if (isDark) TextSecondaryDark else TextSecondary),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Language Picker Dialog
// -------------------------------------------------------------------------------------------------

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
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery) {
        LanguageManager.searchLanguages(searchQuery)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(440.dp),
            shape = RoundedCornerShape(26.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
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
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Text(
                            text = stringResource(R.string.picker_subtitle),
                            fontSize = 11.5.sp,
                            color = secondaryTextColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(stringResource(R.string.picker_search_hint), fontSize = 12.5.sp, color = secondaryTextColor)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = secondaryTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
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

                Spacer(modifier = Modifier.height(12.dp))

                // Balanced 2-Column Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(filteredLanguages) { lang ->
                        val isSelected = currentLanguage.equals(lang.englishName, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectLanguage(lang.englishName) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) (if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE)) else (if (isDark) Color(0xFF231B32) else Color(0xFFFAF7FF)),
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) accent else cardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 9.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = lang.flag, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lang.nativeName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                            color = if (isSelected) accent else primaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = lang.englishName,
                                            fontSize = 10.5.sp,
                                            color = secondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

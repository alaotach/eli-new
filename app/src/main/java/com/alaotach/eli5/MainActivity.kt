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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = primaryTextColor
                )
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
                                text = if (hasOverlayPermission) "ELI5 is on" else "Permission needed",
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (hasOverlayPermission) "Ready in any app" else "Tap here to allow overlay card",
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
                                contentDescription = "Dismiss",
                                tint = if (hasOverlayPermission) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else Color(0xFFEF6C00),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Try-It Text Box
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
                            text = "Try ELI5 directly",
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
                                contentDescription = "Dismiss",
                                tint = secondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Paste or type any complex text to test the explanation card:",
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = tryItText,
                        onValueChange = { tryItText = it },
                        placeholder = { Text("e.g. Quantum entanglement occurs when particles...", fontSize = 13.sp, color = secondaryTextColor) },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            tryItText = "A blockchain is a decentralized distributed ledger that records transactions across a network of computers."
                        }) {
                            Text("Insert sample", color = accent, fontSize = 12.sp)
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
                            Text("Explain ✨", color = Color.White, fontWeight = FontWeight.Bold)
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
                text = "Recent Activity",
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                style = MaterialTheme.typography.titleMedium
            )

            if (history.isNotEmpty()) {
                TextButton(onClick = {
                    HistoryManager.clearHistory(context)
                    history = emptyList()
                }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
                    text = "No recent explanations",
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Highlight text in any app and select ELI5 to start.",
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
                    Text("Interactive Guide", color = Color.White)
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
                        Text("Load More", color = accent)
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
                    text = "Visual Guide",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = primaryTextColor
                )
                TextButton(onClick = onShowTutorial) {
                    Text("Interactive Tour →", color = accent, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val guideSteps = listOf(
                Triple(R.drawable.tutorial_img_6, "Highlight Text", "Select text in any app (Chrome, WhatsApp) & tap ⋮"),
                Triple(R.drawable.tutorial_img_7, "Choose ELI5", "Select 'Explain with ELI5' from the popup"),
                Triple(R.drawable.tutorial_img_4, "Instant Card", "Read concise explanation without leaving your app"),
                Triple(R.drawable.tutorial_img_8, "Proofread", "Auto-correct mistakes and adjust tone in 1 tap")
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
                        text = "Settings",
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
                            contentDescription = "Close",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Default Mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Default Explanation Mode",
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

                // Explanation Language
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Explanation Language",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = primaryTextColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("English", "Hindi").forEach { lang ->
                            val isSelected = language == lang
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        language = lang
                                        SettingsManager.setLanguage(context, lang)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accent else if (isDark) Color(0xFF261F36) else PurpleLightBg
                            ) {
                                Text(
                                    text = lang,
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

                // Proofread Tone
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Proofread Tone Default",
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
                        "Theme",
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
                            "Save History",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = primaryTextColor
                        )
                        Text(
                            "Keep recent explanations for quick access",
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
                                    Toast.makeText(context, "Home cards restored", Toast.LENGTH_SHORT).show()
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
                                    "Restore hidden home cards",
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
                                "Check for Updates",
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
                                "Privacy Policy",
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
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

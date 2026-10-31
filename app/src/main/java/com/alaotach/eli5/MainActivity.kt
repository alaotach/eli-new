package com.alaotach.eli5

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import com.alaotach.eli5.ui.theme.Eli5Theme
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager
    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            if (!prefs.getBoolean("onboarding_done", false) || !Settings.canDrawOverlays(this@MainActivity)) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
                return@setContent
            }

            var isViewingTutorial by remember { mutableStateOf(false) }

            Eli5Theme(darkTheme = isDark, dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (isDark) Color(0xFF121212) else Color(0xFFF9FAFB)
                ) { innerPadding ->
                    if (isViewingTutorial) {
                        HowToUseScreen(
                            isDark = isDark,
                            onBack = { isViewingTutorial = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        MainScreen(
                            themePref = themePreference,
                            isDark = isDark,
                            onThemeChange = {
                                SettingsManager.setTheme(this@MainActivity, it)
                                themePreference = it
                            },
                            onCheckUpdates = {
                                triggerManualUpdateCheck()
                            },
                            onShowTutorial = {
                                startActivity(Intent(this@MainActivity, HowToUseActivity::class.java))
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
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
                } catch (_: Exception) {
                }
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
                    android.widget.Toast.makeText(this, "Could not start update: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(this, "ELI5 is up to date!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            android.widget.Toast.makeText(this, "Check failed: Play Store not available", android.widget.Toast.LENGTH_SHORT).show()
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
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2B2B2A) else Color(0xFFE5E7EB)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF111827)
    val secondaryTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF6B7280)
    val mutedTextColor = if (isDark) Color(0xFF888888) else Color(0xFF6B7280)

    val androidTextColor = if (isDark) {
        android.graphics.Color.parseColor("#E0E0E0")
    } else {
        android.graphics.Color.parseColor("#212121")
    }

    LaunchedEffect(Unit) {
        history = HistoryManager.getHistory(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "ELI5 Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFE8F5E9))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ELI5",
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = primaryTextColor
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System Theme") },
                        onClick = {
                            onThemeChange("system")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Light Mode") },
                        onClick = {
                            onThemeChange("light")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Dark Mode") },
                        onClick = {
                            onThemeChange("dark")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Check for Updates") },
                        onClick = {
                            onCheckUpdates()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Clear All History",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            HistoryManager.clearHistory(context)
                            history = emptyList()
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val todayStr = dateFormat.format(Date())
            val todayItems = history.filter { dateFormat.format(Date(it.timestamp)) == todayStr }
            val wordsToday = todayItems.sumOf { item ->
                Regex("\\s+").split(item.originalText).count { it.isNotEmpty() }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = wordsToday.toString(),
                        color = primaryTextColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Words Explained Today",
                        color = mutedTextColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clickable(onClick = onShowTutorial),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF00C853)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How to Use",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Interactive Guide",
                        color = Color(0xFFE0FFE0),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Recent Activity",
            modifier = Modifier.padding(start = 16.dp),
            fontWeight = FontWeight.Bold,
            color = primaryTextColor,
            style = MaterialTheme.typography.titleMedium
        )

        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No history yet!",
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Highlight some text anywhere and select Eli5 to get started.",
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onShowTutorial,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("How to Use ELI5", color = Color.White)
                }
            }
        } else {
            history.take(itemsToShow).forEach { item ->
                HistoryCard(
                    item = item,
                    isDark = isDark,
                    onClick = { selectedItem = item },
                    onDelete = {
                        HistoryManager.deleteItem(context, item)
                        history = HistoryManager.getHistory(context)
                    }
                )
            }

            if (history.size > itemsToShow) {
                TextButton(
                    onClick = { itemsToShow += 10 },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                ) {
                    Text("Load More")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Detail Dialog
    selectedItem?.let { item ->
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            confirmButton = {
                TextButton(onClick = { selectedItem = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = screenHeight * 0.75f),
            title = {
                Text(
                    text = item.mode,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF262626) else Color(0xFFF3F4F6)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Original Text",
                                color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.originalText,
                                color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E2820) else Color(0xFFE8F5E9).copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Result",
                                color = if (isDark) Color(0xFF4CAF50) else Color(0xFF1B5E20),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            AndroidView(
                                factory = { ctx ->
                                    TextView(ctx).apply {
                                        textSize = 15f
                                        setLineSpacing(0f, 1.2f)
                                    }
                                },
                                update = { tv ->
                                    tv.setTextColor(androidTextColor)
                                    Markwon.create(tv.context).setMarkdown(tv, item.resultText)
                                }
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

@Composable
fun HistoryCard(
    item: HistoryItem,
    isDark: Boolean = isSystemInDarkTheme(),
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    val badgeColor = when {
        item.mode.contains("ELI5", ignoreCase = true) -> if (isDark) Color(0xFF00C853) else Color(0xFF008933)
        item.mode.contains("Proofread", ignoreCase = true) -> if (isDark) Color(0xFFFF7043) else Color(0xFFC04000)
        else -> if (isDark) Color(0xFF4CA640) else Color(0xFF2E7D32)
    }

    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2B2B2A) else Color(0xFFE5E7EB)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF111827)
    val secondaryTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f, fill = false),
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = if (isDark) 0.2f else 0.12f),
                    border = BorderStroke(0.5.dp, badgeColor.copy(alpha = if (isDark) 0.5f else 0.4f))
                ) {
                    Text(
                        text = item.mode,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = badgeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFF5252)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.originalText,
                color = primaryTextColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun HowToUseScreen(
    isDark: Boolean = isSystemInDarkTheme(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryTextColor = if (isDark) Color.White else Color(0xFF111827)
    val secondaryTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF6B7280)
    val tagBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val tagBorder = if (isDark) Color(0xFF2B2B2A) else Color(0xFFE5E7EB)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = primaryTextColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "How to Use ELI5",
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                style = MaterialTheme.typography.titleLarge
            )
        }

        ScrollableTabRow(
            selectedTabIndex = 0,
            edgePadding = 16.dp,
            indicator = {},
            divider = {},
            containerColor = Color.Transparent
        ) {
            listOf("Setup", "Explain", "Proofread", "Tips").forEach { tag ->
                Surface(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { },
                    shape = RoundedCornerShape(20.dp),
                    color = tagBg,
                    border = BorderStroke(1.dp, tagBorder)
                ) {
                    Text(
                        text = tag,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = secondaryTextColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            StepCard("1", "Highlight Any Text", "Find text you want explained in any app (Chrome, WhatsApp, Twitter, etc.) and long-press to highlight it.", isDark)
            StepCard("2", "Open Context Menu", "Wait for the native Android pop-up menu. If you don't see 'Explain with ELI5', tap the three dots.", isDark)
            StepCard("3", "Tap 'Explain with ELI5'", "Select ELI5 from the menu. The app will instantly process your request.", isDark)
            StepCard("4", "Get the Magic", "A tiny elegant card will overlay directly on your screen without ever forcing you to switch apps!", isDark)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Visual Guide",
                modifier = Modifier.padding(bottom = 16.dp),
                color = Color(0xFF00C853),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            val guides = listOf(
                R.drawable.tutorial_img_6 to "Highlight Text",
                R.drawable.tutorial_img_7 to "Tap Explain",
                R.drawable.tutorial_img_8 to "Review Proofread",
                R.drawable.tutorial_img_9 to "Adjust Response",
                R.drawable.tutorial_img_4 to "Read Result",
                R.drawable.tutorial_img_5 to "Learn Meaning"
            )

            guides.forEach { (imgRes, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(imgRes),
                        contentDescription = label,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 250.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2B2B2A) else Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = label,
                                color = primaryTextColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This step shows how to interact seamlessly outside the main app.",
                                color = secondaryTextColor,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StepCard(stepNum: String, title: String, desc: String, isDark: Boolean = isSystemInDarkTheme()) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = Color(0xFF00C853)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNum,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = if (isDark) Color.White else Color(0xFF111827),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = if (isDark) Color(0xFFAAAAAA) else Color(0xFF6B7280),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

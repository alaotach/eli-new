package com.alaotach.eli5

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LocalContext
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alaotach.eli5.ui.screens.DashboardScreen
import com.alaotach.eli5.ui.screens.OnboardingScreen
import com.alaotach.eli5.ui.theme.ELI5Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            val context = LocalContext.current
            val sharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            val currentTheme = remember { mutableStateOf(sharedPreferences.getInt("theme", 0)) }

            val todayCount by viewModel.todayCount.collectAsState()
            val history by viewModel.history.collectAsState()

            ELI5Theme(
                darkTheme = when (currentTheme.value) {
                    1 -> false
                    2 -> true
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            ) {
                val hasOverlayPermission = remember { mutableStateOf(Settings.canDrawOverlays(this)) }

                // Refresh permission state when returning to app
                LaunchedEffect(Unit) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            hasOverlayPermission.value = Settings.canDrawOverlays(this@MainActivity)
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDestroy { lifecycle.removeObserver(observer) }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (hasOverlayPermission.value) "dashboard" else "onboarding"
                ) {
                    composable("onboarding") {
                        OnboardingScreen(
                            onFinish = {
                                if (Settings.canDrawOverlays(this@MainActivity)) {
                                    navController.navigate("dashboard") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            },
                            onGrantPermission = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                            }
                        )
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            todayCount = todayCount,
                            history = history,
                            onDeleteHistory = { item -> viewModel.deleteHistory(item) },
                            onClearAll = { viewModel.clearAllHistory() },
                            onNavigateToGuide = { navController.navigate("onboarding") },
                            currentTheme = currentTheme.value,
                            onThemeChange = { theme ->
                                currentTheme.value = theme
                                val sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                                sharedPreferences.edit().putInt("theme", theme).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
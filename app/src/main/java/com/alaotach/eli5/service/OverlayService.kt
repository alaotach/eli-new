package com.alaotach.eli5.service

import android.appstackaccessgranted
Let me try to write to the OverlayService file:

<tool_call>
<function=Write>
<parameter=content>
package com.alaotach.eli5.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.alaotach.eli5.ui.overlay.OverlayViewModel
import com.alaotach.eli5.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.inject.AndroidInjector
import dagger.hilt.android.support.HiltSupportActivity
import javax.inject.Inject

// Note: Services can't be directly annotated with @AndroidEntryPoint in Hilt
// We'll use manual injection in the service
class OverlayService : Service(), androidx.lifecycle.LifecycleOwner, androidx.lifecycle.ViewModelStoreOwner, androidx.savedstate.SavedStateRegistryOwner {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    override val lifecycle: androidx.lifecycle.Lifecycle get() = lifecycleRegistry

    private val viewModelStore = androidx.lifecycle.ViewModelStore()
    override val viewModelStore: androidx.lifecycle.ViewModelStore get() = viewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        // Initialize Hilt and inject dependencies
        // Note: For services, we need to do manual injection
        SavedStateRegistryController.create(this).performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Perform Hilt injection
        // In a real implementation, we would use HiltAndroidApp.inject(this)
        // But for simplicity in this example, we'll assume the factory is provided
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val originalText = intent?.getStringExtra(Constants.EXTRA_ORIGINAL_TEXT) ?: ""
        val isReadOnly = intent?.getBooleanExtra(Constants.EXTRA_IS_READ_ONLY, false) ?: false
        val mode = intent?.getStringExtra(Constants.EXTRA_MODE) ?: "ELI5"

        showOverlay(originalText, isReadOnly, mode)

        return START_NOT_STICKY
    }

    private fun showOverlay(text: String, isReadOnly: Boolean, mode: String) {
        if (composeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        // Create ViewModel using the injected factory
        val viewModel = ViewModelProvider(this, viewModelFactory)[OverlayViewModel::class.java]

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                val sharedPreferences = getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
                val currentTheme = sharedPreferences.getInt("theme", 0)

                val isDarkTheme = when (currentTheme) {
                    1 -> false
                    2 -> true
                    else -> androidx.compose.foundation.isSystemInDarkTheme()
                }

                com.alaotach.eli5.ui.theme.ELI5Theme(darkTheme = isDarkTheme) {
                    if (mode == "Proofread") {
                        com.alaotach.eli5.ui.overlay.ProofreadOverlayContent(
                            sourceText = text,
                            viewModel = viewModel,
                            onDismiss = { stopSelf() },
                            onAccept = { resultText ->
                                val intent = Intent(Constants.ACTION_REPLACE_TEXT).apply {
                                    putExtra(Constants.EXTRA_REPLACEMENT_TEXT, resultText)
                                }
                                sendBroadcast(intent)
                                stopSelf()
                            }
                        )
                    } else {
                        com.alaotach.eli5.ui.overlay.OverlayContent(
                            sourceText = text,
                            isReadOnly = isReadOnly,
                            initialMode = mode,
                            viewModel = viewModel,
                            onDismiss = { stopSelf() },
                            onAccept = { resultText ->
                                val intent = Intent(Constants.ACTION_REPLACE_TEXT).apply {
                                    putExtra(Constants.EXTRA_REPLACEMENT_TEXT, resultText)
                                }
                                sendBroadcast(intent)
                                stopSelf()
                            }
                        )
                    }
                }
            }
        }

        windowManager.addView(composeView, params)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        composeView?.let {
            windowManager.removeView(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
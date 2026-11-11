package com.alaotach.eli5.assistant

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.alaotach.eli5.AnalyticsManager
import com.alaotach.eli5.ExplainOverlayActivity
import com.alaotach.eli5.R
import com.alaotach.eli5.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CircleSnipActivity : ComponentActivity() {

    private lateinit var selectionView: CircleSelectionView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsManager.logCircleGesture(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        selectionView = CircleSelectionView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            onSelectionCompleted = { croppedBitmap, localPath, _ ->
                processCroppedBitmap(croppedBitmap, localPath)
            }

            onDismissRequested = {
                finish()
            }
        }

        root.addView(selectionView)

        // Floating Tutorial Video Button at bottom center
        val dp = resources.displayMetrics.density
        val isDark = SettingsManager.isDarkTheme(this)
        val tutorialBtn = TextView(this).apply {
            text = "Watch Tutorial"
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val playIcon = androidx.core.content.ContextCompat.getDrawable(this@CircleSnipActivity, R.drawable.ic_play_tutorial)?.mutate()?.apply {
                setTint(Color.WHITE)
            }
            setCompoundDrawablesWithIntrinsicBounds(playIcon, null, null, null)
            compoundDrawablePadding = (dp * 8).toInt()
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6A0DAD"))
                cornerRadius = dp * 22f
                setStroke((dp * 1.2f).toInt(), Color.parseColor("#A855F7"))
            }
            setPadding((dp * 18).toInt(), (dp * 10).toInt(), (dp * 20).toInt(), (dp * 10).toInt())
            elevation = dp * 6f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (dp * 28).toInt()
            }
            setOnClickListener {
                showTutorialVideoDialog()
            }
        }
        root.addView(tutorialBtn)

        setContentView(root)

        // Generate a rich demo canvas if no bitmap passed
        val demoBitmap = createDemoScreenBitmap()
        selectionView.setScreenshot(demoBitmap)

        // Show tutorial video overlay automatically on first open only
        val prefs = getSharedPreferences("eli5_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("try_gesture_tutorial_video_shown", false)) {
            prefs.edit().putBoolean("try_gesture_tutorial_video_shown", true).apply()
            root.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    showTutorialVideoDialog()
                }
            }, 350)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::selectionView.isInitialized) {
            selectionView.resetSelection()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::selectionView.isInitialized) {
            selectionView.resetTranslation()
        }
    }

    private fun processCroppedBitmap(croppedBitmap: Bitmap, localPath: android.graphics.Path?) {
        val preferredLang = SettingsManager.getLanguage(this)
        OcrHelper.extractText(croppedBitmap, localPath, preferredLang) { extractedText ->
            if (!extractedText.isNullOrBlank()) {
                val intent = Intent(this, ExplainOverlayActivity::class.java).apply {
                    putExtra(ExplainOverlayActivity.EXTRA_TEXT, extractedText)
                    putExtra(ExplainOverlayActivity.EXTRA_MODE, SettingsManager.getDefaultMode(this@CircleSnipActivity))
                    putExtra(ExplainOverlayActivity.EXTRA_IS_SNIP, true)
                }
                startActivity(intent)
                // Keep CircleSnipActivity active underneath so the explanation overlay appears over the demo screen!
            } else {
                Toast.makeText(
                    this,
                    "No text found. Try circling around the demo text cards.",
                    Toast.LENGTH_SHORT
                ).show()
                selectionView.resetSelection()
            }
        }
    }

    private fun showTutorialVideoDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val dp = resources.displayMetrics.density
        val isDark = SettingsManager.isDarkTheme(this)
        var isFullscreen = false
        var currentStep = 0
        var exoPlayer: ExoPlayer? = null
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var updateProgressRunnable: Runnable? = null

        val primaryTextColor = if (isDark) Color.WHITE else Color.parseColor("#21112F")
        val secondaryTextColor = if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782")
        val cardBgColor = if (isDark) Color.parseColor("#1C172B") else Color.WHITE
        val cardBorderColor = if (isDark) Color.parseColor("#32254C") else Color.parseColor("#E9D5FF")
        val circleBtnBg = if (isDark) Color.parseColor("#2B1F42") else Color.parseColor("#F3E8FF")
        val circleBtnTint = if (isDark) Color.parseColor("#D8B4FE") else Color.parseColor("#6A0DAD")
        val pillBgColor = if (isDark) Color.parseColor("#261C39") else Color.parseColor("#F5EEFF")
        val pillBorderColor = if (isDark) Color.parseColor("#3C2A58") else Color.parseColor("#E9D5FF")

        val steps = listOf(
            "Circle any content on your screen to get an instant explanation.",
            "Translate foreign languages into your preferred language instantly.",
            "Share branded cards, copy explanations, or listen with natural voice."
        )

        val dialogRoot = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#B8000000"))
        }

        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                cornerRadius = dp * 28f
                setStroke((dp * 1.2f).toInt(), cardBorderColor)
            }
            elevation = dp * 16f
            setPadding((dp * 16).toInt(), (dp * 16).toInt(), (dp * 16).toInt(), (dp * 14).toInt())
        }

        // 1. Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (dp * 14).toInt())
        }

        // App Icon
        val appIconView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            background = GradientDrawable().apply {
                setColor(circleBtnBg)
                cornerRadius = dp * 12f
            }
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams((dp * 44).toInt(), (dp * 44).toInt()).apply {
                marginEnd = (dp * 12).toInt()
            }
        }
        header.addView(appIconView)

        // Title & Subtitle
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        val titleTv = TextView(this).apply {
            text = "Circle to ELI5 Tutorial"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(primaryTextColor)
        }
        val subtitleTv = TextView(this).apply {
            text = "See how it works in under a minute"
            textSize = 12.5f
            setTextColor(secondaryTextColor)
        }
        titleCol.addView(titleTv)
        titleCol.addView(subtitleTv)
        header.addView(titleCol)

        // Fullscreen Toggle Button
        val fullscreenBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_fullscreen)
            imageTintList = ColorStateList.valueOf(circleBtnTint)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(circleBtnBg)
            }
            layoutParams = LinearLayout.LayoutParams((dp * 36).toInt(), (dp * 36).toInt()).apply {
                marginEnd = (dp * 8).toInt()
            }
            setPadding((dp * 8).toInt(), (dp * 8).toInt(), (dp * 8).toInt(), (dp * 8).toInt())
        }
        header.addView(fullscreenBtn)

        // Close Button
        val closeBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_close_rounded)
            imageTintList = ColorStateList.valueOf(circleBtnTint)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(circleBtnBg)
            }
            layoutParams = LinearLayout.LayoutParams((dp * 36).toInt(), (dp * 36).toInt())
            setPadding((dp * 8).toInt(), (dp * 8).toInt(), (dp * 8).toInt(), (dp * 8).toInt())
            setOnClickListener {
                dialog.dismiss()
            }
        }
        header.addView(closeBtn)
        cardContainer.addView(header)

        // 2. Video Player Container with Ambient Purple Background & Compact Modern Controls
        val defaultVideoHeight = minOf((resources.displayMetrics.heightPixels * 0.46f).toInt(), (dp * 390).toInt())
        val videoContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                defaultVideoHeight
            )
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#120722"))
                cornerRadius = dp * 18f
            }
            clipToOutline = true
        }

        // Layer A: Ambient Blurred Video Thumbnail Layer
        val ambientImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        videoContainer.addView(ambientImageView)

        // Layer B: Deep Rich Purple Ambient Overlay
        val ambientOverlay = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#E6200A38"),
                    Color.parseColor("#803C156B"),
                    Color.parseColor("#F2140624")
                )
            )
        }
        videoContainer.addView(ambientOverlay)

        // Layer C: Sharp 9:16 Foreground Portrait PlayerView (RESIZE_MODE_FIT, never stretched or cropped)
        val playerView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        videoContainer.addView(playerView)

        // Layer D: Compact Controls Overlay with Auto-Hide & Tap-to-Reveal
        val controlsOverlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Center Circular Play/Pause Glass Button
        val centerPlayPauseBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_pause_rounded)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#731B0936"))
                setStroke((dp * 1.5f).toInt(), Color.parseColor("#4DFFFFFF"))
            }
            setPadding((dp * 11).toInt(), (dp * 11).toInt(), (dp * 11).toInt(), (dp * 11).toInt())
            layoutParams = FrameLayout.LayoutParams((dp * 48).toInt(), (dp * 48).toInt(), Gravity.CENTER)
        }
        controlsOverlay.addView(centerPlayPauseBtn)

        // Bottom Compact Controller Bar with Gradient Scrim
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#990E051C"),
                    Color.parseColor("#E60E051C")
                )
            )
            setPadding((dp * 12).toInt(), (dp * 6).toInt(), (dp * 12).toInt(), (dp * 8).toInt())
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        }

        // Thin Purple Progress Track Seekbar
        val seekBar = android.widget.SeekBar(this).apply {
            max = 1000
            progressDrawable = ContextCompat.getDrawable(this@CircleSnipActivity, R.drawable.video_seekbar_progress)
            thumb = ContextCompat.getDrawable(this@CircleSnipActivity, R.drawable.video_seekbar_thumb)
            setPadding((dp * 4).toInt(), 0, (dp * 4).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (dp * 16).toInt()
            )
        }
        bottomBar.addView(seekBar)

        // Clean Control Row: Play/Pause, Timestamp, Speed, CC, Fullscreen
        val controlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (dp * 2).toInt()
            }
        }

        val playPauseMiniBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_pause_rounded)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams((dp * 22).toInt(), (dp * 22).toInt()).apply {
                marginEnd = (dp * 8).toInt()
            }
        }
        controlRow.addView(playPauseMiniBtn)

        val timeTv = TextView(this).apply {
            text = "00:00 / 00:36"
            textSize = 10.5f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        controlRow.addView(timeTv)

        var currentSpeedIndex = 0
        val speeds = floatArrayOf(1.0f, 1.25f, 1.5f, 2.0f)
        val speedTv = TextView(this).apply {
            text = "1x"
            textSize = 10.5f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding((dp * 6).toInt(), (dp * 2).toInt(), (dp * 6).toInt(), (dp * 2).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF"))
                cornerRadius = dp * 6f
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (dp * 8).toInt()
            }
        }
        controlRow.addView(speedTv)

        var ccEnabled = true
        val ccBtn = TextView(this).apply {
            text = "CC"
            textSize = 10f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding((dp * 6).toInt(), (dp * 2).toInt(), (dp * 6).toInt(), (dp * 2).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6A0DAD"))
                cornerRadius = dp * 6f
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (dp * 8).toInt()
            }
        }
        controlRow.addView(ccBtn)

        val fullscreenInPlayerBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_fullscreen)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams((dp * 22).toInt(), (dp * 22).toInt())
        }
        controlRow.addView(fullscreenInPlayerBtn)

        bottomBar.addView(controlRow)
        controlsOverlay.addView(bottomBar)
        videoContainer.addView(controlsOverlay)
        cardContainer.addView(videoContainer)

        // 3. Info Pill Card beneath Video
        val pillCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(pillBgColor)
                cornerRadius = dp * 16f
                setStroke((dp * 1f).toInt(), pillBorderColor)
            }
            setPadding((dp * 12).toInt(), (dp * 10).toInt(), (dp * 12).toInt(), (dp * 10).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (dp * 14).toInt()
            }
        }

        // Gesture Icon container
        val iconBox = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(circleBtnBg)
                cornerRadius = dp * 10f
            }
            layoutParams = LinearLayout.LayoutParams((dp * 34).toInt(), (dp * 34).toInt()).apply {
                marginEnd = (dp * 10).toInt()
            }
        }
        val handIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_gesture_hand)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#6A0DAD"))
            layoutParams = FrameLayout.LayoutParams((dp * 20).toInt(), (dp * 20).toInt(), Gravity.CENTER)
        }
        iconBox.addView(handIcon)
        pillCard.addView(iconBox)

        // Divider
        val divider = View(this).apply {
            background = GradientDrawable().apply {
                setColor(pillBorderColor)
            }
            layoutParams = LinearLayout.LayoutParams((dp * 1.5f).toInt(), (dp * 22).toInt()).apply {
                marginEnd = (dp * 10).toInt()
            }
        }
        pillCard.addView(divider)

        // Caption Text
        val captionTv = TextView(this).apply {
            text = steps[0]
            textSize = 12.5f
            setTextColor(primaryTextColor)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        pillCard.addView(captionTv)
        cardContainer.addView(pillCard)

        // 4. Footer Row: Dot Indicators + Next Button
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((dp * 4).toInt(), (dp * 14).toInt(), (dp * 4).toInt(), (dp * 2).toInt())
        }

        // Dot indicators
        val dotsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        val dots = Array(3) { View(this) }
        for (i in 0 until 3) {
            val dot = dots[i].apply {
                background = GradientDrawable().apply {
                    setColor(if (i == 0) Color.parseColor("#6A0DAD") else (if (isDark) Color.parseColor("#43315B") else Color.parseColor("#D8B4FE")))
                    cornerRadius = dp * 4f
                }
                layoutParams = LinearLayout.LayoutParams(
                    (dp * (if (i == 0) 18 else 7)).toInt(),
                    (dp * 7).toInt()
                ).apply {
                    marginEnd = (dp * 6).toInt()
                }
            }
            dotsContainer.addView(dot)
        }
        footer.addView(dotsContainer)

        // Next Button
        val nextBtn = TextView(this).apply {
            text = "Next ›"
            textSize = 13.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6A0DAD"))
                cornerRadius = dp * 20f
            }
            setPadding((dp * 22).toInt(), (dp * 9).toInt(), (dp * 22).toInt(), (dp * 9).toInt())
        }
        footer.addView(nextBtn)
        cardContainer.addView(footer)

        fun updateStep(stepIndex: Int) {
            currentStep = stepIndex
            captionTv.text = steps[currentStep]
            for (i in 0 until 3) {
                val isActive = i == currentStep
                dots[i].background = GradientDrawable().apply {
                    setColor(if (isActive) Color.parseColor("#6A0DAD") else (if (isDark) Color.parseColor("#43315B") else Color.parseColor("#D8B4FE")))
                    cornerRadius = dp * 4f
                }
                dots[i].layoutParams = LinearLayout.LayoutParams(
                    (dp * (if (isActive) 18 else 7)).toInt(),
                    (dp * 7).toInt()
                ).apply {
                    marginEnd = (dp * 6).toInt()
                }
            }
            nextBtn.text = if (currentStep == 2) "Got it ✓" else "Next ›"
        }

        nextBtn.setOnClickListener {
            if (currentStep < 2) {
                updateStep(currentStep + 1)
            } else {
                dialog.dismiss()
            }
        }

        val cardLayoutParams = FrameLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        dialogRoot.addView(cardContainer, cardLayoutParams)

        // Toggle Fullscreen action (both in header and inside player controls)
        val toggleFullscreenAction = {
            isFullscreen = !isFullscreen
            val fullscreenIcon = if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
            fullscreenBtn.setImageResource(fullscreenIcon)
            fullscreenInPlayerBtn.setImageResource(fullscreenIcon)
            if (isFullscreen) {
                header.visibility = View.GONE
                pillCard.visibility = View.GONE
                footer.visibility = View.GONE
                cardContainer.setPadding(0, 0, 0, 0)
                (cardContainer.background as? GradientDrawable)?.cornerRadius = 0f
                cardLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                cardLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                videoContainer.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                (videoContainer.background as? GradientDrawable)?.cornerRadius = 0f
            } else {
                header.visibility = View.VISIBLE
                pillCard.visibility = View.VISIBLE
                footer.visibility = View.VISIBLE
                cardContainer.setPadding((dp * 16).toInt(), (dp * 16).toInt(), (dp * 16).toInt(), (dp * 14).toInt())
                (cardContainer.background as? GradientDrawable)?.cornerRadius = dp * 28f
                cardLayoutParams.width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
                cardLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                videoContainer.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    defaultVideoHeight
                )
                (videoContainer.background as? GradientDrawable)?.cornerRadius = dp * 18f
            }
            cardContainer.layoutParams = cardLayoutParams
        }

        fullscreenBtn.setOnClickListener { toggleFullscreenAction() }
        fullscreenInPlayerBtn.setOnClickListener { toggleFullscreenAction() }

        // Tap background outside to dismiss (when not fullscreen)
        dialogRoot.setOnClickListener {
            if (!isFullscreen) {
                dialog.dismiss()
            }
        }
        cardContainer.setOnClickListener { /* prevent dismiss when clicking inside */ }

        // Video Setup with ExoPlayer
        val rawUri = Uri.parse("android.resource://$packageName/${R.raw.eli5_tutorial_voiceover}")
        val fileFallback = File("/home/aloo/Downloads/eli5_tutorial_voiceover.mp4")
        val videoUri = if (fileFallback.exists()) Uri.fromFile(fileFallback) else rawUri

        // Asynchronously extract and blur video frame for the ambient background layer
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                if (fileFallback.exists()) {
                    retriever.setDataSource(fileFallback.absolutePath)
                } else {
                    retriever.setDataSource(this@CircleSnipActivity, rawUri)
                }
                val frame = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                retriever.release()
                if (frame != null) {
                    val small = Bitmap.createScaledBitmap(frame, 60, 106, true)
                    withContext(Dispatchers.Main) {
                        ambientImageView.setImageBitmap(small)
                    }
                }
            } catch (_: Exception) {}
        }

        fun formatTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }

        val player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            val mediaItem = MediaItem.fromUri(videoUri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
        exoPlayer = player
        playerView.player = player

        // Auto-Hide Controls & Tap-to-Reveal Logic
        var controlsVisible = true
        val hideControlsRunnable = Runnable {
            if (controlsVisible && player.isPlaying) {
                controlsOverlay.animate()
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction {
                        controlsOverlay.visibility = View.GONE
                        controlsVisible = false
                    }
                    .start()
            }
        }

        fun resetHideTimer() {
            handler.removeCallbacks(hideControlsRunnable)
            if (player.isPlaying) {
                handler.postDelayed(hideControlsRunnable, 2500)
            }
        }

        fun showControls() {
            handler.removeCallbacks(hideControlsRunnable)
            controlsOverlay.visibility = View.VISIBLE
            controlsOverlay.animate().alpha(1f).setDuration(180).start()
            controlsVisible = true
            resetHideTimer()
        }

        fun toggleControls() {
            if (controlsVisible) {
                handler.removeCallbacks(hideControlsRunnable)
                controlsOverlay.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction {
                        controlsOverlay.visibility = View.GONE
                        controlsVisible = false
                    }
                    .start()
            } else {
                showControls()
            }
        }

        // Tap on player or background to toggle controls
        playerView.setOnClickListener { toggleControls() }
        videoContainer.setOnClickListener { toggleControls() }

        val togglePlayPause = {
            if (player.isPlaying) {
                player.pause()
                showControls()
                handler.removeCallbacks(hideControlsRunnable)
            } else {
                player.play()
                resetHideTimer()
            }
        }

        centerPlayPauseBtn.setOnClickListener {
            centerPlayPauseBtn.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).withEndAction {
                centerPlayPauseBtn.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }.start()
            togglePlayPause()
        }
        playPauseMiniBtn.setOnClickListener { togglePlayPause() }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val icon = if (isPlaying) R.drawable.ic_pause_rounded else R.drawable.ic_play_arrow_rounded
                centerPlayPauseBtn.setImageResource(icon)
                playPauseMiniBtn.setImageResource(icon)
                if (isPlaying) {
                    resetHideTimer()
                } else {
                    showControls()
                    handler.removeCallbacks(hideControlsRunnable)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = maxOf(1L, player.duration)
                    val cur = player.currentPosition
                    timeTv.text = "${formatTime(cur)} / ${formatTime(dur)}"
                    if (!seekBar.isPressed) {
                        seekBar.progress = (cur * 1000L / dur).toInt()
                    }
                    resetHideTimer()
                }
            }
        })

        updateProgressRunnable = object : Runnable {
            override fun run() {
                try {
                    val dur = player.duration
                    if (dur > 0) {
                        val cur = player.currentPosition
                        timeTv.text = "${formatTime(cur)} / ${formatTime(dur)}"
                        if (!seekBar.isPressed) {
                            seekBar.progress = (cur * 1000L / dur).toInt()
                        }
                    }
                } catch (_: Exception) {}
                handler.postDelayed(this, 250)
            }
        }
        handler.post(updateProgressRunnable!!)

        speedTv.setOnClickListener {
            currentSpeedIndex = (currentSpeedIndex + 1) % speeds.size
            val chosenSpeed = speeds[currentSpeedIndex]
            speedTv.text = if (chosenSpeed == 1.0f) "1x" else "${chosenSpeed}x"
            try {
                player.setPlaybackSpeed(chosenSpeed)
            } catch (_: Exception) {}
            resetHideTimer()
        }

        ccBtn.setOnClickListener {
            ccEnabled = !ccEnabled
            if (ccEnabled) {
                (ccBtn.background as? GradientDrawable)?.setColor(Color.parseColor("#6A0DAD"))
                Toast.makeText(this@CircleSnipActivity, "Captions enabled", Toast.LENGTH_SHORT).show()
            } else {
                (ccBtn.background as? GradientDrawable)?.setColor(Color.parseColor("#33FFFFFF"))
                Toast.makeText(this@CircleSnipActivity, "Captions disabled", Toast.LENGTH_SHORT).show()
            }
            resetHideTimer()
        }

        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && player.duration > 0) {
                    val targetMs = progress * player.duration / 1000L
                    player.seekTo(targetMs)
                    timeTv.text = "${formatTime(targetMs)} / ${formatTime(player.duration)}"
                    resetHideTimer()
                }
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {
                handler.removeCallbacks(hideControlsRunnable)
            }
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {
                resetHideTimer()
            }
        })

        dialog.setOnDismissListener {
            handler.removeCallbacks(hideControlsRunnable)
            updateProgressRunnable?.let { handler.removeCallbacks(it) }
            try {
                player.stop()
                player.release()
            } catch (_: Exception) {}
        }

        dialog.setContentView(dialogRoot)
        dialog.show()
    }

    /**
     * Creates an authentic-looking demo screen with interesting sample texts
     * so users can immediately experience "Circle to ELI5" even inside the app demo.
     */
    private fun createDemoScreenBitmap(): Bitmap {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val dp = displayMetrics.density

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isDark = SettingsManager.isDarkTheme(this)

        // Persistent theme background (Dark or Light)
        canvas.drawColor(if (isDark) Color.parseColor("#120E1A") else Color.parseColor("#F8F5FF"))

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#1C1826") else Color.WHITE
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#35234E") else Color.parseColor("#EFE9F7")
            style = Paint.Style.STROKE
            strokeWidth = 1f * dp
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782")
            textSize = 10.5f * dp
            isFakeBoldText = true
            letterSpacing = 0.08f
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.WHITE else Color.parseColor("#21112F")
            textSize = 14f * dp
            isFakeBoldText = true
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#4A3E56")
            textSize = 11.5f * dp
        }

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#A855F7") else Color.parseColor("#6A0DAD")
            textSize = 9.5f * dp
            isFakeBoldText = true
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#C4B5FD") else Color.parseColor("#A855F7")
            textSize = 9.5f * dp
        }

        // Section header
        canvas.drawText("DISCOVER FEED", 20f * dp, 85f * dp, headerPaint)

        val cardMargin = 16f * dp
        val cardW = width - (cardMargin * 2)
        val cardH = 114f * dp
        val cardR = 16f * dp
        val cardGap = 12f * dp
        val innerPad = 14f * dp
        val leftText = cardMargin + innerPad

        // Card 1: Quantum Computing
        val c1Top = 98f * dp
        val c1Bottom = c1Top + cardH
        val card1 = RectF(cardMargin, c1Top, cardMargin + cardW, c1Bottom)
        canvas.drawRoundRect(card1, cardR, cardR, cardPaint)
        canvas.drawRoundRect(card1, cardR, cardR, borderPaint)

        canvas.drawText("TECH EXPLAINER", leftText, c1Top + 20f * dp, tagPaint)
        canvas.drawText("2h ago", cardMargin + cardW - innerPad - 36f * dp, c1Top + 20f * dp, metaPaint)
        canvas.drawText("Quantum Entanglement", leftText, c1Top + 40f * dp, titlePaint)
        canvas.drawText("When two particles remain connected so", leftText, c1Top + 58f * dp, bodyPaint)
        canvas.drawText("actions on one affect the other instantly,", leftText, c1Top + 74f * dp, bodyPaint)
        canvas.drawText("even across light-years of distance.", leftText, c1Top + 90f * dp, bodyPaint)
        canvas.drawText("4.5k Likes • Share", leftText, c1Top + 105f * dp, metaPaint)

        // Card 2: Legal Terms
        val c2Top = c1Bottom + cardGap
        val c2Bottom = c2Top + cardH
        val card2 = RectF(cardMargin, c2Top, cardMargin + cardW, c2Bottom)
        canvas.drawRoundRect(card2, cardR, cardR, cardPaint)
        canvas.drawRoundRect(card2, cardR, cardR, borderPaint)

        canvas.drawText("LEGAL TERMS", leftText, c2Top + 20f * dp, tagPaint)
        canvas.drawText("Yesterday", cardMargin + cardW - innerPad - 46f * dp, c2Top + 20f * dp, metaPaint)
        canvas.drawText("Force Majeure Clause", leftText, c2Top + 40f * dp, titlePaint)
        canvas.drawText("An unforeseeable circumstance preventing", leftText, c2Top + 58f * dp, bodyPaint)
        canvas.drawText("someone from fulfilling a contractual pact,", leftText, c2Top + 74f * dp, bodyPaint)
        canvas.drawText("such as natural disasters or war.", leftText, c2Top + 90f * dp, bodyPaint)
        canvas.drawText("1.2k Replies • Retweet", leftText, c2Top + 105f * dp, metaPaint)

        // Card 3: Adaptive localized demo card
        val c3Top = c2Bottom + cardGap
        val c3Bottom = c3Top + cardH
        val card3 = RectF(cardMargin, c3Top, cardMargin + cardW, c3Bottom)
        canvas.drawRoundRect(card3, cardR, cardR, cardPaint)
        canvas.drawRoundRect(card3, cardR, cardR, borderPaint)

        val lang = SettingsManager.getLanguage(this).lowercase()
        when {
            lang.contains("urdu") || lang == "ur" -> {
                canvas.drawText("سائنس اور ٹیکنالوجی", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("کوانٹم الجھاؤ", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("جب دو ذرات اس طرح آپس میں جڑ جائیں کہ", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("ایک پر عمل کرنے سے دوسرا فوری متاثر ہو،", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("چاہے وہ نوری سالوں کی دوری پر ہی کیوں نہ ہوں۔", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("persian") || lang.contains("farsi") || lang == "fa" -> {
                canvas.drawText("علم و فناوری", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("درهم‌تنیدگی کوانتومی", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("پدیده‌ای که در آن دو ذره چنان به هم پیوسته‌اند", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("که تغییر در یکی بلافاصله بر دیگری اثر می‌گذارد،", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("حتی اگر سال‌های نوری از هم فاصله داشته باشند.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("bengali") || lang == "bn" -> {
                canvas.drawText("বিজ্ঞান ও প্রযুক্তি", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("কোয়ান্টাম এন্ট্যাঙ্গলমেন্ট", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("যখন দুটি কণা এমনভাবে সংযুক্ত থাকে যে", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("একটির অবস্থা পরিবর্তনের সাথে সাথেই", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("দূরত্ব নির্বিশেষে অন্যটিতে প্রভাব পড়ে।", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("tamil") || lang == "ta" -> {
                canvas.drawText("அறிவியல் & தொழில்நுட்பம்", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("குவாண்டம் பின்னல்", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("இரண்டு துகள்கள் மிகத் தொலைவில் இருந்தாலும்,", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("ஒன்றின் மாற்றம் உடனடியாக மற்றொன்றை", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("பாதிக்கும் விசித்திரமான நிகழ்வு.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("telugu") || lang == "te" -> {
                canvas.drawText("సైన్స్ & టెక్నాలజీ", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("క్వాంటమ్ ఎంటాంగిల్మెంట్", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("రెండు కణాలు ఎంత దూరంలో ఉన్నప్పటికీ,", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("ఒకదానిలో మార్పు వెంటనే రెండవదానిపై", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("ప్రభావం చూపే భౌతిక దృగ్విషయం.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("arabic") || lang == "ar" -> {
                canvas.drawText("العلوم والتكنولوجيا", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("التشابك الكمي", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("عندما يرتبط جسيمان بحيث تؤثر التغيرات", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("في أحدهما فوراً على الآخر بغض النظر", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("عن المسافة الفاصلة بينهما.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("russian") || lang == "ru" -> {
                canvas.drawText("НАУКА И ТЕХНОЛОГИИ", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("Квантовая запутанность", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("Явление, при котором квантовые состояния", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("двух частиц оказываются взаимосвязанными", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("даже на колоссальных расстояниях.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("hindi") || lang.contains("marathi") || lang == "hi" || lang == "mr" -> {
                canvas.drawText("विज्ञान एवं तकनीक", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("क्वांटम उलझाव", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("जब दो कण एक दूसरे से जुड़े होते हैं,", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("तो एक की स्थिति बदलने पर दूसरे पर तुरंत", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("असर होता है, चाहे कितनी भी दूरी हो।", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("japanese") || lang == "ja" -> {
                canvas.drawText("テクノロジー", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("量子もつれの原理", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("2つの粒子が強く结びつき、互いの", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("状態がどれほど離れていても瞬時に", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("影響し合う物理現象です。", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("chinese") || lang == "zh" -> {
                canvas.drawText("科学技术", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("量子纠缠现象", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("当两个微观粒子相互纠缠在一起时，", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("即使相隔遥远，测量其中一个也会", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("瞬间影响另一个粒子的状态。", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("spanish") || lang == "es" -> {
                canvas.drawText("FINANZAS", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("Paradoja de Estanflación", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("Una combinación de estancamiento económico", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("con alta inflación y desempleo sostenido,", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("afectando el poder adquisitivo.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            else -> {
                canvas.drawText("FINANCE", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("Stagflation Paradox", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("A rare toxic economic combination of", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("stagnant growth, high unemployment, and", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("rapidly escalating consumer inflation.", leftText, c3Top + 90f * dp, bodyPaint)
            }
        }
        canvas.drawText("3.1k Reads • Discussion", leftText, c3Top + 105f * dp, metaPaint)

        return bitmap
    }
}

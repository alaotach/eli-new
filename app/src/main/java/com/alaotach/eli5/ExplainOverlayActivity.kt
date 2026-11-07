package com.alaotach.eli5

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ExplainOverlayActivity : Activity() {
    companion object {
        const val EXTRA_TEXT = "extra_text"
    }

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var bodyTextView: TextView? = null
    private var shimmerSkeleton: LinearLayout? = null
    private var errorContainer: LinearLayout? = null
    private var errorText: TextView? = null
    private var retryButton: TextView? = null
    private var tooLongBanner: TextView? = null
    private var footerChipsRow: LinearLayout? = null

    private var currentJob: Job? = null
    private var typewriterJob: Job? = null
    private var isExplanationStreaming: Boolean = false
    private val cachedExplanations: MutableMap<String, String> = LinkedHashMap()

    private var originalSelectedText: String = ""
    private var processedText: String = ""
    private var currentExplanationText: String = ""
    private var targetExplanationText: String = ""
    private var displayedExplanationText: String = ""
    private var currentActiveMode: String = "ELI5"

    private val activityScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private val dp: Float
        get() = resources.displayMetrics.density

    private val isDark: Boolean
        get() = SettingsManager.isDarkTheme(this)

    // Palette per spec
    private val purpleAccent: Int
        get() = Color.parseColor(if (isDark) "#8B5CF6" else "#7C3AED")

    private val purpleBorderColor: Int
        get() = Color.argb((255 * 0.40f).toInt(), 139, 92, 246)

    private val cardBgColor: Int
        get() = Color.parseColor(if (isDark) "#1C1826" else "#FFFFFF")

    private val contentBoxBgColor: Int
        get() = Color.parseColor(if (isDark) "#161320" else "#F7F5FA")

    private val textPrimaryColor: Int
        get() = Color.parseColor(if (isDark) "#FFFFFF" else "#1C1826")

    private val textMutedColor: Int
        get() = Color.parseColor(if (isDark) "#9E9AA8" else "#6B6678")

    private val tabsBgColor: Int
        get() = Color.parseColor(if (isDark) "#14101E" else "#F0EDF7")

    private val handleColor: Int
        get() = Color.parseColor(if (isDark) "#443E54" else "#DCD8E8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        setContentView(root)

        originalSelectedText = intent?.getStringExtra(EXTRA_TEXT) ?: ""
        if (originalSelectedText.isBlank()) {
            finish()
            return
        }

        // Too-long text handling (>600 chars): explain first part to stay responsive
        if (originalSelectedText.length > 600) {
            processedText = originalSelectedText.take(600)
        } else {
            processedText = originalSelectedText
        }

        // Remember last used mode
        currentActiveMode = SettingsManager.getLastMode(this)
        if (currentActiveMode !in listOf("ELI5", "Direct", "Detailed")) {
            currentActiveMode = "ELI5"
        }

        showBottomSheet()
        requestExplanation(currentActiveMode)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showLoadingState() {
        bodyTextView?.visibility = View.GONE
        errorContainer?.visibility = View.GONE
        footerChipsRow?.visibility = View.GONE
        shimmerSkeleton?.visibility = View.VISIBLE
    }

    private fun showContentState() {
        shimmerSkeleton?.visibility = View.GONE
        errorContainer?.visibility = View.GONE
        bodyTextView?.visibility = View.VISIBLE
        footerChipsRow?.visibility = View.VISIBLE
    }

    private fun showErrorState(message: String) {
        currentJob?.cancel()
        typewriterJob?.cancel()
        isExplanationStreaming = false
        shimmerSkeleton?.visibility = View.GONE
        bodyTextView?.visibility = View.GONE
        footerChipsRow?.visibility = View.GONE
        errorContainer?.visibility = View.VISIBLE
        errorText?.text = message
    }

    private fun requestExplanation(mode: String, extraModifier: String? = null) {
        currentActiveMode = mode
        SettingsManager.setLastMode(this, mode)

        currentJob?.cancel()
        typewriterJob?.cancel()

        val cacheKey = if (extraModifier.isNullOrEmpty()) mode else "$mode:$extraModifier"

        if (cachedExplanations.containsKey(cacheKey)) {
            val cached = cachedExplanations[cacheKey]!!
            currentExplanationText = cached
            showContentState()
            bodyTextView?.let { tv ->
                tv.alpha = 1.0f
                Markwon.create(this).setMarkdown(tv, cached)
            }
            return
        }

        if (!isNetworkAvailable()) {
            showErrorState("No connection. Try again")
            return
        }

        showLoadingState()

        currentJob = activityScope.launch {
            var firstChunk = true
            isExplanationStreaming = true
            targetExplanationText = ""
            displayedExplanationText = ""

            typewriterJob = activityScope.launch(Dispatchers.Main) {
                val markwon = Markwon.create(this@ExplainOverlayActivity)
                while (true) {
                    if (displayedExplanationText.length < targetExplanationText.length) {
                        val diff = targetExplanationText.length - displayedExplanationText.length
                        val step = when {
                            diff > 120 -> 18
                            diff > 50 -> 9
                            diff > 15 -> 4
                            diff > 5 -> 2
                            else -> 1
                        }
                        val nextLen = Math.min(targetExplanationText.length, displayedExplanationText.length + step)
                        displayedExplanationText = targetExplanationText.substring(0, nextLen)
                        bodyTextView?.let { tv ->
                            tv.alpha = 1.0f
                            markwon.setMarkdown(tv, "$displayedExplanationText▎")
                        }
                        delay(14L)
                    } else {
                        if (!isExplanationStreaming) {
                            bodyTextView?.let { tv ->
                                tv.alpha = 1.0f
                                markwon.setMarkdown(tv, displayedExplanationText)
                            }
                            return@launch
                        }
                        delay(25L)
                    }
                }
            }

            val language = SettingsManager.getLanguage(this@ExplainOverlayActivity)
            val explanation = fetchExplanation(processedText, mode, language, extraModifier) { partialText ->
                withContext(Dispatchers.Main) {
                    if (firstChunk) {
                        firstChunk = false
                        showContentState()
                        bodyTextView?.text = ""
                    }
                    targetExplanationText = partialText
                }
            }

            withContext(Dispatchers.Main) {
                isExplanationStreaming = false
                if (explanation.startsWith("Error:") || explanation.startsWith("API returned")) {
                    showErrorState("Unable to load explanation. Tap retry.")
                } else {
                    currentExplanationText = explanation
                    cachedExplanations[cacheKey] = explanation
                    showContentState()
                    bodyTextView?.let { tv ->
                        tv.alpha = 1.0f
                    }
                    HistoryManager.saveItem(
                        this@ExplainOverlayActivity,
                        HistoryItem(System.currentTimeMillis(), originalSelectedText, explanation, mode)
                    )
                }
            }
        }
    }

    private suspend fun fetchExplanation(
        text: String,
        mode: String,
        language: String,
        extraModifier: String?,
        onChunk: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${BuildConfig.BACKEND_URL}/api/explain"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 ELI5-Android/2.2.1")
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true

            val resolvedMode = when (mode) {
                "Direct" -> "Straightforward"
                "Detailed" -> "Detailed"
                else -> "ELI5"
            }

            var promptText = text
            if (!extraModifier.isNullOrEmpty()) {
                promptText = "$text\n\n[$extraModifier]"
            }
            if (language.equals("Hindi", ignoreCase = true)) {
                promptText = "$promptText\n\n[Please explain in clear, natural Hindi]"
            }

            val payload = JSONObject().apply {
                put("text", promptText)
                put("mode", resolvedMode)
                put("language", language)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val fullResponse = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val json = JSONObject(data)
                            val delta = json.getJSONArray("choices").getJSONObject(0).getJSONObject("delta")
                            val content = delta.optString("content", "")
                            if (content.isNotEmpty()) {
                                fullResponse.append(content)
                                onChunk(fullResponse.toString())
                            }
                        } catch (_: Exception) {}
                    }
                }
                reader.close()
                fullResponse.toString().trim()
            } else {
                "Error: API returned ${conn.responseCode}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun showBottomSheet() {
        // Haptic feedback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(40L)
            }
        }

        bottomSheetDialog = BottomSheetDialog(this)

        // Root Container: 28dp corner radius, 1dp purple border (40% opacity), 20dp padding
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(cardBgColor)
                val r = dp * 28f
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                setStroke((dp * 1.0f).toInt(), purpleBorderColor)
            }
            setPadding((dp * 20).toInt(), (dp * 10).toInt(), (dp * 20).toInt(), (dp * 20).toInt())
        }

        // 1. Drag Handle
        val handleContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, (dp * 12).toInt())
        }
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                setColor(handleColor)
                cornerRadius = 2 * dp
            }
            layoutParams = LinearLayout.LayoutParams((36 * dp).toInt(), (4 * dp).toInt())
        }
        handleContainer.addView(handle)
        root.addView(handleContainer)

        // 2. Header: Icon + "ELI5" on left; Copy, Share, Close on right
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (dp * 8).toInt())
        }

        val appTitleBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val iconTv = TextView(this).apply {
            text = "✨"
            textSize = 17.0f
            setPadding(0, 0, (dp * 6).toInt(), 0)
        }
        val titleTv = TextView(this).apply {
            text = "ELI5"
            setTextColor(textPrimaryColor)
            textSize = 18.0f
            setTypeface(typeface, Typeface.BOLD)
        }
        appTitleBox.addView(iconTv)
        appTitleBox.addView(titleTv)
        headerRow.addView(appTitleBox)

        // Copy button
        val copyBtn = TextView(this).apply {
            text = "Copy"
            setTextColor(purpleAccent)
            textSize = 14.0f
            setTypeface(typeface, Typeface.BOLD)
            setPadding((dp * 8).toInt(), (dp * 6).toInt(), (dp * 8).toInt(), (dp * 6).toInt())
            setOnClickListener {
                if (currentExplanationText.isNotBlank()) {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("ELI5 Explanation", currentExplanationText))
                    Toast.makeText(this@ExplainOverlayActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }
        headerRow.addView(copyBtn)

        // Share button
        val shareBtn = TextView(this).apply {
            text = "Share"
            setTextColor(purpleAccent)
            textSize = 14.0f
            setTypeface(typeface, Typeface.BOLD)
            setPadding((dp * 8).toInt(), (dp * 6).toInt(), (dp * 8).toInt(), (dp * 6).toInt())
            setOnClickListener {
                if (currentExplanationText.isNotBlank()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, currentExplanationText)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share ELI5 Explanation"))
                }
            }
        }
        headerRow.addView(shareBtn)

        // Close button
        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(textMutedColor)
            textSize = 18.0f
            setTypeface(typeface, Typeface.BOLD)
            setPadding((dp * 8).toInt(), (dp * 6).toInt(), 0, (dp * 6).toInt())
            setOnClickListener {
                bottomSheetDialog.dismiss()
            }
        }
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // 3. Selected text preview (One line, truncated in muted gray)
        val previewClean = originalSelectedText.replace('\n', ' ').trim()
        val previewTv = TextView(this).apply {
            text = "\"$previewClean\""
            setTextColor(textMutedColor)
            textSize = 13.0f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 0, 0, (dp * 10).toInt())
        }
        root.addView(previewTv)

        // Notice if text was truncated (>600 chars)
        if (originalSelectedText.length > 600) {
            tooLongBanner = TextView(this).apply {
                text = "Selection is long, explaining the first part"
                setTextColor(purpleAccent)
                textSize = 12.0f
                setPadding(0, 0, 0, (dp * 8).toInt())
            }
            root.addView(tooLongBanner)
        }

        // 4. Mode tabs: ELI5 / Direct / Detailed
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(tabsBgColor)
                cornerRadius = 12 * dp
                setStroke((dp * 1.0f).toInt(), purpleBorderColor)
            }
            setPadding((dp * 4).toInt(), (dp * 4).toInt(), (dp * 4).toInt(), (dp * 4).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (dp * 12).toInt()
            }
        }

        val modeViews = ArrayList<TextView>()
        val modes = listOf("ELI5", "Direct", "Detailed")

        for (modeName in modes) {
            val tv = TextView(this).apply {
                text = modeName
                gravity = Gravity.CENTER
                textSize = 14.0f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, (dp * 7).toInt(), 0, (dp * 7).toInt())
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = dp * 8
                }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            }
            modeViews.add(tv)
            modeRow.addView(tv)

            tv.setOnClickListener {
                for (btn in modeViews) {
                    btn.setTextColor(textMutedColor)
                    (btn.background as? GradientDrawable)?.setColor(Color.TRANSPARENT)
                }
                tv.setTextColor(Color.WHITE)
                (tv.background as? GradientDrawable)?.setColor(purpleAccent)

                requestExplanation(modeName)
            }
        }

        // Highlight the active mode based on last used / default
        val activeIndex = modes.indexOf(currentActiveMode).coerceAtLeast(0)
        for (i in modes.indices) {
            if (i == activeIndex) {
                modeViews[i].setTextColor(Color.WHITE)
                (modeViews[i].background as? GradientDrawable)?.setColor(purpleAccent)
            } else {
                modeViews[i].setTextColor(textMutedColor)
                (modeViews[i].background as? GradientDrawable)?.setColor(Color.TRANSPARENT)
            }
        }
        root.addView(modeRow)

        // 5. Answer Area Container
        val contentBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(contentBoxBgColor)
                cornerRadius = dp * 16
            }
            setPadding((dp * 16).toInt(), (dp * 14).toInt(), (dp * 16).toInt(), (dp * 14).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // Shimmer Skeleton View (Loading state)
        shimmerSkeleton = createShimmerSkeletonView().also {
            contentBox.addView(it)
        }

        // Error Container (Offline / rate limit)
        errorContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(0, (dp * 16).toInt(), 0, (dp * 16).toInt())
        }
        errorText = TextView(this).apply {
            text = "No connection. Try again"
            setTextColor(textPrimaryColor)
            textSize = 15.0f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (dp * 12).toInt())
        }
        retryButton = TextView(this).apply {
            text = "Retry"
            setTextColor(Color.WHITE)
            textSize = 14.0f
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                setColor(purpleAccent)
                cornerRadius = dp * 8
            }
            setPadding((dp * 20).toInt(), (dp * 8).toInt(), (dp * 20).toInt(), (dp * 8).toInt())
            setOnClickListener {
                requestExplanation(currentActiveMode)
            }
        }
        errorContainer?.addView(errorText)
        errorContainer?.addView(retryButton)
        contentBox.addView(errorContainer)

        // Scrollable Text View
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength((dp * 20).toInt())
        }

        // Text: 17sp, line height 26sp, white on #1C1826
        bodyTextView = TextView(this).apply {
            setTextColor(textPrimaryColor)
            textSize = 17.0f
            setLineSpacing(dp * 6, 1.15f)
            visibility = View.GONE
        }
        scrollView.addView(bodyTextView)
        contentBox.addView(scrollView)
        root.addView(contentBox)

        // 6. Footer Chips (Phase 2: "Simpler", "Give an example")
        footerChipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (dp * 10).toInt(), 0, 0)
        }

        val simplerChip = createChip("✨ Simpler") {
            requestExplanation(currentActiveMode, extraModifier = "Explain this even simpler with fun analogies.")
        }
        val exampleChip = createChip("💡 Give an example") {
            requestExplanation(currentActiveMode, extraModifier = "Provide a real-world, concrete example.")
        }
        footerChipsRow?.addView(simplerChip)
        footerChipsRow?.addView(exampleChip)
        root.addView(footerChipsRow)

        // Setup bottom sheet behavior
        bottomSheetDialog.setContentView(root)
        bottomSheetDialog.setOnDismissListener { finish() }

        bottomSheetDialog.setOnShowListener { dialog ->
            val sheetDialog = dialog as BottomSheetDialog
            val sheet = sheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(it)
                behavior.isFitToContents = true
                behavior.skipCollapsed = true
                val maxAllowedHeight = (resources.displayMetrics.heightPixels * 0.55).toInt()
                it.layoutParams = it.layoutParams.apply {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                behavior.maxHeight = maxAllowedHeight
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        bottomSheetDialog.show()
    }

    private fun createChip(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13.0f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(purpleAccent)
            background = GradientDrawable().apply {
                setColor(if (isDark) Color.parseColor("#251F33") else Color.parseColor("#EDE9FE"))
                cornerRadius = dp * 14
                setStroke((dp * 1f).toInt(), purpleBorderColor)
            }
            setPadding((dp * 14).toInt(), (dp * 6).toInt(), (dp * 14).toInt(), (dp * 6).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = (dp * 8).toInt()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createShimmerSkeletonView(): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (dp * 4).toInt(), 0, (dp * 4).toInt())
        }

        val barColor = if (isDark) Color.parseColor("#2E273D") else Color.parseColor("#E4DFEE")
        val widths = listOf(1.0f, 0.88f, 0.94f, 0.55f)

        for (w in widths) {
            val bar = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(barColor)
                    cornerRadius = dp * 5
                }
                layoutParams = LinearLayout.LayoutParams(
                    (resources.displayMetrics.widthPixels * 0.7f * w).toInt(),
                    (dp * 13).toInt()
                ).apply {
                    bottomMargin = (dp * 12).toInt()
                }
            }
            container.addView(bar)

            val pulseAnim = ObjectAnimator.ofFloat(bar, "alpha", 0.35f, 0.85f).apply {
                duration = 750L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
            }
            pulseAnim.start()
        }
        return container
    }
}

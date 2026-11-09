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
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
        const val EXTRA_RESULT = "extra_result"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_IS_SNIP = "extra_is_snip"
    }

    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var bodyTextView: TextView? = null
    private var shimmerSkeleton: LinearLayout? = null
    private var errorContainer: LinearLayout? = null
    private var errorText: TextView? = null
    private var retryButton: TextView? = null
    private var tooLongBanner: TextView? = null
    private var footerChipsRow: LinearLayout? = null

    private var isFromSnip: Boolean = false

    private var currentJob: Job? = null
    private var typewriterJob: Job? = null
    private var isExplanationStreaming: Boolean = false
    private val cachedExplanations: MutableMap<String, String> = LinkedHashMap()

    private var originalSelectedText: String = ""
    private var processedText: String = ""
    private var currentExplanationText: String = ""
    private var targetExplanationText: String = ""
    private var displayedExplanationText: String = ""
    private var currentActiveMode: String = "Direct"
    private var userDefaultLanguage: String = "English"
    private var currentActiveLanguage: String = "English"
    private var secondaryLanguage: String = "Hindi"

    private var primaryLangBtn: TextView? = null
    private var secondaryLangBtn: TextView? = null
    private var moreLangBtn: TextView? = null

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

        isFromSnip = intent?.getBooleanExtra(EXTRA_IS_SNIP, false) ?: false

        val cachedResult = intent?.getStringExtra(EXTRA_RESULT)
        val initialMode = intent?.getStringExtra(EXTRA_MODE)

        // Too-long text handling (>600 chars): explain first part to stay responsive
        if (originalSelectedText.length > 600) {
            processedText = originalSelectedText.take(600)
        } else {
            processedText = originalSelectedText
        }

        if (!initialMode.isNullOrEmpty() && initialMode in listOf("ELI5", "Direct", "Detailed")) {
            currentActiveMode = initialMode
        } else {
            // Remember last used mode
            currentActiveMode = SettingsManager.getLastMode(this)
            if (currentActiveMode !in listOf("ELI5", "Direct", "Detailed")) {
                currentActiveMode = "Direct"
            }
        }

        // Preload any saved history results for this exact text so tab switching and reopening never re-fetch
        try {
            val matchingHistory = HistoryManager.getHistory(this).filter { it.originalText == originalSelectedText }
            for (hist in matchingHistory) {
                if (hist.mode in listOf("ELI5", "Direct", "Detailed") && hist.resultText.isNotBlank()) {
                    cachedExplanations[hist.mode] = hist.resultText
                }
            }
        } catch (_: Exception) {}

        if (!cachedResult.isNullOrEmpty()) {
            cachedExplanations[currentActiveMode] = cachedResult
        }

        userDefaultLanguage = SettingsManager.getLanguage(this)
        currentActiveLanguage = userDefaultLanguage
        secondaryLanguage = if (userDefaultLanguage.equals("English", ignoreCase = true)) "Hindi" else "English"

        showBottomSheet()
        requestExplanation(currentActiveMode, currentActiveLanguage)
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val activeNetwork = cm.activeNetwork ?: return true
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return true
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true // If permission check fails or is restricted, proceed gracefully
        }
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

    private fun requestExplanation(
        mode: String,
        language: String = currentActiveLanguage,
        extraModifier: String? = null
    ) {
        currentActiveMode = mode
        currentActiveLanguage = language
        SettingsManager.setLastMode(this, mode)

        currentJob?.cancel()
        typewriterJob?.cancel()

        val cacheKey = "$mode:$language" + if (extraModifier.isNullOrEmpty()) "" else ":$extraModifier"

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
            if (isFromSnip) {
                promptText = """
                    [Context: The user selected this text by circling/snipping on their mobile screen.]
                    [Instruction: Focus exclusively on explaining the central entity, concept, punchline, jargon, or question the user circled. Disregard any peripheral timestamps, battery/status text, buttons, or accidental edge clippings. Give a clear, direct, and insightful explanation.]

                    $text
                """.trimIndent()
            }
            if (!extraModifier.isNullOrEmpty()) {
                promptText = "$promptText\n\n[$extraModifier]"
            }
            if (!language.equals("English", ignoreCase = true)) {
                promptText = "$promptText\n\n[Please explain in clear, natural $language]"
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
        try {
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
        } catch (e: Exception) {
            // Ignore vibration errors if restricted on device
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
                    ShareImageGenerator.shareExplanation(
                        this@ExplainOverlayActivity,
                        originalSelectedText,
                        currentExplanationText,
                        currentActiveMode,
                        activityScope
                    )
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

                requestExplanation(modeName, currentActiveLanguage)
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

        // 4b. Language Switcher Row (Always quick toggle: English <-> Default Language, plus 35+ languages)
        val languageRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
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

        primaryLangBtn = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 13.0f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, (dp * 6).toInt(), 0, (dp * 6).toInt())
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp * 8
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setOnClickListener {
                requestExplanation(currentActiveMode, userDefaultLanguage)
                updateLanguageButtonsUi()
            }
        }
        languageRow.addView(primaryLangBtn)

        secondaryLangBtn = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 13.0f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, (dp * 6).toInt(), 0, (dp * 6).toInt())
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp * 8
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setOnClickListener {
                requestExplanation(currentActiveMode, secondaryLanguage)
                updateLanguageButtonsUi()
            }
        }
        languageRow.addView(secondaryLangBtn)

        moreLangBtn = TextView(this).apply {
            text = "🌐 ▾"
            gravity = Gravity.CENTER
            textSize = 13.0f
            setTypeface(typeface, Typeface.BOLD)
            setPadding((dp * 10).toInt(), (dp * 6).toInt(), (dp * 10).toInt(), (dp * 6).toInt())
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp * 8
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                showLanguagePickerDialog { selected ->
                    if (!selected.englishName.equals(userDefaultLanguage, ignoreCase = true)) {
                        secondaryLanguage = selected.englishName
                    }
                    requestExplanation(currentActiveMode, selected.englishName)
                    updateLanguageButtonsUi()
                }
            }
        }
        languageRow.addView(moreLangBtn)

        updateLanguageButtonsUi()
        root.addView(languageRow)

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
            val sheetDialog = dialog as? BottomSheetDialog ?: return@setOnShowListener
            val sheet = sheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                try {
                    it.setBackgroundColor(Color.TRANSPARENT)
                    val behavior = BottomSheetBehavior.from(it)
                    behavior.isFitToContents = true
                    behavior.skipCollapsed = true
                    val maxAllowedHeight = (resources.displayMetrics.heightPixels * 0.55).toInt()
                    behavior.maxHeight = maxAllowedHeight
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                } catch (e: Exception) {
                    // Fallback safely
                }
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

    private fun updateLanguageButtonsUi() {
        val defaultLangObj = LanguageManager.getLanguageByEnglishName(userDefaultLanguage)
        val secLangObj = LanguageManager.getLanguageByEnglishName(secondaryLanguage)

        val isPrimaryActive = currentActiveLanguage.equals(userDefaultLanguage, ignoreCase = true)
        val isSecondaryActive = currentActiveLanguage.equals(secondaryLanguage, ignoreCase = true)

        primaryLangBtn?.apply {
            text = "${defaultLangObj.flag} ${defaultLangObj.nativeName}"
            if (isPrimaryActive) {
                setTextColor(Color.WHITE)
                (background as? GradientDrawable)?.setColor(purpleAccent)
            } else {
                setTextColor(textMutedColor)
                (background as? GradientDrawable)?.setColor(Color.TRANSPARENT)
            }
        }

        secondaryLangBtn?.apply {
            text = "${secLangObj.flag} ${secLangObj.nativeName}"
            if (isSecondaryActive) {
                setTextColor(Color.WHITE)
                (background as? GradientDrawable)?.setColor(purpleAccent)
            } else {
                setTextColor(textMutedColor)
                (background as? GradientDrawable)?.setColor(Color.TRANSPARENT)
            }
        }

        moreLangBtn?.apply {
            if (!isPrimaryActive && !isSecondaryActive) {
                val currentObj = LanguageManager.getLanguageByEnglishName(currentActiveLanguage)
                text = "${currentObj.flag} ${currentObj.nativeName} ▾"
                setTextColor(Color.WHITE)
                (background as? GradientDrawable)?.setColor(purpleAccent)
            } else {
                text = "🌐 ▾"
                setTextColor(textMutedColor)
                (background as? GradientDrawable)?.setColor(Color.TRANSPARENT)
            }
        }
    }

    private fun showLanguagePickerDialog(onLanguageSelected: (Language) -> Unit) {
        val dialog = BottomSheetDialog(this)
        val dialogRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardBgColor)
            setPadding((dp * 20).toInt(), (dp * 16).toInt(), (dp * 20).toInt(), (dp * 24).toInt())
        }

        val titleTv = TextView(this).apply {
            text = "Select Language / भाषा चुनें"
            textSize = 18f
            setTextColor(textPrimaryColor)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, (dp * 12).toInt())
        }
        dialogRoot.addView(titleTv)

        val searchBox = EditText(this).apply {
            hint = "Search 35+ languages..."
            setHintTextColor(textMutedColor)
            setTextColor(textPrimaryColor)
            textSize = 14f
            background = GradientDrawable().apply {
                setColor(contentBoxBgColor)
                cornerRadius = dp * 10
                setStroke((dp * 1).toInt(), purpleBorderColor)
            }
            setPadding((dp * 12).toInt(), (dp * 10).toInt(), (dp * 12).toInt(), (dp * 10).toInt())
        }
        dialogRoot.addView(searchBox)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (dp * 360).toInt()
            ).apply {
                topMargin = (dp * 12).toInt()
            }
        }

        val langListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(langListContainer)
        dialogRoot.addView(scroll)

        fun renderLanguages(query: String = "") {
            langListContainer.removeAllViews()
            val filtered = if (query.isBlank()) {
                LanguageManager.SUPPORTED_LANGUAGES
            } else {
                LanguageManager.SUPPORTED_LANGUAGES.filter {
                    it.englishName.contains(query, ignoreCase = true) ||
                    it.nativeName.contains(query, ignoreCase = true) ||
                    it.code.contains(query, ignoreCase = true)
                }
            }

            for (lang in filtered) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding((dp * 10).toInt(), (dp * 12).toInt(), (dp * 10).toInt(), (dp * 12).toInt())
                    background = GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                        cornerRadius = dp * 8
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        onLanguageSelected(lang)
                        dialog.dismiss()
                    }
                }

                val flagTv = TextView(this).apply {
                    text = lang.flag
                    textSize = 18f
                    setPadding(0, 0, (dp * 12).toInt(), 0)
                }
                val nameTv = TextView(this).apply {
                    text = lang.displayName
                    textSize = 15f
                    setTextColor(if (lang.englishName.equals(currentActiveLanguage, ignoreCase = true)) purpleAccent else textPrimaryColor)
                    if (lang.englishName.equals(currentActiveLanguage, ignoreCase = true)) {
                        setTypeface(typeface, Typeface.BOLD)
                    }
                }
                row.addView(flagTv)
                row.addView(nameTv)
                langListContainer.addView(row)
            }
        }

        renderLanguages()

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                renderLanguages(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        dialog.setContentView(dialogRoot)
        dialog.show()
    }
}

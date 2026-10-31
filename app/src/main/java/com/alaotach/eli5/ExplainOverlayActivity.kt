package com.alaotach.eli5

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

    private var bodyTextView: TextView? = null
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private var currentJob: Job? = null
    private var isExplanationStreaming: Boolean = false
    private var typewriterJob: Job? = null
    private val cachedExplanations: MutableMap<String, String> = LinkedHashMap()
    private var currentExplanationText: String = ""
    private var originalSelectedText: String = ""
    private val activityScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var targetExplanationText: String = ""
    private var displayedExplanationText: String = ""

    private val isDark: Boolean
        get() = SettingsManager.isDarkTheme(this)

    private val bgOuter: String
        get() = if (isDark) "#202020" else "#FFFFFF"

    private val bgInner: String
        get() = if (isDark) "#282828" else "#F0F0F0"

    private val strokeColor: String
        get() = if (isDark) "#333333" else "#CCCCCC"

    private val textMainPrimary: Int
        get() = if (isDark) Color.WHITE else ViewCompat.MEASURED_STATE_MASK

    private val textMainFaded: Int
        get() = Color.parseColor(if (isDark) "#E0E0E0" else "#212121")

    private val hintColor: String
        get() = if (isDark) "#AAAAAA" else "#888888"

    private val bgMainStr: String
        get() = if (isDark) "#1E1E1E" else "#FFFFFF"

    private val bgContentStr: String
        get() = if (isDark) "#252525" else "#F5F5F5"

    private val modesRowBgStr: String
        get() = if (isDark) "#111111" else "#EAEAEA"

    private val textMainStr: String
        get() = if (isDark) "#E0E0E0" else "#212121"

    private val textTitleColor: Int
        get() = if (isDark) Color.WHITE else ViewCompat.MEASURED_STATE_MASK

    private val textFadedStr: String
        get() = if (isDark) "#888888" else "#666666"

    private val modeActiveBgStr: String
        get() = if (isDark) "#007ACC" else "#005A9E"

    private val handleColorStr: String
        get() = if (isDark) "#444444" else "#CCCCCC"

    private val dp: Float
        get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        setContentView(root)

        originalSelectedText = intent?.getStringExtra(EXTRA_TEXT) ?: ""
        if (originalSelectedText.isBlank()) {
            finish()
        } else {
            showBottomSheet()
            requestExplanation("ELI5")
        }
    }

    private fun requestExplanation(mode: String) {
        currentJob?.cancel()
        typewriterJob?.cancel()

        if (cachedExplanations.containsKey(mode)) {
            val cached = cachedExplanations[mode]!!
            currentExplanationText = cached
            bodyTextView?.let { tv ->
                (tv.tag as? ObjectAnimator)?.cancel()
                tv.alpha = 1.0f
                tv.setTextColor(Color.parseColor(textMainStr))
                Markwon.create(this).setMarkdown(tv, cached)
            }
            return
        }

        currentExplanationText = "Explaining..."
        bodyTextView?.let { tv ->
            tv.setTextColor(Color.parseColor(handleColorStr))
            tv.text = currentExplanationText
            val anim = ObjectAnimator.ofFloat(tv, "alpha", 0.3f, 1.0f).apply {
                duration = 600L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
            }
            anim.start()
            tv.tag = anim
        }

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
                            diff > 100 -> 15
                            diff > 40 -> 8
                            diff > 10 -> 3
                            diff > 5 -> 2
                            else -> 1
                        }
                        val nextLen = Math.min(targetExplanationText.length, displayedExplanationText.length + step)
                        displayedExplanationText = targetExplanationText.substring(0, nextLen)
                        bodyTextView?.let { tv ->
                            markwon.setMarkdown(tv, "$displayedExplanationText▎")
                        }
                        delay(16L)
                    } else {
                        if (!isExplanationStreaming) {
                            bodyTextView?.let { tv ->
                                markwon.setMarkdown(tv, displayedExplanationText)
                            }
                            return@launch
                        }
                        delay(30L)
                    }
                }
            }

            val explanation = fetchExplanation(originalSelectedText, mode) { partialText ->
                if (firstChunk) {
                    firstChunk = false
                    bodyTextView?.let { tv ->
                        (tv.tag as? ObjectAnimator)?.cancel()
                        tv.alpha = 1.0f
                        tv.setTextColor(Color.parseColor(textMainStr))
                        tv.text = ""
                    }
                }
                targetExplanationText = partialText
            }

            isExplanationStreaming = false
            currentExplanationText = explanation
            cachedExplanations[mode] = explanation
            HistoryManager.saveItem(
                this@ExplainOverlayActivity,
                HistoryItem(System.currentTimeMillis(), originalSelectedText, explanation, mode)
            )
        }
    }

    private suspend fun fetchExplanation(
        text: String,
        mode: String,
        onChunk: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://ai.hackclub.com/proxy/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.API_KEY}")
            conn.doOutput = true

            val modePrompt = when (mode) {
                "Detailed" -> "Give a very detailed, thorough analysis with deep context about this."
                "Straightforward" -> "Give a direct, clear, no-fluff summary of this."
                "ELI5" -> "Explain this like I am 5 years old. Use simple words and fun analogies."
                else -> "Explain this casually."
            }

            val fullPrompt = "$modePrompt\n\nText to explain:\n$text\n\nExplanation:"

            val payload = JSONObject().apply {
                put("model", "google/gemini-3-flash-preview")
                put("stream", true)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", fullPrompt)
                    })
                })
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
                        if (data == "[DONE]") {
                            break
                        }
                        try {
                            val json = JSONObject(data)
                            val delta = json.getJSONArray("choices").getJSONObject(0).getJSONObject("delta")
                            val content = delta.optString("content", "")
                            if (content.isNotEmpty()) {
                                fullResponse.append(content)
                                onChunk(fullResponse.toString())
                            }
                        } catch (_: Exception) {
                        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(50L)
            }
        }

        bottomSheetDialog = BottomSheetDialog(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgMainStr))
                val r = dp * 24f
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            setPadding((dp * 16).toInt(), (dp * 8).toInt(), (dp * 16).toInt(), (dp * 24).toInt())
        }

        val handleContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, (dp * 16).toInt())
        }
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor(handleColorStr))
                cornerRadius = 2 * dp
            }
            layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (4 * dp).toInt())
        }
        handleContainer.addView(handle)

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (dp * 16).toInt())
        }

        var titleText = originalSelectedText.replace("\n", " ")
        if (titleText.length > 25) {
            titleText = titleText.take(25) + "..."
        }

        val titleTv = TextView(this).apply {
            text = titleText
            setTextColor(textTitleColor)
            textSize = 18.0f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val copyBtn = TextView(this).apply {
            text = "📋 Copy"
            setTextColor(Color.parseColor(modeActiveBgStr))
            textSize = 14.0f
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("ELI5 Explanation", currentExplanationText))
                Toast.makeText(this@ExplainOverlayActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor(textFadedStr))
            textSize = 18.0f
            setPadding((dp * 8).toInt(), (dp * 8).toInt(), 0, (dp * 8).toInt())
            setOnClickListener {
                bottomSheetDialog.dismiss()
            }
        }

        headerRow.addView(titleTv)
        headerRow.addView(copyBtn)
        headerRow.addView(closeBtn)

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(modesRowBgStr))
                cornerRadius = 12 * dp
                setStroke((dp * 1.0f).toInt(), Color.parseColor(if (isDark) "#333333" else "#CCCCCC"))
            }
            setPadding((dp * 4).toInt(), (dp * 4).toInt(), (dp * 4).toInt(), (dp * 4).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (dp * 16).toInt()
            }
        }

        val modeViews = ArrayList<TextView>()
        val modes = listOf("ELI5", "Direct", "Detailed")
        for (modeName in modes) {
            val tv = TextView(this).apply {
                text = modeName
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(textFadedStr))
                textSize = 14.0f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, (dp * 8).toInt(), 0, (dp * 8).toInt())
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
                    btn.setTextColor(Color.parseColor(textFadedStr))
                    (btn.background as? GradientDrawable)?.setColor(Color.TRANSPARENT)
                }
                tv.setTextColor(textTitleColor)
                (tv.background as? GradientDrawable)?.setColor(Color.parseColor(modeActiveBgStr))

                val resolvedMode = if (modeName == "Direct") "Straightforward" else modeName
                requestExplanation(resolvedMode)
            }
        }

        modeViews[0].setTextColor(textTitleColor)
        (modeViews[0].background as? GradientDrawable)?.setColor(Color.parseColor(modeActiveBgStr))

        val contentBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgContentStr))
                cornerRadius = dp * 16
            }
            setPadding((dp * 16).toInt(), (dp * 16).toInt(), (dp * 16).toInt(), (dp * 16).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        bodyTextView = TextView(this).apply {
            setTextColor(Color.parseColor(textMainStr))
            textSize = 15.0f
            setLineSpacing(0.0f, 1.2f)
        }
        scrollView.addView(bodyTextView)
        contentBox.addView(scrollView)

        root.addView(handleContainer)
        root.addView(headerRow)
        root.addView(modeRow)
        root.addView(contentBox)

        bottomSheetDialog.setContentView(root)
        bottomSheetDialog.setOnDismissListener {
            finish()
        }
        bottomSheetDialog.setOnShowListener { dialog ->
            val sheetDialog = dialog as BottomSheetDialog
            val sheet = sheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.45).toInt()
            }
        }
        bottomSheetDialog.show()
    }
}

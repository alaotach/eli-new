package com.alaotach.eli5

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

class ProofreadTextActivity : Activity() {
    private lateinit var actionLayout: LinearLayout
    private lateinit var fullTextScrollView: ScrollView
    private lateinit var fullTextView: TextView
    private lateinit var input: EditText
    private var isReadOnly: Boolean = false
    private lateinit var popupContainer: LinearLayout
    private lateinit var previewContainer: LinearLayout
    private lateinit var promptLayout: LinearLayout
    private lateinit var submitBtn: TextView
    private lateinit var titleText: TextView
    private val activityScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var originalText: String = ""
    private var currentText: String = ""

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
        window.addFlags(131080)
        isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
        originalText = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""

        if (originalText.isBlank()) {
            Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            setupUI()
            submitPrompt("")
        }
    }

    private fun setupUI() {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        popupContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgMainStr))
                cornerRadius = dp * 8f
                setStroke((dp * 1.0f).toInt(), Color.parseColor(hintColor))
            }
            elevation = dp * 8f
            setOnClickListener { /* Consume clicks */ }
        }

        actionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((dp * 12).toInt(), (dp * 8).toInt(), (dp * 12).toInt(), (dp * 8).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                titleText.performClick()
            }
        }

        promptLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((dp * 12).toInt(), (dp * 12).toInt(), (dp * 12).toInt(), (dp * 12).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            visibility = View.GONE
        }

        titleText = TextView(this).apply {
            text = "✨ Proofreading..."
            setTextColor(textMainPrimary)
            textSize = 14.0f
            isClickable = true
            isFocusable = true
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            layoutParams = lp
            setPadding(0, (dp * 8).toInt(), (dp * 8).toInt(), (dp * 8).toInt())
            setOnClickListener {
                if (currentText.isBlank()) return@setOnClickListener
                if (submitBtn.text.toString() == "Wait...") return@setOnClickListener

                if (previewContainer.visibility == View.VISIBLE) {
                    previewContainer.visibility = View.GONE
                } else {
                    fullTextView.text = "Loading preview..."
                    previewContainer.visibility = View.VISIBLE
                    activityScope.launch(Dispatchers.Default) {
                        val spannable = generateDiffSpannable(originalText, currentText)
                        withContext(Dispatchers.Main) {
                            fullTextView.text = spannable
                            fullTextScrollView.scrollTo(0, 0)
                        }
                    }
                }
            }
        }

        val acceptBtn = createButton(
            text = if (isReadOnly) "Copy ↗" else "Accept ✓",
            bgColor = Color.parseColor(if (isReadOnly) "#F5A623" else modeActiveBgStr),
            isPrimary = true
        ) {
            if (isReadOnly) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Proofread Text", currentText))
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
            } else {
                val intent = Intent().apply {
                    putExtra(Intent.EXTRA_PROCESS_TEXT, currentText)
                }
                setResult(RESULT_OK, intent)
            }
            finish()
        }

        val adjustBtn = createButton(text = "Adjust", bgColor = 0, isPrimary = false) {
            window.clearFlags(131080)
            actionLayout.visibility = View.GONE
            promptLayout.visibility = View.VISIBLE
            previewContainer.visibility = View.GONE
            input.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        val cancelBtn = createButton(text = "Cancel", bgColor = 0, isPrimary = false) {
            setResult(RESULT_CANCELED)
            finish()
        }

        actionLayout.addView(titleText)
        actionLayout.addView(acceptBtn)
        actionLayout.addView(adjustBtn)
        actionLayout.addView(cancelBtn)

        input = EditText(this).apply {
            hint = "Ask AI to adjust..."
            setHintTextColor(0xFF888888.toInt())
            setTextColor(textMainPrimary)
            textSize = 14.0f
            background = null
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEND
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val str = text.toString()
                    if (str.isNotBlank()) {
                        submitPrompt(str)
                    }
                    true
                } else {
                    false
                }
            }
        }

        submitBtn = createButton(text = "Send ↑", bgColor = Color.parseColor(modeActiveBgStr), isPrimary = true) {
            val str = input.text.toString()
            if (str.isNotBlank()) {
                submitPrompt(str)
            }
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgContentStr))
                cornerRadius = dp * 8f
                setStroke((dp * 1.0f).toInt(), Color.parseColor(hintColor))
            }
            setPadding((dp * 12).toInt(), (dp * 4).toInt(), (dp * 4).toInt(), (dp * 4).toInt())
            addView(input)
            addView(submitBtn)
        }

        val suggestionLabel = TextView(this).apply {
            text = "suggestions"
            setTextColor(0xFF888888.toInt())
            textSize = 12.0f
            setPadding((dp * 4).toInt(), (dp * 12).toInt(), 0, (dp * 8).toInt())
        }

        val suggestionsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val suggestionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        suggestionsScroll.addView(suggestionsRow)

        val samplePrompts = listOf("make it more formal", "shorter", "translate to Hindi", "sound urgent", "fix grammar")
        for (chip in samplePrompts) {
            val chipView = TextView(this).apply {
                text = chip
                setTextColor(textMainFaded)
                textSize = 13.0f
                setPadding((dp * 12).toInt(), (dp * 6).toInt(), (dp * 12).toInt(), (dp * 6).toInt())
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = dp * 16f
                    setStroke((dp * 1.0f).toInt(), Color.parseColor(hintColor))
                }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = (dp * 8).toInt()
                }
                setOnClickListener {
                    input.setText(chip)
                    submitPrompt(chip)
                }
            }
            suggestionsRow.addView(chipView)
        }

        val backBtn = createButton(text = "← back", bgColor = 0, isPrimary = false) {
            promptLayout.visibility = View.GONE
            input.text.clear()
            actionLayout.visibility = View.VISIBLE
        }

        promptLayout.addView(inputRow)
        promptLayout.addView(suggestionLabel)
        promptLayout.addView(suggestionsScroll)
        promptLayout.addView(backBtn)

        fullTextScrollView = object : ScrollView(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val maxHeight = (resources.displayMetrics.heightPixels * 0.4).toInt()
                val size = MeasureSpec.getSize(heightMeasureSpec)
                val newSpec = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED || size > maxHeight) {
                    MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                } else {
                    heightMeasureSpec
                }
                super.onMeasure(widthMeasureSpec, newSpec)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            visibility = View.VISIBLE
        }

        fullTextView = TextView(this).apply {
            setTextColor(textMainFaded)
            textSize = 15.0f
            setLineSpacing(0.0f, 1.2f)
            setPadding((dp * 16).toInt(), (dp * 16).toInt(), (dp * 16).toInt(), 0)
            movementMethod = LinkMovementMethod.getInstance()
        }
        fullTextScrollView.addView(fullTextView)

        previewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgMainStr))
                cornerRadius = dp * 12f
                setStroke((dp * 1.0f).toInt(), Color.parseColor(hintColor))
            }
            elevation = dp * 8f
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (dp * 12).toInt()
            }
            visibility = View.GONE
            addView(fullTextScrollView)
        }

        popupContainer.addView(actionLayout)
        popupContainer.addView(promptLayout)

        val outerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = (dp * 80).toInt()
                leftMargin = (dp * 16).toInt()
                rightMargin = (dp * 16).toInt()
            }
            layoutParams = lp
            setOnClickListener { /* Consume clicks */ }
            addView(previewContainer)
            addView(popupContainer)
        }

        root.addView(outerContainer)
        setContentView(root)
    }

    private fun createButton(text: String, bgColor: Int, isPrimary: Boolean, onClick: () -> Unit): TextView {
        val btn = TextView(this).apply {
            this.text = text
            setTextColor(if (isPrimary) Color.WHITE else 0xFFCCCCCC.toInt())
            textSize = 14.0f
            gravity = Gravity.CENTER
            setPadding((dp * 16).toInt(), (dp * 8).toInt(), (dp * 16).toInt(), (dp * 8).toInt())
            background = GradientDrawable().apply {
                setColor(if (isPrimary) bgColor else Color.TRANSPARENT)
                cornerRadius = dp * 8f
                if (!isPrimary) {
                    setStroke((dp * 1.0f).toInt(), Color.parseColor(hintColor))
                }
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = (dp * 8).toInt()
            }
            setOnClickListener {
                performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        }
        return btn
    }

    private fun submitPrompt(instruction: String) {
        val isFirstTime = instruction.isEmpty()
        if (!isFirstTime) {
            input.isEnabled = false
            submitBtn.text = "Wait..."
        }

        actionLayout.visibility = View.VISIBLE
        promptLayout.visibility = View.GONE

        titleText.text = "█████████..."
        titleText.setTextColor(Color.parseColor(hintColor))
        val shimmerAnim = ObjectAnimator.ofFloat(titleText, "alpha", 0.3f, 1.0f).apply {
            duration = 600L
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
        shimmerAnim.start()

        (actionLayout.getChildAt(2) as? TextView)?.isEnabled = false
        (actionLayout.getChildAt(3) as? TextView)?.isEnabled = false

        activityScope.launch {
            var firstChunk = true
            val baseText = if (isFirstTime) originalText else currentText

            val newText = fetchRefinement(baseText, instruction) { partialText ->
                withContext(Dispatchers.Main) {
                    if (firstChunk) {
                        firstChunk = false
                        shimmerAnim.cancel()
                        titleText.alpha = 1.0f
                        titleText.setTextColor(textMainPrimary)
                    }
                    val preview = partialText.take(25).replace("\n", " ") + (if (partialText.length > 25) "..." else "")
                    titleText.text = "✨ \"$preview\""

                    if (previewContainer.visibility == View.VISIBLE) {
                        val span = generateDiffSpannable(originalText, partialText)
                        fullTextView.text = span
                    }
                }
            }

            withContext(Dispatchers.Main) {
                currentText = newText
                if (!isFirstTime) {
                    input.isEnabled = true
                    submitBtn.text = "Submit"
                    input.text.clear()
                }

                shimmerAnim.cancel()
                titleText.alpha = 1.0f
                titleText.setTextColor(textMainPrimary)

                val finalPreview = currentText.take(25).replace("\n", " ") + (if (currentText.length > 25) "..." else "")
                titleText.text = "✨ \"$finalPreview\""

                val tag = if (isFirstTime) "Proofread" else "Proofread ($instruction)"
                HistoryManager.saveItem(
                    this@ProofreadTextActivity,
                    HistoryItem(System.currentTimeMillis(), if (isFirstTime) originalText else currentText, newText, tag)
                )
            }

            (actionLayout.getChildAt(2) as? TextView)?.isEnabled = true
            (actionLayout.getChildAt(3) as? TextView)?.isEnabled = true
        }
    }

    private fun generateDiffSpannable(old: String, new: String, maxLimit: Int = 250): SpannableStringBuilder {
        val oldWords = old.split(" ")
        val newWords = new.split(" ")
        val truncated = oldWords.size > maxLimit || newWords.size > maxLimit

        val oldTaken = oldWords.take(maxLimit)
        val newTaken = newWords.take(maxLimit)
        val m = oldTaken.size
        val n = newTaken.size

        val dpTable = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) {
            for (j in 1..n) {
                if (oldTaken[i - 1] == newTaken[j - 1]) {
                    dpTable[i][j] = dpTable[i - 1][j - 1] + 1
                } else {
                    dpTable[i][j] = max(dpTable[i - 1][j], dpTable[i][j - 1])
                }
            }
        }

        val diff = ArrayList<Pair<Int, String>>()
        var i = m
        var j = n
        while (i > 0 && j > 0) {
            if (oldTaken[i - 1] == newTaken[j - 1]) {
                diff.add(Pair(0, oldTaken[i - 1]))
                i--
                j--
            } else if (dpTable[i - 1][j] > dpTable[i][j - 1]) {
                diff.add(Pair(2, oldTaken[i - 1]))
                i--
            } else {
                diff.add(Pair(1, newTaken[j - 1]))
                j--
            }
        }
        while (i > 0) {
            diff.add(Pair(2, oldTaken[i - 1]))
            i--
        }
        while (j > 0) {
            diff.add(Pair(1, newTaken[j - 1]))
            j--
        }
        diff.reverse()

        val sb = SpannableStringBuilder()
        val isDarkTheme = SettingsManager.isDarkTheme(this)
        val addBg = Color.parseColor(if (isDarkTheme) "#284B30" else "#DAFBE1")
        val addFg = Color.parseColor(if (isDarkTheme) "#56D364" else "#1A7F37")
        val delBg = Color.parseColor(if (isDarkTheme) "#4A292D" else "#FFEBE9")
        val delFg = Color.parseColor(if (isDarkTheme) "#F85149" else "#CF222E")

        var currentState = 0
        var spanStart = 0

        fun flushSpan() {
            if (spanStart < sb.length) {
                when (currentState) {
                    1 -> {
                        sb.setSpan(BackgroundColorSpan(addBg), spanStart, sb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.setSpan(ForegroundColorSpan(addFg), spanStart, sb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    2 -> {
                        sb.setSpan(BackgroundColorSpan(delBg), spanStart, sb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.setSpan(ForegroundColorSpan(delFg), spanStart, sb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.setSpan(StrikethroughSpan(), spanStart, sb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
            spanStart = sb.length
        }

        fun setState(newState: Int) {
            if (currentState != newState) {
                flushSpan()
                currentState = newState
            }
        }

        for (item in diff) {
            setState(item.first)
            sb.append(item.second).append(" ")
        }
        flushSpan()

        if (truncated) {
            setState(0)
            val linkStart = sb.length
            sb.append("\n\n... (Read full text)")
            val linkColor = Color.parseColor(modeActiveBgStr)
            sb.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    fullTextView.text = "Loading remainder... (This might freeze the screen momentarily while computing differences for the huge text)"
                    activityScope.launch(Dispatchers.Default) {
                        val fullSpan = generateDiffSpannable(old, new, Int.MAX_VALUE)
                        withContext(Dispatchers.Main) {
                            fullTextView.text = fullSpan
                        }
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = linkColor
                    ds.isUnderlineText = false
                }
            }, linkStart, sb.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return sb
    }

    private suspend fun fetchRefinement(
        baseText: String,
        customInstructions: String,
        onChunk: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://ai.hackclub.com/proxy/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.API_KEY}")
            conn.doOutput = true

            val prompt = if (customInstructions.isBlank()) {
                "Proofread and correct the following text. Do not use any em dashes (—). Keep the tone natural and human-like. Just provide the corrected text without any preamble or explanation:\n\n$baseText"
            } else {
                "Modify the following text based strictly on this instruction: \"$customInstructions\". Do not use any em dashes (—). Keep the tone natural and human-like. Just provide the corrected text without any preamble or explanation:\n\n$baseText"
            }

            val payload = JSONObject().apply {
                put("model", "google/gemini-3-flash-preview")
                put("stream", true)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
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
}

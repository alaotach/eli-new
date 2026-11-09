package com.alaotach.eli5

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
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
import android.view.ViewTreeObserver
import android.view.WindowManager
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import kotlin.math.min

class ProofreadTextActivity : Activity() {
    private lateinit var rootView: FrameLayout
    private lateinit var outerContainer: LinearLayout
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
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var lastImeHeight: Int = 0
    private var lastNavHeight: Int = 0

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
        get() = if (isDark) "#8B5CF6" else "#7C3AED"

    private val handleColorStr: String
        get() = if (isDark) "#444444" else "#CCCCCC"

    private val dp: Float
        get() = resources.displayMetrics.density

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsManager.getLanguage(newBase)
        val langObj = LanguageManager.getLanguageByEnglishName(lang)
        val locale = java.util.Locale.forLanguageTag(langObj.code)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED or
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isSendAction = intent?.action == Intent.ACTION_SEND
        isReadOnly = isSendAction || intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
        val textFromProcess = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        val textFromSend = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        originalText = textFromProcess ?: textFromSend ?: ""

        if (originalText.isBlank()) {
            Toast.makeText(this, getString(R.string.no_text_selected), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            setupUI()
            submitPrompt("")
        }
    }

    private fun setupUI() {
        rootView = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            updateKeyboardPosition(imeInsets.bottom, navInsets.bottom)
            insets
        }

        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                updateKeyboardPosition(keypadHeight, lastNavHeight)
            } else if (keypadHeight < (dp * 60).toInt()) {
                updateKeyboardPosition(0, lastNavHeight)
            }
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        globalLayoutListener = listener

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
            text = "✨ " + getString(R.string.proofread_title)
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
                if (submitBtn.text.toString() == getString(R.string.proofread_wait)) return@setOnClickListener

                if (previewContainer.visibility == View.VISIBLE) {
                    previewContainer.visibility = View.GONE
                } else {
                    fullTextView.text = getString(R.string.proofread_loading_preview)
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
            text = if (isReadOnly) getString(R.string.proofread_copy) else getString(R.string.proofread_accept),
            bgColor = Color.parseColor(modeActiveBgStr),
            isPrimary = true
        ) {
            if (isReadOnly) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Proofread Text", currentText))
                Toast.makeText(this, getString(R.string.proofread_copied_toast), Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
            } else {
                val intent = Intent().apply {
                    putExtra(Intent.EXTRA_PROCESS_TEXT, currentText)
                }
                setResult(RESULT_OK, intent)
            }
            finish()
        }

        val adjustBtn = createButton(text = getString(R.string.proofread_adjust), bgColor = 0, isPrimary = false) {
            actionLayout.visibility = View.GONE
            promptLayout.visibility = View.VISIBLE
            previewContainer.visibility = View.GONE
            input.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        val cancelBtn = createButton(text = getString(R.string.proofread_cancel), bgColor = 0, isPrimary = false) {
            setResult(RESULT_CANCELED)
            finish()
        }

        actionLayout.addView(titleText)
        actionLayout.addView(acceptBtn)
        actionLayout.addView(adjustBtn)
        actionLayout.addView(cancelBtn)

        input = EditText(this).apply {
            hint = getString(R.string.proofread_hint_ask_ai)
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

        submitBtn = createButton(text = getString(R.string.proofread_send), bgColor = Color.parseColor(modeActiveBgStr), isPrimary = true) {
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
            text = getString(R.string.proofread_suggestions)
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

        // 1. Tone Chips
        val toneChips = listOf(
            "Formal" to "👔 " + getString(R.string.proofread_chip_formal),
            "Casual" to "☕ " + getString(R.string.proofread_chip_casual),
            "Urgent" to "⚡ " + getString(R.string.proofread_chip_urgent),
            "Concise" to "✂️ " + getString(R.string.proofread_chip_concise)
        )
        for ((toneKey, chipLabel) in toneChips) {
            val chipView = TextView(this).apply {
                text = chipLabel
                setTextColor(if (isDark) Color.parseColor("#DDD6FE") else Color.parseColor("#6D28D9"))
                textSize = 13.0f
                setTypeface(typeface, Typeface.BOLD)
                setPadding((dp * 14).toInt(), (dp * 6).toInt(), (dp * 14).toInt(), (dp * 6).toInt())
                background = GradientDrawable().apply {
                    setColor(if (isDark) Color.parseColor("#261F36") else Color.parseColor("#EDE9FE"))
                    cornerRadius = dp * 14f
                    setStroke((dp * 1.0f).toInt(), Color.argb((255 * 0.35f).toInt(), 139, 92, 246))
                }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = (dp * 8).toInt()
                }
                setOnClickListener {
                    val prompt = when (toneKey) {
                        "Formal" -> "Make it formal, polished, and professional. Preserve the original language of the text."
                        "Casual" -> "Make it casual, friendly, and natural. Preserve the original language of the text."
                        "Urgent" -> "Make it sound urgent, decisive, and time-sensitive. Preserve the original language of the text."
                        "Concise" -> "Make it concise, crisp, and to the point. Preserve the original language of the text."
                        else -> "Make it $toneKey. Preserve the original language of the text."
                    }
                    input.setText(prompt)
                    submitPrompt(prompt)
                }
            }
            suggestionsRow.addView(chipView)
        }

        // 2. Language Quick Chips
        val prefLangName = SettingsManager.getLanguage(this)
        val prefLang = LanguageManager.getLanguageByEnglishName(prefLangName)
        val langChips = mutableListOf<Pair<String, String>>()
        if (!prefLang.code.equals("en", ignoreCase = true)) {
            langChips.add("${prefLang.flag} To ${prefLang.englishName}" to prefLang.englishName)
            langChips.add("🇺🇸 To English" to "English")
        } else {
            langChips.add("🇪🇸 To Spanish" to "Spanish")
            langChips.add("🇮🇳 To Hindi" to "Hindi")
            langChips.add("🇫🇷 To French" to "French")
        }

        for ((chipLabel, targetLang) in langChips) {
            val chipView = TextView(this).apply {
                text = chipLabel
                setTextColor(if (isDark) Color.parseColor("#93C5FD") else Color.parseColor("#1D4ED8"))
                textSize = 13.0f
                setTypeface(typeface, Typeface.BOLD)
                setPadding((dp * 14).toInt(), (dp * 6).toInt(), (dp * 14).toInt(), (dp * 6).toInt())
                background = GradientDrawable().apply {
                    setColor(if (isDark) Color.parseColor("#1E293B") else Color.parseColor("#EFF6FF"))
                    cornerRadius = dp * 14f
                    setStroke((dp * 1.0f).toInt(), Color.argb((255 * 0.35f).toInt(), 59, 130, 246))
                }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    rightMargin = (dp * 8).toInt()
                }
                setOnClickListener {
                    val prompt = "Translate to $targetLang and polish naturally"
                    input.setText(prompt)
                    submitPrompt(prompt, targetLanguage = targetLang)
                }
            }
            suggestionsRow.addView(chipView)
        }

        // 3. More Languages Selector Chip (All 35 supported languages)
        val moreLangChip = TextView(this).apply {
            text = getString(R.string.proofread_more_languages)
            setTextColor(if (isDark) Color.parseColor("#FDE047") else Color.parseColor("#B45309"))
            textSize = 13.0f
            setTypeface(typeface, Typeface.BOLD)
            setPadding((dp * 14).toInt(), (dp * 6).toInt(), (dp * 14).toInt(), (dp * 6).toInt())
            background = GradientDrawable().apply {
                setColor(if (isDark) Color.parseColor("#292524") else Color.parseColor("#FEF3C7"))
                cornerRadius = dp * 14f
                setStroke((dp * 1.0f).toInt(), Color.argb((255 * 0.35f).toInt(), 245, 158, 11))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = (dp * 8).toInt()
            }
            setOnClickListener {
                showLanguagePickerDialog()
            }
        }
        suggestionsRow.addView(moreLangChip)

        val backBtn = createButton(text = getString(R.string.proofread_back), bgColor = 0, isPrimary = false) {
            promptLayout.visibility = View.GONE
            input.text.clear()
            actionLayout.visibility = View.VISIBLE
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
        }

        promptLayout.addView(inputRow)
        promptLayout.addView(suggestionLabel)
        promptLayout.addView(suggestionsScroll)
        promptLayout.addView(backBtn)

        fullTextScrollView = object : ScrollView(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val screenHeight = resources.displayMetrics.heightPixels
                val availableAboveKeyboard = if (lastImeHeight > (dp * 80).toInt()) {
                    screenHeight - lastImeHeight - (dp * 160).toInt()
                } else {
                    (screenHeight * 0.4).toInt()
                }
                val maxHeight = max((dp * 120).toInt(), min((screenHeight * 0.4).toInt(), availableAboveKeyboard))
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

        outerContainer = LinearLayout(this).apply {
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

        rootView.addView(outerContainer)
        setContentView(rootView)
    }

    private fun updateKeyboardPosition(imeHeight: Int, navHeight: Int) {
        if (imeHeight > 0) {
            lastImeHeight = imeHeight
        } else if (imeHeight == 0) {
            lastImeHeight = 0
        }
        if (navHeight > 0) {
            lastNavHeight = navHeight
        }

        if (!::outerContainer.isInitialized) return
        val lp = outerContainer.layoutParams as? FrameLayout.LayoutParams ?: return
        val baseBottomMargin = (dp * 80).toInt()
        val minNavMargin = lastNavHeight + (dp * 16).toInt()

        val targetBottomMargin = if (lastImeHeight > (dp * 80).toInt()) {
            lastImeHeight + (dp * 16).toInt()
        } else {
            max(baseBottomMargin, minNavMargin)
        }

        if (lp.bottomMargin != targetBottomMargin) {
            lp.bottomMargin = targetBottomMargin
            outerContainer.layoutParams = lp
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        globalLayoutListener?.let {
            if (::rootView.isInitialized) {
                rootView.viewTreeObserver.removeOnGlobalLayoutListener(it)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::promptLayout.isInitialized && promptLayout.visibility == View.VISIBLE) {
            promptLayout.visibility = View.GONE
            input.text.clear()
            actionLayout.visibility = View.VISIBLE
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(input.windowToken, 0)
        } else {
            super.onBackPressed()
        }
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

    private fun showLanguagePickerDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((dp * 20).toInt(), (dp * 16).toInt(), (dp * 20).toInt(), (dp * 16).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgMainStr))
                cornerRadius = dp * 16f
            }
        }

        val title = TextView(this).apply {
            text = getString(R.string.proofread_choose_target_lang)
            setTextColor(textMainPrimary)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, (dp * 12).toInt())
        }
        dialogView.addView(title)

        val searchInput = EditText(this).apply {
            hint = getString(R.string.picker_search_hint)
            setHintTextColor(0xFF888888.toInt())
            setTextColor(textMainPrimary)
            textSize = 14f
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgContentStr))
                cornerRadius = dp * 8f
                setStroke((dp * 1f).toInt(), Color.parseColor(hintColor))
            }
            setPadding((dp * 12).toInt(), (dp * 8).toInt(), (dp * 12).toInt(), (dp * 8).toInt())
        }
        dialogView.addView(searchInput)

        val listScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (dp * 320).toInt()).apply {
                topMargin = (dp * 12).toInt()
            }
        }
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        listScroll.addView(listContainer)
        dialogView.addView(listScroll)

        var dialog: android.app.AlertDialog? = null

        fun populateList(query: String) {
            listContainer.removeAllViews()
            val filtered = LanguageManager.searchLanguages(query)
            for (lang in filtered) {
                val row = TextView(this).apply {
                    text = "${lang.flag}  ${lang.displayName}"
                    setTextColor(textMainPrimary)
                    textSize = 14.5f
                    setPadding((dp * 12).toInt(), (dp * 12).toInt(), (dp * 12).toInt(), (dp * 12).toInt())
                    background = GradientDrawable().apply {
                        cornerRadius = dp * 8f
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        dialog?.dismiss()
                        val prompt = "Translate to ${lang.englishName} and polish naturally"
                        input.setText(prompt)
                        submitPrompt(prompt, targetLanguage = lang.englishName)
                    }
                }
                listContainer.addView(row)
            }
        }

        populateList("")

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                populateList(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun submitPrompt(instruction: String, targetLanguage: String? = null) {
        val isFirstTime = instruction.isEmpty()
        if (!isFirstTime) {
            input.isEnabled = false
            submitBtn.text = getString(R.string.proofread_wait)
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

            val defaultTone = SettingsManager.getProofreadDefaultTone(this@ProofreadTextActivity)
            val effectiveInstruction = if (isFirstTime) {
                val toneDesc = when (defaultTone.lowercase()) {
                    "formal" -> "Make it formal, polished, and professional."
                    "urgent" -> "Make it concise, urgent, and time-sensitive."
                    else -> "Make it clear, natural, and friendly."
                }
                """
                Fix all grammar, spelling, typos, punctuation, and wording. $toneDesc
                CRITICAL LANGUAGE RULE:
                - Detect the language of the source text.
                - Preserve and output in the EXACT SAME LANGUAGE as the source text (e.g. if the input is in Spanish, output in Spanish; if in Hindi/Hinglish, output in Hindi/Hinglish; if in French, output in French; if in German, output in German; if in Japanese, output in Japanese).
                - Never translate to English unless explicitly requested.
                - Output ONLY the proofread text, with no preamble, markdown wrapping, or explanations.
                """.trimIndent()
            } else {
                instruction
            }

            AnalyticsManager.logExplainRequest(
                this@ProofreadTextActivity,
                if (isFirstTime) "proofread_initial" else "proofread_adjust",
                targetLanguage ?: "auto",
                false,
                baseText.length
            )

            val newText = fetchRefinement(baseText, effectiveInstruction, targetLanguage) { partialText ->
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
                    submitBtn.text = getString(R.string.proofread_send)
                    input.text.clear()
                }

                shimmerAnim.cancel()
                titleText.alpha = 1.0f
                titleText.setTextColor(textMainPrimary)

                val finalPreview = currentText.take(25).replace("\n", " ") + (if (currentText.length > 25) "..." else "")
                titleText.text = "✨ \"$finalPreview\""

                AnalyticsManager.logExplainSuccess(
                    this@ProofreadTextActivity,
                    if (isFirstTime) "proofread_initial" else "proofread_adjust",
                    targetLanguage ?: "auto",
                    false
                )

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
        targetLanguage: String? = null,
        onChunk: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${BuildConfig.BACKEND_URL}/api/proofread"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 ELI5-Android/2.2.1")
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("text", baseText)
                put("instructions", customInstructions)
                if (!targetLanguage.isNullOrEmpty()) {
                    put("language", targetLanguage)
                }
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

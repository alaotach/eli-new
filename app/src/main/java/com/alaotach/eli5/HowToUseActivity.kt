package com.alaotach.eli5

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

class DotsView(context: Context) : View(context) {
    private var activeIndex = 0
    private val dotCount = 4
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spacing = dpToPx(12.0f)
    private val activeRadius = dpToPx(4.0f)
    private val inactiveRadius = dpToPx(2.5f)

    fun updateActive(index: Int) {
        activeIndex = index
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val f = 2f
        val activeDiameter = activeRadius * f
        val totalWidth = (activeDiameter + ((dotCount - 1) * (inactiveRadius * f))) + ((dotCount - 1) * spacing)
        var startX = (width - totalWidth) / 2.0f
        val centerY = height / 2.0f

        for (i in 0 until dotCount) {
            val r = if (i == activeIndex) activeRadius else inactiveRadius
            paint.color = Color.parseColor(if (i == activeIndex) "#007ACC" else "#2A2A2A")
            canvas.drawCircle(startX + r, centerY, r, paint)
            startX += (r * f) + spacing
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}

class HowToUseActivity : AppCompatActivity() {
    private val colorBg = Color.parseColor("#0D0D0D")
    private val colorCard = Color.parseColor("#1E1E1E")
    private val colorCard2 = Color.parseColor("#252525")
    private val colorBlue = Color.parseColor("#007ACC")
    private val colorBlueL = Color.parseColor("#7EC8FF")
    private val colorWhite = Color.parseColor("#FFFFFF")
    private val colorMuted = Color.parseColor("#888888")
    private val colorBorder = Color.parseColor("#2A2A2A")
    private val colorRed = Color.parseColor("#FF5555")

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var dotsView: DotsView
    private lateinit var backBtn: TextView
    private lateinit var nextBtn: TextView
    private var currentStep = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val root = ConstraintLayout(this).apply {
            setBackgroundColor(colorBg)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        dotsView = DotsView(this).apply {
            id = View.generateViewId()
        }
        val dotsParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24)).apply {
            topToTop = 0
        }
        root.addView(dotsView, dotsParams)

        val navLayout = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            weightSum = 2.0f
        }

        backBtn = TextView(this).apply {
            text = "Back"
            textSize = 16.0f
            setTextColor(colorWhite)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1.5f), colorBorder)
                cornerRadius = dp(12.0f).toFloat()
            }
            visibility = View.INVISIBLE
            setOnClickListener { handleBack() }
        }

        nextBtn = TextView(this).apply {
            text = "Next →"
            textSize = 16.0f
            setTextColor(colorWhite)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(colorBlue)
                cornerRadius = dp(12.0f).toFloat()
            }
            setOnClickListener { handleNext() }
        }

        val backParams = LinearLayout.LayoutParams(0, dp(48), 1.0f).apply {
            marginEnd = dp(12)
        }
        val nextParams = LinearLayout.LayoutParams(0, dp(48), 1.0f)
        navLayout.addView(backBtn, backParams)
        navLayout.addView(nextBtn, nextParams)

        val navParams = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomToBottom = 0
            leftToLeft = 0
            rightToRight = 0
            marginStart = dp(24)
            marginEnd = dp(24)
        }
        root.addView(navLayout, navParams)

        viewFlipper = ViewFlipper(this).apply {
            id = View.generateViewId()
        }
        val flipperParams = ConstraintLayout.LayoutParams(0, 0).apply {
            topToBottom = dotsView.id
            bottomToTop = navLayout.id
            leftToLeft = 0
            rightToRight = 0
            topMargin = dp(16)
            bottomMargin = dp(24)
        }
        viewFlipper.addView(createStep1())
        viewFlipper.addView(createStep2())
        viewFlipper.addView(createStep3())
        viewFlipper.addView(createStep4())
        root.addView(viewFlipper, flipperParams)

        setContentView(root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            dotsParams.topMargin = sysBars.top + dp(16)
            navParams.bottomMargin = sysBars.bottom + dp(24)
            dotsView.layoutParams = dotsParams
            navLayout.layoutParams = navParams
            insets
        }
    }

    private fun handleNext() {
        if (currentStep < 3) {
            viewFlipper.setInAnimation(this, android.R.anim.slide_in_left)
            viewFlipper.setOutAnimation(this, android.R.anim.slide_out_right)
            viewFlipper.showNext()
            currentStep++
            updateNavState()
        } else {
            finish()
        }
    }

    private fun handleBack() {
        if (currentStep > 0) {
            viewFlipper.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            viewFlipper.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
            viewFlipper.showPrevious()
            currentStep--
            updateNavState()
        }
    }

    private fun updateNavState() {
        dotsView.updateActive(currentStep)
        backBtn.visibility = if (currentStep == 0) View.INVISIBLE else View.VISIBLE
        nextBtn.text = if (currentStep == 3) "Done ✓" else "Next →"
    }

    private fun createStep1(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), 0, dp(24), dp(24))
        }

        val phoneFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1.5f), Color.parseColor("#222222"))
                cornerRadius = dp(20.0f).toFloat()
            }
        }
        val phoneParams = LinearLayout.LayoutParams(dp(180), dp(320)).apply {
            bottomMargin = dp(32)
        }

        val phoneInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val notch = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#080808"))
                val r = dp(18.0f).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40))
        }

        val bubbleText = TextView(this).apply {
            text = "Supercalifragilistic is crazy!"
            setTextColor(colorWhite)
            textSize = 10.0f
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                setColor(colorCard)
                cornerRadius = dp(8.0f).toFloat()
            }
            val lp = LinearLayout.LayoutParams(dp(130), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(24)
                marginStart = dp(12)
            }
            layoutParams = lp
        }

        val actionMenu = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(colorCard2)
                cornerRadius = dp(4.0f).toFloat()
            }
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            layoutParams = lp
        }

        val actions = arrayOf("Cut", "Copy", "Paste")
        for (item in actions) {
            val tv = TextView(this).apply {
                text = item
                textSize = 8.0f
                setTextColor(colorWhite)
                setPadding(dp(4), 0, dp(4), 0)
            }
            actionMenu.addView(tv)
        }

        val moreTv = TextView(this).apply {
            text = "More ⋯"
            textSize = 8.0f
            setTextColor(colorWhite)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), 0, dp(4), 0)
        }
        actionMenu.addView(moreTv)

        val blinkAnim = ObjectAnimator.ofFloat(moreTv, "alpha", 1.0f, 0.5f, 1.0f).apply {
            duration = 1200L
            repeatCount = ObjectAnimator.INFINITE
        }
        blinkAnim.start()

        phoneInner.addView(notch)
        phoneInner.addView(bubbleText)
        phoneInner.addView(actionMenu)
        phoneFrame.addView(phoneInner)
        content.addView(phoneFrame, phoneParams)

        content.addView(stepItem("1", "Select any text", "Long press on a word or phrase you don't understand."))
        content.addView(stepItem("2", "Open Context Menu", "Wait for the copy/paste popup to appear above the text."))
        content.addView(stepItem("3", "Tap More", "If you don't see ELI5 immediately, tap the three dots (More)."))

        scroll.addView(content)
        return scroll
    }

    private fun createStep2(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), 0, dp(24), dp(24))
        }

        val phoneFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setStroke(dp(1.5f), Color.parseColor("#222222"))
                cornerRadius = dp(20.0f).toFloat()
            }
            clipToOutline = true
        }
        val phoneParams = LinearLayout.LayoutParams(dp(180), dp(320)).apply {
            bottomMargin = dp(32)
        }

        val dimView = View(this).apply {
            setBackgroundColor(Color.parseColor("#4D000000"))
        }
        phoneFrame.addView(dimView)

        val menuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2A2A2A"))
                cornerRadius = dp(10.0f).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(40)
            }
        }

        val items = listOf("Read aloud", "Explain with ELI5", "Proofread (ELI5)")
        for (item in items) {
            val tv = TextView(this).apply {
                text = item
                textSize = 9.0f
                setPadding(dp(12), dp(8), dp(12), dp(8))
                if (item == "Explain with ELI5") {
                    setBackgroundColor(colorBlue)
                    setTextColor(colorWhite)
                    typeface = Typeface.DEFAULT_BOLD
                } else {
                    setTextColor(Color.parseColor("#DDDDDD"))
                }
            }
            menuLayout.addView(tv)
        }
        phoneFrame.addView(menuLayout)

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(colorCard)
                val r = dp(16.0f).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)).apply {
                gravity = Gravity.BOTTOM
            }
        }

        val handle = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#444444"))
                cornerRadius = dp(1.5f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(3)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(10)
            }
        }

        val wordTv = TextView(this).apply {
            text = "Supercalifragilistic..."
            textSize = 8.0f
            setTextColor(colorMuted)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }

        val pillRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val pill = TextView(this).apply {
            text = "ELI5"
            textSize = 8.0f
            setTextColor(colorWhite)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                setColor(colorBlue)
                cornerRadius = dp(20.0f).toFloat()
            }
        }
        pillRow.addView(pill)

        val expTv = TextView(this).apply {
            val s = SpannableString("Imagine you just ate the world's best chocolate cupcake...")
            s.setSpan(ForegroundColorSpan(colorBlueL), 0, 7, 0)
            text = s
            textSize = 9.0f
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(colorCard2)
                cornerRadius = dp(10.0f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f).apply {
                setMargins(dp(12), dp(8), dp(12), dp(12))
            }
        }

        sheet.addView(handle)
        sheet.addView(wordTv)
        sheet.addView(pillRow)
        sheet.addView(expTv)
        phoneFrame.addView(sheet)
        content.addView(phoneFrame, phoneParams)

        content.addView(stepItem("1", "Choose an Action", "Tap on 'Explain with ELI5' to launch the bottom sheet."))
        content.addView(stepItem("2", "Read the Results", "The card will slide up over your current app without switching screens!"))

        sheet.translationY = dp(180).toFloat()
        val sheetAnim = ObjectAnimator.ofFloat(sheet, "translationY", dp(180).toFloat(), 0.0f).apply {
            duration = 400L
            interpolator = DecelerateInterpolator()
        }

        viewFlipper.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (viewFlipper.displayedChild == 1) {
                Handler(Looper.getMainLooper()).postDelayed({
                    sheetAnim.start()
                }, 600L)
            }
        }

        scroll.addView(content)
        return scroll
    }

    private fun createStep3(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), 0, dp(24), dp(24))
        }

        val phoneFrame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setStroke(dp(1.5f), Color.parseColor("#222222"))
                cornerRadius = dp(20.0f).toFloat()
            }
            clipToOutline = true
        }
        val phoneParams = LinearLayout.LayoutParams(dp(180), dp(320)).apply {
            bottomMargin = dp(32)
        }

        val origText = TextView(this).apply {
            val s = SpannableString("Hie could I order a little chip packs!?")
            s.setSpan(ForegroundColorSpan(colorRed), 0, 3, 0)
            s.setSpan(UnderlineSpan(), 0, 3, 0)
            text = s
            textSize = 9.0f
            setTextColor(colorWhite)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A"))
                cornerRadius = dp(18.0f).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM
                setMargins(dp(12), 0, dp(12), dp(12))
            }
        }

        val proofCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(colorCard)
                cornerRadius = dp(10.0f).toFloat()
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = FrameLayout.LayoutParams(dp(156), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = dp(50)
            }
            alpha = 0.0f
            translationY = dp(60).toFloat()
        }

        val refText = TextView(this).apply {
            text = "✨ Hi, could I order some sm..."
            textSize = 9.0f
            setTextColor(Color.parseColor("#DDDDDD"))
            setPadding(0, 0, 0, dp(8))
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2.0f
        }

        val acceptTv = TextView(this).apply {
            text = "Accept ✓"
            textSize = 8.0f
            setTextColor(colorWhite)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            background = GradientDrawable().apply {
                setColor(colorBlue)
                cornerRadius = dp(14.0f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                marginEnd = dp(4)
            }
        }

        val adjustTv = TextView(this).apply {
            text = "Adjust"
            textSize = 8.0f
            setTextColor(colorMuted)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
            background = GradientDrawable().apply {
                setStroke(dp(1.0f), Color.parseColor("#3A3A3A"))
                cornerRadius = dp(14.0f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        btnRow.addView(acceptTv)
        btnRow.addView(adjustTv)

        proofCard.addView(refText)
        proofCard.addView(btnRow)

        phoneFrame.addView(proofCard)
        phoneFrame.addView(origText)
        content.addView(phoneFrame, phoneParams)

        content.addView(stepItem("1", "Select Your Draft", "Type your message, then select it entirely."))
        content.addView(stepItem("2", "Proofread", "Choose 'Proofread (ELI5)' from the context menu."))
        content.addView(stepItem("3", "Accept or Adjust", "If you like the AI suggestion, click Accept to automatically replace your text!"))

        val animY = ObjectAnimator.ofFloat(proofCard, "translationY", dp(60).toFloat(), 0.0f).apply {
            duration = 350L
            interpolator = DecelerateInterpolator()
        }
        val animA = ObjectAnimator.ofFloat(proofCard, "alpha", 0.0f, 1.0f).apply {
            duration = 350L
        }

        viewFlipper.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (viewFlipper.displayedChild == 2) {
                Handler(Looper.getMainLooper()).postDelayed({
                    animY.start()
                    animA.start()
                }, 600L)
            }
        }

        scroll.addView(content)
        return scroll
    }

    private fun createStep4(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), 0, dp(24), dp(24))
        }

        content.addView(createStep4TipCard(this, "Permission required", "ELI5 needs Display over other apps so we can overlay the explanation while you stay inside your current app window.", "Display over other apps"))
        content.addView(createStep4TipCard(this, "Can't find ELI5 in the menu?", "Sometimes menus are too wide. Look for the three dots or 'More' button to expand your context menu options.", "'More' button"))
        content.addView(createStep4TipCard(this, "Proofread entire messages", "To quickly proofread long paragraphs, just double tap and choose Select All first.", "Select All"))
        content.addView(createStep4TipCard(this, "Use Adjust for translations", "The Adjust button lets you ask follow-up questions or even ask the bot to translate the text to another language.", "Adjust button"))

        val finishCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(colorCard)
                cornerRadius = dp(16.0f).toFloat()
            }
            setPadding(dp(24), dp(32), dp(24), dp(32))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(24)
            }
        }

        val allSetTv = TextView(this).apply {
            text = "You're all set"
            textSize = 20.0f
            setTextColor(colorWhite)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        finishCard.addView(allSetTv)

        val enjoyTv = TextView(this).apply {
            text = "Enjoy understanding the world effortlessly."
            textSize = 14.0f
            setTextColor(colorMuted)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(24))
        }
        finishCard.addView(enjoyTv)

        val startBtn = TextView(this).apply {
            text = "Start using ELI5"
            textSize = 14.0f
            setTextColor(colorWhite)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(colorBlue)
                cornerRadius = dp(40.0f).toFloat()
            }
            setPadding(dp(32), dp(12), dp(32), dp(12))
            setOnClickListener { finish() }
        }
        finishCard.addView(startBtn)

        content.addView(finishCard)
        scroll.addView(content)
        return scroll
    }

    private fun createStep4TipCard(activity: HowToUseActivity, title: String, subtitle: String, highlight: String = ""): View {
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(activity.colorCard)
                cornerRadius = activity.dp(12.0f).toFloat()
            }
            setPadding(activity.dp(16), activity.dp(14), activity.dp(16), activity.dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = activity.dp(16)
            }
        }

        val titleTv = TextView(activity).apply {
            text = title
            textSize = 14.0f
            setTextColor(activity.colorWhite)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, activity.dp(4))
        }
        card.addView(titleTv)

        val subTv = TextView(activity).apply {
            if (highlight.isNotEmpty() && subtitle.contains(highlight)) {
                val idx = subtitle.indexOf(highlight)
                val ss = SpannableString(subtitle)
                ss.setSpan(ForegroundColorSpan(activity.colorBlueL), idx, idx + highlight.length, 0)
                text = ss
            } else {
                text = subtitle
            }
            textSize = 13.0f
            setTextColor(activity.colorMuted)
            setLineSpacing(0.0f, 1.5f)
        }
        card.addView(subTv)

        return card
    }

    private fun stepItem(num: String, title: String, subtitle: String): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(16)
            }
        }

        val badge = TextView(this).apply {
            text = num
            textSize = 12.0f
            setTextColor(colorWhite)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(colorBlue)
                shape = GradientDrawable.OVAL
            }
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                marginEnd = dp(16)
            }
        }

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleTv = TextView(this).apply {
            text = title
            textSize = 15.0f
            setTextColor(colorWhite)
            typeface = Typeface.DEFAULT_BOLD
        }

        val subTv = TextView(this).apply {
            text = subtitle
            textSize = 13.0f
            setTextColor(colorMuted)
        }

        textCol.addView(titleTv)
        textCol.addView(subTv)

        root.addView(badge)
        root.addView(textCol)
        return root
    }

    private fun dp(f: Float): Int {
        return (f * resources.displayMetrics.density).roundToInt()
    }

    private fun dp(i: Int): Int {
        return (i * resources.displayMetrics.density).roundToInt()
    }
}

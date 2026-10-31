package com.alaotach.eli5

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

val COLOR_BG: Int = Color.parseColor("#0D0D0D")
val COLOR_SURFACE: Int = Color.parseColor("#1E1E1E")
val COLOR_PRIMARY: Int = Color.parseColor("#007ACC")
val COLOR_TEXT_PRIMARY: Int = Color.parseColor("#FFFFFF")
val COLOR_TEXT_SEC: Int = Color.parseColor("#888888")
val COLOR_BORDER: Int = Color.parseColor("#2A2A2A")
val COLOR_RED: Int = Color.parseColor("#FF5555")

fun Context.dpToPx(dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}

class BubbleView(context: Context) : View(context) {
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val pRed: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4848")
        textSize = context.dpToPx(45.0f).toFloat()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val pBlack: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        textSize = context.dpToPx(70.0f).toFloat()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val path: Path = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val bubbleBottom = h - context.dpToPx(30.0f)
        val radius = context.dpToPx(24.0f).toFloat()

        canvas.drawRoundRect(0.0f, 0.0f, w, bubbleBottom, radius, radius, paint)

        path.reset()
        path.moveTo(context.dpToPx(30.0f).toFloat(), bubbleBottom)
        path.lineTo(context.dpToPx(30.0f).toFloat(), bubbleBottom + context.dpToPx(30.0f))
        path.lineTo(context.dpToPx(60.0f).toFloat(), bubbleBottom)
        path.close()
        canvas.drawPath(path, paint)

        val centerY = (bubbleBottom / 2.0f) - ((pBlack.descent() + pBlack.ascent()) / 2f)
        val centerX = w / 2.0f
        val eliX = centerX - context.dpToPx(30.0f)
        canvas.drawText("ELI", eliX, centerY - context.dpToPx(8.0f), pRed)
        canvas.drawText("5", centerX + context.dpToPx(40.0f), centerY, pBlack)
    }
}

class DotsIndicatorView(context: Context) : View(context) {
    var activePage: Int = 0
        set(value) {
            field = value
            invalidate()
        }
    var totalPages: Int = 4

    private val paintActive: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PRIMARY
    }
    private val paintInactive: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_BORDER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2.0f
        val spacing = context.dpToPx(20.0f).toFloat()
        val activeRadius = context.dpToPx(8.0f).toFloat()
        val inactiveRadius = context.dpToPx(5.0f).toFloat()

        var totalWidth = 0.0f
        for (i in 0 until totalPages) {
            val r = if (i == activePage || activePage >= totalPages - 1) activeRadius else inactiveRadius
            totalWidth += 2 * r
            if (i < totalPages - 1) {
                totalWidth += spacing
            }
        }

        var startX = (width - totalWidth) / 2.0f
        for (i in 0 until totalPages) {
            val isActive = (i == activePage || activePage >= totalPages - 1)
            val r = if (isActive) activeRadius else inactiveRadius
            val dotCenter = startX + r
            canvas.drawCircle(dotCenter, centerY, r, if (isActive) paintActive else paintInactive)
            startX = dotCenter + r + spacing
        }
    }
}

class ShieldView(context: Context) : View(context) {
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_PRIMARY
    }
    private val checkPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = context.dpToPx(6.0f).toFloat()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path: Path = Path()
    private val checkPath: Path = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2.0f

        path.reset()
        path.moveTo(centerX, 0.0f)
        val shoulderY = 0.2f * h
        path.lineTo(w, shoulderY)
        val flankY = 0.6f * h
        path.lineTo(w, flankY)
        val bottomCurveY = 0.8f * h
        path.quadTo(w, bottomCurveY, centerX, h)
        path.quadTo(0.0f, bottomCurveY, 0.0f, flankY)
        path.lineTo(0.0f, shoulderY)
        path.close()
        canvas.drawPath(path, paint)

        checkPath.reset()
        checkPath.moveTo(0.3f * w, 0.5f * h)
        checkPath.lineTo(0.45f * w, 0.65f * h)
        checkPath.lineTo(w * 0.7f, h * 0.35f)
        canvas.drawPath(checkPath, checkPaint)
    }
}

class Step1Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = FrameLayout(context).apply {
            setBackgroundColor(COLOR_BG)
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpToPx(32.0f), context.dpToPx(130.0f), context.dpToPx(32.0f), 0)
        }

        val bubbleView = BubbleView(context)
        val bubbleParams = LinearLayout.LayoutParams(context.dpToPx(180.0f), context.dpToPx(140.0f)).apply {
            bottomMargin = context.dpToPx(48.0f)
        }
        content.addView(bubbleView, bubbleParams)

        val title = TextView(context).apply {
            text = "Understand anything.\nInstantly."
            textSize = 32.0f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val titleParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(16.0f)
        }
        content.addView(title, titleParams)

        val sub = TextView(context).apply {
            text = "Highlight any word in any app.\nWe explain it like you're 5."
            textSize = 16.0f
            setTextColor(COLOR_TEXT_SEC)
            gravity = Gravity.CENTER
            setLineSpacing(0.0f, 1.2f)
        }
        content.addView(sub, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val skip = TextView(context).apply {
            text = "Skip"
            setTextColor(COLOR_TEXT_SEC)
            textSize = 15.0f
            setPadding(context.dpToPx(16.0f), context.dpToPx(48.0f), context.dpToPx(24.0f), context.dpToPx(16.0f))
            setOnClickListener {
                context.getSharedPreferences("eli5_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("onboarding_done", true)
                    .apply()
                startActivity(Intent(context, MainActivity::class.java))
                requireActivity().finish()
            }
        }
        root.addView(skip, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, GravityCompat.END))

        val btn = Button(context).apply {
            text = "Get Started"
            setTextColor(Color.WHITE)
            textSize = 16.0f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(COLOR_PRIMARY)
                cornerRadius = context.dpToPx(28.0f).toFloat()
            }
            setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
        val btnParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(56.0f), Gravity.BOTTOM).apply {
            setMargins(context.dpToPx(32.0f), 0, context.dpToPx(32.0f), context.dpToPx(48.0f))
        }
        root.addView(btn, btnParams)

        return root
    }
}

class Step2Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpToPx(32.0f), context.dpToPx(64.0f), context.dpToPx(32.0f), context.dpToPx(48.0f))
        }

        val dots = DotsIndicatorView(context).apply {
            activePage = 1
        }
        val dotsParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(20.0f)).apply {
            bottomMargin = context.dpToPx(48.0f)
        }
        root.addView(dots, dotsParams)

        val card = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A"))
                cornerRadius = context.dpToPx(24.0f).toFloat()
            }
            setPadding(context.dpToPx(16.0f), context.dpToPx(24.0f), context.dpToPx(16.0f), context.dpToPx(24.0f))
        }

        val cardContent = FrameLayout(context)
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = GravityCompat.START
        }

        val sampleText = TextView(context).apply {
            val s = SpannableString("bro did u read abt supercalifragilistic...?")
            s.setSpan(BackgroundColorSpan(Color.parseColor("#154060")), 19, s.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            s.setSpan(ForegroundColorSpan(Color.parseColor("#7EC8FF")), 19, s.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = s
            setTextColor(Color.WHITE)
            textSize = 14.0f
            background = GradientDrawable().apply {
                setColor(COLOR_SURFACE)
                cornerRadius = context.dpToPx(12.0f).toFloat()
            }
            setPadding(context.dpToPx(12.0f), context.dpToPx(8.0f), context.dpToPx(12.0f), context.dpToPx(8.0f))
        }
        textContainer.addView(sampleText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val menu = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = context.dpToPx(8.0f).toFloat()
            }
            elevation = context.dpToPx(4.0f).toFloat()
            setPadding(context.dpToPx(8.0f), context.dpToPx(8.0f), context.dpToPx(8.0f), context.dpToPx(8.0f))
        }

        val copyTv = TextView(context).apply {
            text = "Copy"
            setTextColor(Color.BLACK)
            setPadding(context.dpToPx(8.0f), 0, context.dpToPx(8.0f), 0)
            textSize = 13.0f
        }
        menu.addView(copyTv)

        val cutTv = TextView(context).apply {
            text = "Cut"
            setTextColor(Color.BLACK)
            setPadding(context.dpToPx(8.0f), 0, context.dpToPx(8.0f), 0)
            textSize = 13.0f
        }
        menu.addView(cutTv)

        val divider = View(context).apply {
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }
        val dividerParams = LinearLayout.LayoutParams(context.dpToPx(1.0f), context.dpToPx(16.0f)).apply {
            setMargins(context.dpToPx(4.0f), 0, context.dpToPx(4.0f), 0)
        }
        menu.addView(divider, dividerParams)

        val eliTv = TextView(context).apply {
            text = "Explain with ELI5"
            setTextColor(COLOR_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(context.dpToPx(8.0f), 0, context.dpToPx(8.0f), 0)
            textSize = 13.0f
        }
        menu.addView(eliTv)

        val menuParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = context.dpToPx(16.0f)
            marginStart = context.dpToPx(16.0f)
        }
        textContainer.addView(menu, menuParams)

        cardContent.addView(textContainer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL))
        card.addView(cardContent, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val cardParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f).apply {
            bottomMargin = context.dpToPx(40.0f)
        }
        root.addView(card, cardParams)

        menu.alpha = 0.0f
        menu.translationY = context.dpToPx(60.0f).toFloat()
        menu.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.animate().alpha(1.0f).translationY(0.0f).setDuration(400L).setInterpolator(DecelerateInterpolator()).setStartDelay(200L).start()
            }
            override fun onViewDetachedFromWindow(v: View) {}
        })

        val heading = TextView(context).apply {
            text = "Highlight. Tap. Understand."
            textSize = 22.0f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
        }
        val headingParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(12.0f)
        }
        root.addView(heading, headingParams)

        val desc = TextView(context).apply {
            text = "Works in WhatsApp, Chrome, Twitter, PDFs — everywhere."
            textSize = 14.0f
            setTextColor(COLOR_TEXT_SEC)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setLineSpacing(0.0f, 1.2f)
        }
        val descParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(32.0f)
        }
        root.addView(desc, descParams)

        val nextBtn = Button(context).apply {
            text = "Next →"
            setTextColor(Color.WHITE)
            textSize = 16.0f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(COLOR_PRIMARY)
                cornerRadius = context.dpToPx(28.0f).toFloat()
            }
            setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
        root.addView(nextBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(56.0f)))

        return root
    }
}

class Step3Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpToPx(32.0f), context.dpToPx(64.0f), context.dpToPx(32.0f), context.dpToPx(48.0f))
        }

        val dots = DotsIndicatorView(context).apply {
            activePage = 2
        }
        val dotsParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(20.0f)).apply {
            bottomMargin = context.dpToPx(48.0f)
        }
        root.addView(dots, dotsParams)

        val card = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A1A1A"))
                cornerRadius = context.dpToPx(24.0f).toFloat()
            }
            setPadding(context.dpToPx(16.0f), context.dpToPx(24.0f), context.dpToPx(16.0f), context.dpToPx(24.0f))
        }

        val cardContent = FrameLayout(context)

        val originalText = TextView(context).apply {
            val s = SpannableString("Hie could I order a little chip packs!?")
            s.setSpan(UnderlineSpan(), 0, s.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            s.setSpan(ForegroundColorSpan(COLOR_RED), 0, s.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
            text = s
            textSize = 14.0f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2A2A2A"))
                cornerRadius = context.dpToPx(20.0f).toFloat()
            }
            setPadding(context.dpToPx(16.0f), context.dpToPx(12.0f), context.dpToPx(16.0f), context.dpToPx(12.0f))
        }

        val popup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(COLOR_SURFACE)
                val r = context.dpToPx(16.0f).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0.0f, 0.0f, 0.0f, 0.0f)
            }
            elevation = context.dpToPx(4.0f).toFloat()
            setPadding(context.dpToPx(16.0f), context.dpToPx(16.0f), context.dpToPx(16.0f), context.dpToPx(16.0f))
        }

        val refinedText = TextView(context).apply {
            text = "✨ \"Hi, could I order some sm...\""
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 13.0f
            typeface = Typeface.DEFAULT_BOLD
        }
        val refinedParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(16.0f)
        }
        popup.addView(refinedText, refinedParams)

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3.0f
        }

        val acceptBtn = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(COLOR_PRIMARY)
                cornerRadius = context.dpToPx(10.0f).toFloat()
            }
            val tv = TextView(context).apply {
                text = "Accept ✓"
                setTextColor(Color.WHITE)
                textSize = 12.0f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        val acceptParams = LinearLayout.LayoutParams(0, context.dpToPx(36.0f), 1.0f).apply {
            marginEnd = context.dpToPx(8.0f)
        }
        btnRow.addView(acceptBtn, acceptParams)

        val adjustBtn = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setStroke(context.dpToPx(1.0f), COLOR_BORDER)
                cornerRadius = context.dpToPx(10.0f).toFloat()
            }
            val tv = TextView(context).apply {
                text = "Adjust"
                setTextColor(Color.WHITE)
                textSize = 12.0f
                gravity = Gravity.CENTER
            }
            addView(tv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        val adjustParams = LinearLayout.LayoutParams(0, context.dpToPx(36.0f), 1.0f).apply {
            marginEnd = context.dpToPx(8.0f)
        }
        btnRow.addView(adjustBtn, adjustParams)

        val cancelBtn = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setStroke(context.dpToPx(1.0f), COLOR_BORDER)
                cornerRadius = context.dpToPx(10.0f).toFloat()
            }
            val tv = TextView(context).apply {
                text = "Cancel"
                setTextColor(Color.WHITE)
                textSize = 12.0f
                gravity = Gravity.CENTER
            }
            addView(tv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        btnRow.addView(cancelBtn, LinearLayout.LayoutParams(0, context.dpToPx(36.0f), 1.0f))

        popup.addView(btnRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val origParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
            bottomMargin = context.dpToPx(24.0f)
        }
        cardContent.addView(originalText, origParams)

        val popupParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
            bottomMargin = context.dpToPx(80.0f)
        }
        cardContent.addView(popup, popupParams)

        card.addView(cardContent, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val cardParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f).apply {
            bottomMargin = context.dpToPx(40.0f)
        }
        root.addView(card, cardParams)

        popup.alpha = 0.0f
        popup.translationY = context.dpToPx(80.0f).toFloat()
        popup.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.animate().alpha(1.0f).translationY(0.0f).setDuration(400L).setInterpolator(DecelerateInterpolator()).setStartDelay(200L).start()
            }
            override fun onViewDetachedFromWindow(v: View) {}
        })

        val heading = TextView(context).apply {
            text = "Fix your texts. One tap."
            textSize = 22.0f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
        }
        val headingParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(12.0f)
        }
        root.addView(heading, headingParams)

        val desc = TextView(context).apply {
            text = "Proofread anything you type — and replace it instantly inside the same app."
            textSize = 14.0f
            setTextColor(COLOR_TEXT_SEC)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setLineSpacing(0.0f, 1.2f)
        }
        val descParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(32.0f)
        }
        root.addView(desc, descParams)

        val nextBtn = Button(context).apply {
            text = "Next →"
            setTextColor(Color.WHITE)
            textSize = 16.0f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(COLOR_PRIMARY)
                cornerRadius = context.dpToPx(28.0f).toFloat()
            }
            setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
        root.addView(nextBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(56.0f)))

        return root
    }
}

class Step4Fragment : Fragment() {
    private var dots: DotsIndicatorView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpToPx(32.0f), context.dpToPx(64.0f), context.dpToPx(32.0f), context.dpToPx(48.0f))
        }

        val dotsView = DotsIndicatorView(context).apply {
            activePage = 3
        }
        dots = dotsView
        val dotsParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(20.0f)).apply {
            bottomMargin = context.dpToPx(64.0f)
        }
        root.addView(dotsView, dotsParams)

        val shieldContainer = FrameLayout(context).apply {
            addView(ShieldView(context), FrameLayout.LayoutParams(context.dpToPx(72.0f), context.dpToPx(80.0f), Gravity.CENTER))
        }
        val shieldParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(120.0f)).apply {
            bottomMargin = context.dpToPx(24.0f)
        }
        root.addView(shieldContainer, shieldParams)

        val heading = TextView(context).apply {
            text = "One permission.\nUnlimited power."
            textSize = 24.0f
            setTextColor(COLOR_TEXT_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val headingParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(16.0f)
        }
        root.addView(heading, headingParams)

        val desc = TextView(context).apply {
            text = "ELI5 needs to display over other apps so the explanation card can appear on top of Chrome, WhatsApp, and everywhere else — without switching apps."
            textSize = 14.0f
            setTextColor(COLOR_TEXT_SEC)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setLineSpacing(0.0f, 1.2f)
        }
        val descParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(32.0f)
        }
        root.addView(desc, descParams)

        val tipBox = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(COLOR_SURFACE)
                cornerRadius = context.dpToPx(12.0f).toFloat()
            }
            val accentBar = View(context).apply {
                setBackgroundColor(COLOR_PRIMARY)
            }
            addView(accentBar, LinearLayout.LayoutParams(context.dpToPx(4.0f), ViewGroup.LayoutParams.MATCH_PARENT))

            val tipText = TextView(context).apply {
                text = "We never read your screen(s). The overlay only shows when YOU tap Explain or Proofread."
                setTextColor(COLOR_TEXT_SEC)
                textSize = 13.0f
                setTypeface(null, Typeface.ITALIC)
                setPadding(context.dpToPx(16.0f), context.dpToPx(16.0f), context.dpToPx(16.0f), context.dpToPx(16.0f))
            }
            addView(tipText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
        }
        val tipParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = context.dpToPx(32.0f)
        }
        root.addView(tipBox, tipParams)

        val previewImages = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            weightSum = 2.0f

            val img1 = ImageView(context).apply {
                setImageResource(R.drawable.tutorial_img_1)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
            val img1Params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f).apply {
                marginEnd = context.dpToPx(8.0f)
            }
            addView(img1, img1Params)

            val img2 = ImageView(context).apply {
                setImageResource(R.drawable.tutorial_img_2)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
            val img2Params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f).apply {
                marginStart = context.dpToPx(8.0f)
            }
            addView(img2, img2Params)
        }
        val previewParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f).apply {
            bottomMargin = context.dpToPx(32.0f)
        }
        root.addView(previewImages, previewParams)

        val grantBtn = Button(context).apply {
            text = "Grant Permission"
            setTextColor(Color.WHITE)
            textSize = 16.0f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(COLOR_PRIMARY)
                cornerRadius = context.dpToPx(28.0f).toFloat()
            }
            setOnClickListener {
                val onboarding = activity as? OnboardingActivity ?: return@setOnClickListener
                if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    onboarding.requestPermissionLauncher.launch(intent)
                } else {
                    onboarding.requestPermissionLauncher.launch(Intent())
                }
            }
        }
        root.addView(grantBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(56.0f)))

        return root
    }

    fun onPermissionGranted() {
        dots?.activePage = 4
    }
}

class OnboardingActivity : AppCompatActivity() {
    lateinit var viewPager: ViewPager2

    val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) {
            getSharedPreferences("eli5_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_done", true)
                .apply()
            for (fragment in supportFragmentManager.fragments) {
                if (fragment is Step4Fragment) {
                    fragment.onPermissionGranted()
                }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }, 800L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val root = FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(root)

        viewPager = ViewPager2(this).apply {
            isUserInputEnabled = false
            adapter = object : FragmentStateAdapter(this@OnboardingActivity) {
                override fun getItemCount(): Int = 4
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> Step1Fragment()
                        1 -> Step2Fragment()
                        2 -> Step3Fragment()
                        else -> Step4Fragment()
                    }
                }
            }
        }
        root.addView(viewPager, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    fun nextStep() {
        if (viewPager.currentItem < 3) {
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }
}

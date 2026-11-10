package com.alaotach.eli5

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun getOnboardingBg(context: Context): Int =
    Color.parseColor(if (SettingsManager.isDarkTheme(context)) "#120E1A" else "#F8F5FF")

fun getOnboardingCardBg(context: Context): Int =
    Color.parseColor(if (SettingsManager.isDarkTheme(context)) "#1C1826" else "#FFFFFF")

val ONBOARDING_PURPLE: Int = Color.parseColor("#6A0DAD")
val ONBOARDING_PURPLE_LIGHT: Int = Color.parseColor("#E9D5FF")

fun getOnboardingTextPrimary(context: Context): Int =
    Color.parseColor(if (SettingsManager.isDarkTheme(context)) "#FFFFFF" else "#21112F")

fun getOnboardingTextMuted(context: Context): Int =
    Color.parseColor(if (SettingsManager.isDarkTheme(context)) "#9E9AA8" else "#766782")

fun Context.dpToPx(dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}

fun createStepIndicator(context: Context, activeIndex: Int): LinearLayout {
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, context.dpToPx(14f))
    }
    for (i in 0 until 3) {
        val dot = View(context).apply {
            val isActive = i == activeIndex
            layoutParams = LinearLayout.LayoutParams(
                context.dpToPx(if (isActive) 24f else 8f),
                context.dpToPx(8f)
            ).apply {
                setMargins(context.dpToPx(4f), 0, context.dpToPx(4f), 0)
            }
            background = GradientDrawable().apply {
                setColor(if (isActive) ONBOARDING_PURPLE else Color.parseColor("#E9D5FF"))
                cornerRadius = context.dpToPx(4f).toFloat()
            }
        }
        row.addView(dot)
    }
    return row
}

// -------------------------------------------------------------
// Screen 1: How It Works Demo (Animated Looping Tutorial)
// -------------------------------------------------------------
class OnboardingStep1Fragment : Fragment() {
    private var loopJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getOnboardingBg(context))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Scrollable content area
        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpToPx(24f), context.dpToPx(16f), context.dpToPx(24f), context.dpToPx(16f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        scrollView.addView(content)
        root.addView(scrollView)

        // Progress dots
        content.addView(createStepIndicator(context, 0))

        // Title & subtitle
        val title = TextView(context).apply {
            text = getString(R.string.onboarding_step1_title)
            setTextColor(getOnboardingTextPrimary(context))
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(context).apply {
            text = getString(R.string.onboarding_step1_subtitle)
            setTextColor(getOnboardingTextMuted(context))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(6f), 0, context.dpToPx(14f))
        }
        content.addView(title)
        content.addView(subtitle)

        // Demo card container
        val demoCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(getOnboardingCardBg(context))
                cornerRadius = context.dpToPx(18f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(100, 139, 92, 246))
            }
            setPadding(context.dpToPx(12f), context.dpToPx(12f), context.dpToPx(12f), context.dpToPx(12f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dpToPx(8f)
            }
        }

        val demoImageView = ImageView(context).apply {
            setImageResource(R.drawable.tutorial_img_6)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            maxHeight = context.dpToPx(260f)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        demoCard.addView(demoImageView)

        // Step caption bar
        val stepCaption = TextView(context).apply {
            text = getString(R.string.onboarding_step1_caption1)
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(8f), 0, context.dpToPx(8f))
        }
        demoCard.addView(stepCaption)

        // Step indicator dots inside card
        val dotsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(4f))
        }
        val dotViews = mutableListOf<View>()
        for (i in 0 until 3) {
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(context.dpToPx(if (i == 0) 18f else 6f), context.dpToPx(6f)).apply {
                    setMargins(context.dpToPx(3f), 0, context.dpToPx(3f), 0)
                }
                background = GradientDrawable().apply {
                    setColor(if (i == 0) ONBOARDING_PURPLE else Color.parseColor("#E9D5FF"))
                    cornerRadius = context.dpToPx(3f).toFloat()
                }
            }
            dotViews.add(dot)
            dotsRow.addView(dot)
        }
        demoCard.addView(dotsRow)
        content.addView(demoCard)

        // Looping animation steps
        val demoSteps = listOf(
            Triple(R.drawable.tutorial_img_6, getString(R.string.onboarding_step1_caption1), 0),
            Triple(R.drawable.tutorial_img_7, getString(R.string.onboarding_step1_caption2), 1),
            Triple(R.drawable.tutorial_img_4, getString(R.string.onboarding_step1_caption3), 2)
        )
        var currentStep = 0

        fun updateStep(stepIndex: Int) {
            currentStep = stepIndex % demoSteps.size
            val (drawableRes, captionText, idx) = demoSteps[currentStep]
            demoImageView.alpha = 0f
            demoImageView.setImageResource(drawableRes)
            demoImageView.animate().alpha(1f).setDuration(250).start()
            stepCaption.text = captionText
            for (j in dotViews.indices) {
                val d = dotViews[j]
                val isActive = j == idx
                d.layoutParams = LinearLayout.LayoutParams(context.dpToPx(if (isActive) 18f else 6f), context.dpToPx(6f)).apply {
                    setMargins(context.dpToPx(3f), 0, context.dpToPx(3f), 0)
                }
                d.background = GradientDrawable().apply {
                    setColor(if (isActive) ONBOARDING_PURPLE else Color.parseColor("#E9D5FF"))
                    cornerRadius = context.dpToPx(3f).toFloat()
                }
            }
        }

        loopJob = lifecycleScope.launch {
            while (isActive) {
                delay(2800)
                updateStep(currentStep + 1)
            }
        }

        demoCard.setOnClickListener {
            updateStep(currentStep + 1)
        }

        // Pinned Bottom Button Bar (Guaranteed visible on every screen)
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(24f), context.dpToPx(8f), context.dpToPx(24f), context.dpToPx(16f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            background = ColorDrawable(getOnboardingBg(context))
        }

        val continueBtn = Button(context).apply {
            text = getString(R.string.onboarding_btn_continue)
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(28f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(52f))
            setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
        bottomBar.addView(continueBtn)
        root.addView(bottomBar)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loopJob?.cancel()
    }
}

// -------------------------------------------------------------
// Screen 2: Permission Explanation (Used only to show the explanation card)
// -------------------------------------------------------------
class OnboardingStep2Fragment : Fragment() {
    private var grantBtn: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getOnboardingBg(context))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Scrollable content area
        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpToPx(24f), context.dpToPx(16f), context.dpToPx(24f), context.dpToPx(16f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        scrollView.addView(content)
        root.addView(scrollView)

        // Progress dots
        content.addView(createStepIndicator(context, 1))

        // Custom vector ShieldView replacing emoji
        val shield = ShieldView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dpToPx(52f), context.dpToPx(58f)).apply {
                topMargin = context.dpToPx(4f)
                bottomMargin = context.dpToPx(8f)
            }
        }
        content.addView(shield)

        val title = TextView(context).apply {
            text = getString(R.string.onboarding_step2_title)
            setTextColor(getOnboardingTextPrimary(context))
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        content.addView(title)

        // Key required line per spec
        val keyLineCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EDE9FE"))
                cornerRadius = context.dpToPx(12f).toFloat()
                setStroke(context.dpToPx(1f), ONBOARDING_PURPLE)
            }
            setPadding(context.dpToPx(16f), context.dpToPx(8f), context.dpToPx(16f), context.dpToPx(8f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpToPx(10f)
                bottomMargin = context.dpToPx(6f)
            }
        }

        val keyLineText = TextView(context).apply {
            text = getString(R.string.onboarding_step2_keyline)
            setTextColor(ONBOARDING_PURPLE)
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        keyLineCard.addView(keyLineText)
        content.addView(keyLineCard)

        val explanation = TextView(context).apply {
            text = getString(R.string.onboarding_step2_subtitle)
            setTextColor(getOnboardingTextMuted(context))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(8f))
        }
        content.addView(explanation)

        // Tutorial Screenshots Guide (tutorial_img_1 & tutorial_img_2 side by side)
        val imagesRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dpToPx(8f)
            }
        }

        val cardHeight = context.dpToPx(148f)

        // Card 1: Find ELI5
        val card1 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(getOnboardingCardBg(context))
                cornerRadius = context.dpToPx(14f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(80, 139, 92, 246))
            }
            setPadding(context.dpToPx(6f), context.dpToPx(6f), context.dpToPx(6f), context.dpToPx(6f))
            layoutParams = LinearLayout.LayoutParams(0, cardHeight, 1f).apply {
                marginEnd = context.dpToPx(6f)
            }
        }
        val img1 = ImageView(context).apply {
            setImageResource(R.drawable.tutorial_img_1)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val lbl1 = TextView(context).apply {
            text = getString(R.string.onboarding_step2_card1)
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(3f), 0, 0)
        }
        card1.addView(img1)
        card1.addView(lbl1)

        // Card 2: Turn ON toggle
        val card2 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(getOnboardingCardBg(context))
                cornerRadius = context.dpToPx(14f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(80, 139, 92, 246))
            }
            setPadding(context.dpToPx(6f), context.dpToPx(6f), context.dpToPx(6f), context.dpToPx(6f))
            layoutParams = LinearLayout.LayoutParams(0, cardHeight, 1f).apply {
                marginStart = context.dpToPx(6f)
            }
        }
        val img2 = ImageView(context).apply {
            setImageResource(R.drawable.tutorial_img_2)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val lbl2 = TextView(context).apply {
            text = getString(R.string.onboarding_step2_card2)
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(3f), 0, 0)
        }
        card2.addView(img2)
        card2.addView(lbl2)

        imagesRow.addView(card1)
        imagesRow.addView(card2)
        content.addView(imagesRow)

        // Pinned Bottom Button Bar (Guaranteed visible on every screen)
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(24f), context.dpToPx(8f), context.dpToPx(24f), context.dpToPx(16f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            background = ColorDrawable(getOnboardingBg(context))
        }

        grantBtn = Button(context).apply {
            val isGranted = Settings.canDrawOverlays(context)
            text = if (isGranted) getString(R.string.onboarding_btn_granted) else getString(R.string.onboarding_btn_grant)
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(if (isGranted) Color.parseColor("#10B981") else ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(28f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(52f))
            setOnClickListener {
                val onboarding = activity as? OnboardingActivity ?: return@setOnClickListener
                if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    onboarding.requestPermissionLauncher.launch(intent)
                } else {
                    onboarding.nextStep()
                }
            }
        }
        bottomBar.addView(grantBtn)
        root.addView(bottomBar)

        return root
    }

    override fun onResume() {
        super.onResume()
        val context = context ?: return
        if (Settings.canDrawOverlays(context)) {
            grantBtn?.text = getString(R.string.onboarding_btn_granted)
            grantBtn?.background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = context.dpToPx(28f).toFloat()
            }
        }
    }
}

// -------------------------------------------------------------
// Screen 3: Practice ("Try it here" with sample text & green check)
// -------------------------------------------------------------
class OnboardingStep3Fragment : Fragment() {
    private var hasTested = false
    private var greenCheckContainer: LinearLayout? = null
    private var finishBtn: Button? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getOnboardingBg(context))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Scrollable content area
        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpToPx(24f), context.dpToPx(16f), context.dpToPx(24f), context.dpToPx(16f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        scrollView.addView(content)
        root.addView(scrollView)

        // Progress dots
        content.addView(createStepIndicator(context, 2))

        val title = TextView(context).apply {
            text = getString(R.string.onboarding_step3_title)
            setTextColor(getOnboardingTextPrimary(context))
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(context).apply {
            text = getString(R.string.onboarding_step3_subtitle)
            setTextColor(getOnboardingTextMuted(context))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(6f), 0, context.dpToPx(16f))
        }
        content.addView(title)
        content.addView(subtitle)

        // Sample 1: Lease clause
        val sample1Card = createSampleCard(
            badge = getString(R.string.onboarding_sample1_badge),
            text = getString(R.string.onboarding_sample1_text),
            onTest = { testText ->
                onUserTestedSample(testText)
            }
        )
        content.addView(sample1Card)

        // Sample 2: Medical line
        val sample2Card = createSampleCard(
            badge = getString(R.string.onboarding_sample2_badge),
            text = getString(R.string.onboarding_sample2_text),
            onTest = { testText ->
                onUserTestedSample(testText)
            }
        )
        content.addView(sample2Card)

        // Green check success box
        greenCheckContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#142E1F"))
                cornerRadius = context.dpToPx(14f).toFloat()
                setStroke(context.dpToPx(1.5f), Color.parseColor("#22C55E"))
            }
            setPadding(context.dpToPx(16f), context.dpToPx(14f), context.dpToPx(16f), context.dpToPx(14f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpToPx(8f)
                bottomMargin = context.dpToPx(12f)
            }
            visibility = View.GONE
        }
        val checkIcon = TextView(context).apply {
            text = "✓"
            setTextColor(Color.parseColor("#22C55E"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, context.dpToPx(12f), 0)
        }
        val checkText = TextView(context).apply {
            text = getString(R.string.onboarding_step3_success)
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(0f, 1.2f)
        }
        greenCheckContainer?.addView(checkIcon)
        greenCheckContainer?.addView(checkText)
        content.addView(greenCheckContainer)

        // Pinned Bottom Button Bar (Guaranteed visible on every screen)
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(24f), context.dpToPx(8f), context.dpToPx(24f), context.dpToPx(16f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            background = ColorDrawable(getOnboardingBg(context))
        }

        finishBtn = Button(context).apply {
            text = getString(R.string.onboarding_btn_get_started)
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(28f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(52f))
            setOnClickListener {
                completeOnboarding()
            }
        }
        bottomBar.addView(finishBtn)
        root.addView(bottomBar)

        return root
    }

    private fun createSampleCard(badge: String, text: String, onTest: (String) -> Unit): LinearLayout {
        val context = requireContext()
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(getOnboardingCardBg(context))
                cornerRadius = context.dpToPx(18f).toFloat()
                setStroke(context.dpToPx(1.5f), Color.argb(120, 139, 92, 246))
            }
            setPadding(context.dpToPx(16f), context.dpToPx(14f), context.dpToPx(16f), context.dpToPx(14f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dpToPx(12f)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onTest(text)
            }
        }

        val badgePill = TextView(context).apply {
            this.text = badge
            setTextColor(ONBOARDING_PURPLE)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EDE9FE"))
                cornerRadius = context.dpToPx(6f).toFloat()
                setStroke(context.dpToPx(1f), Color.parseColor("#C4B5FD"))
            }
            setPadding(context.dpToPx(10f), context.dpToPx(4f), context.dpToPx(10f), context.dpToPx(4f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dpToPx(10f)
            }
        }

        val bodyTv = TextView(context).apply {
            this.text = text
            setTextColor(getOnboardingTextPrimary(context))
            textSize = 14.5f
            setLineSpacing(0f, 1.25f)
            setPadding(0, 0, 0, context.dpToPx(12f))
        }

        val btn = TextView(context).apply {
            this.text = getString(R.string.explain_with_eli5)
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(12f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(42f))
            setOnClickListener {
                onTest(text)
            }
        }

        card.addView(badgePill)
        card.addView(bodyTv)
        card.addView(btn)
        return card
    }

    private fun onUserTestedSample(sampleText: String) {
        hasTested = true
        greenCheckContainer?.visibility = View.VISIBLE
        finishBtn?.text = getString(R.string.onboarding_btn_lets_go)
        val intent = Intent(requireContext(), ExplainOverlayActivity::class.java).apply {
            putExtra(ExplainOverlayActivity.EXTRA_TEXT, sampleText)
        }
        startActivity(intent)
    }

    private fun completeOnboarding() {
        requireContext().getSharedPreferences("eli5_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_done", true)
            .apply()

        startActivity(Intent(requireContext(), MainActivity::class.java))
        activity?.finish()
    }
}

// -------------------------------------------------------------
// OnboardingActivity Main Host
// -------------------------------------------------------------
class OnboardingActivity : AppCompatActivity() {
    lateinit var viewPager: ViewPager2

    val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) {
            nextStep()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsManager.getLanguage(newBase)
        val langObj = LanguageManager.getLanguageByEnglishName(lang)
        val locale = java.util.Locale.forLanguageTag(langObj.code)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isDark = SettingsManager.isDarkTheme(this)
        val bgCol = getOnboardingBg(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setBackgroundDrawable(ColorDrawable(bgCol))
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(bgCol)
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(root)

        // 3 Screens Max per spec
        viewPager = ViewPager2(this).apply {
            isUserInputEnabled = false
            adapter = object : FragmentStateAdapter(this@OnboardingActivity) {
                override fun getItemCount(): Int = 3
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> OnboardingStep1Fragment()
                        1 -> OnboardingStep2Fragment()
                        else -> OnboardingStep3Fragment()
                    }
                }
            }
        }
        root.addView(viewPager, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    fun nextStep() {
        if (viewPager.currentItem < 2) {
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }
}

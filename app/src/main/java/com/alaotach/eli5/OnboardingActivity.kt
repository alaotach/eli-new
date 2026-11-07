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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

val ONBOARDING_BG: Int = Color.parseColor("#120F1D")
val ONBOARDING_CARD_BG: Int = Color.parseColor("#1C172B")
val ONBOARDING_PURPLE: Int = Color.parseColor("#7C3AED")
val ONBOARDING_PURPLE_LIGHT: Int = Color.parseColor("#8B5CF6")
val ONBOARDING_TEXT_PRIMARY: Int = Color.parseColor("#FFFFFF")
val ONBOARDING_TEXT_MUTED: Int = Color.parseColor("#9E9AA8")

fun Context.dpToPx(dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
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
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(ONBOARDING_BG)
            setPadding(context.dpToPx(24f), context.dpToPx(32f), context.dpToPx(24f), context.dpToPx(24f))
        }

        // Title & subtitle
        val title = TextView(context).apply {
            text = "Explain anything in 1 tap"
            setTextColor(ONBOARDING_TEXT_PRIMARY)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(context).apply {
            text = "Highlight text in any app, tap More (⋮), and choose ELI5."
            setTextColor(ONBOARDING_TEXT_MUTED)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(6f), 0, context.dpToPx(16f))
        }
        root.addView(title)
        root.addView(subtitle)

        // Demo card container
        val demoCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(ONBOARDING_CARD_BG)
                cornerRadius = context.dpToPx(18f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(100, 139, 92, 246))
            }
            setPadding(context.dpToPx(12f), context.dpToPx(12f), context.dpToPx(12f), context.dpToPx(12f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val demoImageView = ImageView(context).apply {
            setImageResource(R.drawable.tutorial_img_6)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        demoCard.addView(demoImageView)

        // Step caption bar
        val stepCaption = TextView(context).apply {
            text = "1. Highlight text & tap ⋮"
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(8f), 0, context.dpToPx(8f))
        }
        demoCard.addView(stepCaption)

        // Step indicator dots
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
                    setColor(if (i == 0) ONBOARDING_PURPLE else Color.parseColor("#443C56"))
                    cornerRadius = context.dpToPx(3f).toFloat()
                }
            }
            dotViews.add(dot)
            dotsRow.addView(dot)
        }
        demoCard.addView(dotsRow)
        root.addView(demoCard)

        // Looping animation steps
        val demoSteps = listOf(
            Triple(R.drawable.tutorial_img_6, "1. Highlight text & tap ⋮", 0),
            Triple(R.drawable.tutorial_img_7, "2. Tap 'Explain with ELI5'", 1),
            Triple(R.drawable.tutorial_img_4, "3. Floating explanation appears instantly!", 2)
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
                    setColor(if (isActive) ONBOARDING_PURPLE else Color.parseColor("#443C56"))
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

        // Continue Button
        val continueBtn = Button(context).apply {
            text = "Continue →"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(28f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(54f)).apply {
                topMargin = context.dpToPx(16f)
            }
            setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
        root.addView(continueBtn)
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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(ONBOARDING_BG)
            setPadding(context.dpToPx(24f), context.dpToPx(32f), context.dpToPx(24f), context.dpToPx(24f))
        }

        // Custom vector ShieldView replacing emoji
        val shield = ShieldView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dpToPx(52f), context.dpToPx(58f)).apply {
                topMargin = context.dpToPx(8f)
                bottomMargin = context.dpToPx(12f)
            }
        }
        root.addView(shield)

        val title = TextView(context).apply {
            text = "Display Over Other Apps"
            setTextColor(ONBOARDING_TEXT_PRIMARY)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(title)

        // Key required line per spec
        val keyLineCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#261D3B"))
                cornerRadius = context.dpToPx(12f).toFloat()
                setStroke(context.dpToPx(1f), ONBOARDING_PURPLE)
            }
            setPadding(context.dpToPx(16f), context.dpToPx(10f), context.dpToPx(16f), context.dpToPx(10f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpToPx(12f)
                bottomMargin = context.dpToPx(8f)
            }
        }

        val keyLineText = TextView(context).apply {
            text = "Used only to show the explanation card."
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        keyLineCard.addView(keyLineText)
        root.addView(keyLineCard)

        val explanation = TextView(context).apply {
            text = "Find ELI5 in settings and switch the toggle ON."
            setTextColor(ONBOARDING_TEXT_MUTED)
            textSize = 13.5f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(10f))
        }
        root.addView(explanation)

        // Tutorial Screenshots Guide (tutorial_img_1 & tutorial_img_2 side by side)
        val imagesRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                bottomMargin = context.dpToPx(14f)
            }
        }

        // Card 1: Find ELI5
        val card1 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(ONBOARDING_CARD_BG)
                cornerRadius = context.dpToPx(14f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(80, 139, 92, 246))
            }
            setPadding(context.dpToPx(6f), context.dpToPx(6f), context.dpToPx(6f), context.dpToPx(8f))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
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
            text = "1. Tap ELI5"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(4f), 0, 0)
        }
        card1.addView(img1)
        card1.addView(lbl1)

        // Card 2: Turn ON toggle
        val card2 = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(ONBOARDING_CARD_BG)
                cornerRadius = context.dpToPx(14f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(80, 139, 92, 246))
            }
            setPadding(context.dpToPx(6f), context.dpToPx(6f), context.dpToPx(6f), context.dpToPx(8f))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
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
            text = "2. Turn ON"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(4f), 0, 0)
        }
        card2.addView(img2)
        card2.addView(lbl2)

        imagesRow.addView(card1)
        imagesRow.addView(card2)
        root.addView(imagesRow)

        val grantBtn = Button(context).apply {
            text = if (Settings.canDrawOverlays(context)) "Permission Granted ✓" else "Grant Permission"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(if (Settings.canDrawOverlays(context)) Color.parseColor("#10B981") else ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(28f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(54f))
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
        root.addView(grantBtn)
        return root
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
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(ONBOARDING_BG)
            setPadding(context.dpToPx(24f), context.dpToPx(36f), context.dpToPx(24f), context.dpToPx(28f))
        }

        val title = TextView(context).apply {
            text = "Try it right now"
            setTextColor(ONBOARDING_TEXT_PRIMARY)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(context).apply {
            text = "Tap either sample text to see the real explanation card:"
            setTextColor(ONBOARDING_TEXT_MUTED)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(6f), 0, context.dpToPx(20f))
        }
        root.addView(title)
        root.addView(subtitle)

        val scrollArea = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            isVerticalScrollBarEnabled = false
        }
        val samplesBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollArea.addView(samplesBox)

        // Sample 1: Lease clause
        val sample1Card = createSampleCard(
            badge = "⚖️  LEGAL LEASE CLAUSE",
            text = "The Lessee shall indemnify and hold harmless the Lessor from and against any subrogation liabilities and liquidated damages.",
            onTest = { testText ->
                onUserTestedSample(testText)
            }
        )
        samplesBox.addView(sample1Card)

        // Sample 2: Medical line
        val sample2Card = createSampleCard(
            badge = "🩺  MEDICAL DIAGNOSIS",
            text = "The patient presents with idiopathic benign paroxysmal positional vertigo secondary to canalithiasis.",
            onTest = { testText ->
                onUserTestedSample(testText)
            }
        )
        samplesBox.addView(sample2Card)

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
            text = "Awesome! Explanation works. You're ready to use ELI5 anywhere."
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(0f, 1.2f)
        }
        greenCheckContainer?.addView(checkIcon)
        greenCheckContainer?.addView(checkText)
        samplesBox.addView(greenCheckContainer)

        root.addView(scrollArea)

        finishBtn = Button(context).apply {
            text = "Get Started →"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(28f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(56f)).apply {
                topMargin = context.dpToPx(16f)
            }
            setOnClickListener {
                completeOnboarding()
            }
        }
        root.addView(finishBtn)
        return root
    }

    private fun createSampleCard(badge: String, text: String, onTest: (String) -> Unit): LinearLayout {
        val context = requireContext()
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ONBOARDING_CARD_BG)
                cornerRadius = context.dpToPx(18f).toFloat()
                setStroke(context.dpToPx(1.5f), Color.argb(120, 139, 92, 246))
            }
            setPadding(context.dpToPx(18f), context.dpToPx(16f), context.dpToPx(18f), context.dpToPx(16f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dpToPx(16f)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onTest(text)
            }
        }

        val badgePill = TextView(context).apply {
            this.text = badge
            setTextColor(Color.parseColor("#DDD6FE"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2A2040"))
                cornerRadius = context.dpToPx(6f).toFloat()
                setStroke(context.dpToPx(1f), Color.parseColor("#6D28D9"))
            }
            setPadding(context.dpToPx(10f), context.dpToPx(4f), context.dpToPx(10f), context.dpToPx(4f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dpToPx(12f)
            }
        }

        val bodyTv = TextView(context).apply {
            this.text = text
            setTextColor(Color.parseColor("#F9FAFB"))
            textSize = 15f
            setLineSpacing(0f, 1.3f)
            setPadding(0, 0, 0, context.dpToPx(14f))
        }

        val btn = TextView(context).apply {
            this.text = "Explain with ELI5 ✨"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(12f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(44f))
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
        finishBtn?.text = "Let's Go! →"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setBackgroundDrawable(ColorDrawable(ONBOARDING_BG))

        val root = FrameLayout(this).apply {
            setBackgroundColor(ONBOARDING_BG)
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

package com.alaotach.eli5

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

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
// Screen 1: How It Works Demo
// -------------------------------------------------------------
class OnboardingStep1Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dpToPx(24f), context.dpToPx(40f), context.dpToPx(24f), context.dpToPx(32f))
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
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(8f), 0, context.dpToPx(32f))
        }
        root.addView(title)
        root.addView(subtitle)

        // Visual simulation of text selection & context menu
        val demoContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ONBOARDING_CARD_BG)
                cornerRadius = context.dpToPx(20f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(100, 139, 92, 246))
            }
            setPadding(context.dpToPx(20f), context.dpToPx(20f), context.dpToPx(20f), context.dpToPx(20f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val appMockBar = TextView(context).apply {
            text = "🌐 Browser / Document"
            setTextColor(ONBOARDING_TEXT_MUTED)
            textSize = 12f
            setPadding(0, 0, 0, context.dpToPx(12f))
        }
        demoContainer.addView(appMockBar)

        val sampleTextTv = TextView(context).apply {
            text = "Quantum computing uses superposition to process complex information exponential times faster."
            setTextColor(Color.parseColor("#E0E0E6"))
            textSize = 15f
            setLineSpacing(0f, 1.25f)
            setPadding(0, 0, 0, context.dpToPx(16f))
        }
        demoContainer.addView(sampleTextTv)

        // Simulated Android Context Menu Popup
        val menuPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#282238"))
                cornerRadius = context.dpToPx(12f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(120, 139, 92, 246))
            }
            setPadding(context.dpToPx(8f), context.dpToPx(6f), context.dpToPx(8f), context.dpToPx(6f))
            gravity = Gravity.CENTER
        }

        val optCopy = TextView(context).apply {
            text = "Copy"
            setTextColor(ONBOARDING_TEXT_MUTED)
            textSize = 13f
            setPadding(context.dpToPx(8f), 0, context.dpToPx(8f), 0)
        }
        val optShare = TextView(context).apply {
            text = "Share"
            setTextColor(ONBOARDING_TEXT_MUTED)
            textSize = 13f
            setPadding(context.dpToPx(8f), 0, context.dpToPx(8f), 0)
        }
        val optEli5 = TextView(context).apply {
            text = "✨ ELI5"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(8f).toFloat()
            }
            setPadding(context.dpToPx(12f), context.dpToPx(6f), context.dpToPx(12f), context.dpToPx(6f))
        }
        menuPill.addView(optCopy)
        menuPill.addView(optShare)
        menuPill.addView(optEli5)
        demoContainer.addView(menuPill)

        // Mini preview card below
        val previewCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#151122"))
                cornerRadius = context.dpToPx(14f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(80, 139, 92, 246))
            }
            setPadding(context.dpToPx(14f), context.dpToPx(12f), context.dpToPx(14f), context.dpToPx(12f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpToPx(20f)
            }
        }
        val previewHeader = TextView(context).apply {
            text = "✨ ELI5 Card opens instantly:"
            setTextColor(ONBOARDING_PURPLE_LIGHT)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, context.dpToPx(4f))
        }
        val previewAnswer = TextView(context).apply {
            text = "Normal computers try keys one by one. Quantum computers try ALL keys at the exact same second."
            setTextColor(Color.WHITE)
            textSize = 13.5f
            setLineSpacing(0f, 1.2f)
        }
        previewCard.addView(previewHeader)
        previewCard.addView(previewAnswer)
        demoContainer.addView(previewCard)

        root.addView(demoContainer)

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
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(56f)).apply {
                topMargin = context.dpToPx(24f)
            }
            setOnClickListener {
                (activity as? OnboardingActivity)?.nextStep()
            }
        }
        root.addView(continueBtn)
        return root
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
            setPadding(context.dpToPx(24f), context.dpToPx(40f), context.dpToPx(24f), context.dpToPx(32f))
        }

        val icon = TextView(context).apply {
            text = "🪟"
            textSize = 48f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(16f), 0, context.dpToPx(16f))
        }
        val title = TextView(context).apply {
            text = "Display Over Other Apps"
            setTextColor(ONBOARDING_TEXT_PRIMARY)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(icon)
        root.addView(title)

        // Key required line per spec
        val keyLineCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#261D3B"))
                cornerRadius = context.dpToPx(14f).toFloat()
                setStroke(context.dpToPx(1f), ONBOARDING_PURPLE)
            }
            setPadding(context.dpToPx(18f), context.dpToPx(14f), context.dpToPx(18f), context.dpToPx(14f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpToPx(24f)
                bottomMargin = context.dpToPx(20f)
            }
        }

        val keyLineText = TextView(context).apply {
            text = "Used only to show the explanation card."
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        keyLineCard.addView(keyLineText)
        root.addView(keyLineCard)

        val explanation = TextView(context).apply {
            text = "When you highlight text in your browser, PDF reader, or messaging app, ELI5 draws a sleek floating card right over that app without closing it.\n\nTap below to enable this permission in Android Settings."
            setTextColor(ONBOARDING_TEXT_MUTED)
            textSize = 14.5f
            setLineSpacing(0f, 1.3f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(explanation)

        val grantBtn = Button(context).apply {
            text = "Grant Permission"
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
        }
        val samplesBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        scrollArea.addView(samplesBox)

        // Sample 1: Lease clause
        val sample1Card = createSampleCard(
            badge = "LEGAL LEASE CLAUSE",
            text = "The Lessee shall indemnify and hold harmless the Lessor from and against any subrogation liabilities and liquidated damages.",
            onTest = { testText ->
                onUserTestedSample(testText)
            }
        )
        samplesBox.addView(sample1Card)

        // Sample 2: Medical line
        val sample2Card = createSampleCard(
            badge = "MEDICAL DIAGNOSIS",
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
                cornerRadius = context.dpToPx(12f).toFloat()
                setStroke(context.dpToPx(1f), Color.parseColor("#2E7D32"))
            }
            setPadding(context.dpToPx(16f), context.dpToPx(12f), context.dpToPx(16f), context.dpToPx(12f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpToPx(14f)
            }
            visibility = View.GONE
        }
        val checkIcon = TextView(context).apply {
            text = "✓"
            setTextColor(Color.parseColor("#4CAF50"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, context.dpToPx(10f), 0)
        }
        val checkText = TextView(context).apply {
            text = "Great job! You're ready to use ELI5 anywhere."
            setTextColor(Color.WHITE)
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        greenCheckContainer?.addView(checkIcon)
        greenCheckContainer?.addView(checkText)
        samplesBox.addView(greenCheckContainer)

        root.addView(scrollArea)

        finishBtn = Button(context).apply {
            text = "Get Started"
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
                cornerRadius = context.dpToPx(16f).toFloat()
                setStroke(context.dpToPx(1f), Color.argb(90, 139, 92, 246))
            }
            setPadding(context.dpToPx(16f), context.dpToPx(14f), context.dpToPx(16f), context.dpToPx(14f))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = context.dpToPx(14f)
            }
        }

        val badgeTv = TextView(context).apply {
            this.text = badge
            setTextColor(ONBOARDING_PURPLE_LIGHT)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, context.dpToPx(6f))
        }
        val bodyTv = TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            setLineSpacing(0f, 1.2f)
            setPadding(0, 0, 0, context.dpToPx(12f))
        }
        val btn = Button(context).apply {
            this.text = "Explain with ELI5 ✨"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply {
                setColor(ONBOARDING_PURPLE)
                cornerRadius = context.dpToPx(10f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, context.dpToPx(38f))
            setOnClickListener {
                onTest(text)
            }
        }
        card.addView(badgeTv)
        card.addView(bodyTv)
        card.addView(btn)
        return card
    }

    private fun onUserTestedSample(sampleText: String) {
        hasTested = true
        greenCheckContainer?.visibility = View.VISIBLE
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

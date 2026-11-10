package com.alaotach.eli5.assistant

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import com.alaotach.eli5.AnalyticsManager
import com.alaotach.eli5.ExplainOverlayActivity
import com.alaotach.eli5.R
import com.alaotach.eli5.SettingsManager
import java.io.File

class CircleSnipActivity : ComponentActivity() {

    private lateinit var selectionView: CircleSelectionView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsManager.logCircleGesture(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        selectionView = CircleSelectionView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            onSelectionCompleted = { croppedBitmap, localPath, _ ->
                processCroppedBitmap(croppedBitmap, localPath)
            }

            onDismissRequested = {
                finish()
            }
        }

        root.addView(selectionView)

        // Floating Tutorial Video Button at bottom center
        val dp = resources.displayMetrics.density
        val isDark = SettingsManager.isDarkTheme(this)
        val tutorialBtn = TextView(this).apply {
            text = "Watch Tutorial"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_tutorial, 0, 0, 0)
            compoundDrawablePadding = (dp * 8).toInt()
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#6A0DAD"))
                cornerRadius = dp * 22f
                setStroke((dp * 1.2f).toInt(), Color.parseColor("#A855F7"))
            }
            setPadding((dp * 16).toInt(), (dp * 9).toInt(), (dp * 18).toInt(), (dp * 9).toInt())
            elevation = dp * 6f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = (dp * 28).toInt()
            }
            setOnClickListener {
                showTutorialVideoDialog()
            }
        }
        root.addView(tutorialBtn)

        setContentView(root)

        // Generate a rich demo canvas if no bitmap passed
        val demoBitmap = createDemoScreenBitmap()
        selectionView.setScreenshot(demoBitmap)

        // Show tutorial video overlay automatically on first open only
        val prefs = getSharedPreferences("eli5_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("try_gesture_tutorial_video_shown", false)) {
            prefs.edit().putBoolean("try_gesture_tutorial_video_shown", true).apply()
            root.postDelayed({
                if (!isFinishing && !isDestroyed) {
                    showTutorialVideoDialog()
                }
            }, 350)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::selectionView.isInitialized) {
            selectionView.resetSelection()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::selectionView.isInitialized) {
            selectionView.resetTranslation()
        }
    }

    private fun processCroppedBitmap(croppedBitmap: Bitmap, localPath: android.graphics.Path?) {
        val preferredLang = SettingsManager.getLanguage(this)
        OcrHelper.extractText(croppedBitmap, localPath, preferredLang) { extractedText ->
            if (!extractedText.isNullOrBlank()) {
                val intent = Intent(this, ExplainOverlayActivity::class.java).apply {
                    putExtra(ExplainOverlayActivity.EXTRA_TEXT, extractedText)
                    putExtra(ExplainOverlayActivity.EXTRA_MODE, SettingsManager.getDefaultMode(this@CircleSnipActivity))
                    putExtra(ExplainOverlayActivity.EXTRA_IS_SNIP, true)
                }
                startActivity(intent)
                // Keep CircleSnipActivity active underneath so the explanation overlay appears over the demo screen!
            } else {
                Toast.makeText(
                    this,
                    "No text found. Try circling around the demo text cards.",
                    Toast.LENGTH_SHORT
                ).show()
                selectionView.resetSelection()
            }
        }
    }

    private fun showTutorialVideoDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val dp = resources.displayMetrics.density
        val isDark = SettingsManager.isDarkTheme(this)
        var isFullscreen = false

        val dialogRoot = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#B3000000"))
        }

        val cardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(if (isDark) Color.parseColor("#1C1826") else Color.WHITE)
                cornerRadius = dp * 20f
                setStroke((dp * 1f).toInt(), if (isDark) Color.parseColor("#35234E") else Color.parseColor("#E9D5FF"))
            }
            elevation = dp * 12f
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((dp * 16).toInt(), (dp * 12).toInt(), (dp * 16).toInt(), (dp * 10).toInt())
        }

        val titleTv = TextView(this).apply {
            text = "Circle to ELI5 Tutorial"
            textSize = 15.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (isDark) Color.WHITE else Color.parseColor("#21112F"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        header.addView(titleTv)

        val fullscreenBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_fullscreen)
            imageTintList = ColorStateList.valueOf(if (isDark) Color.parseColor("#C4B5FD") else Color.parseColor("#6A0DAD"))
            layoutParams = LinearLayout.LayoutParams((dp * 32).toInt(), (dp * 32).toInt()).apply {
                marginEnd = (dp * 8).toInt()
            }
            setPadding((dp * 4).toInt(), (dp * 4).toInt(), (dp * 4).toInt(), (dp * 4).toInt())
        }
        header.addView(fullscreenBtn)

        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782"))
            setPadding((dp * 6).toInt(), 0, (dp * 6).toInt(), 0)
            setOnClickListener {
                dialog.dismiss()
            }
        }
        header.addView(closeBtn)
        cardContainer.addView(header)

        // Video View Frame
        val videoFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (dp * 240).toInt()
            )
            setBackgroundColor(Color.BLACK)
        }

        val videoView = VideoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        videoFrame.addView(videoView)
        cardContainer.addView(videoFrame)

        // Subtitle hint at bottom
        val hintTv = TextView(this).apply {
            text = "Circle any content on your screen to get an instant explanation"
            textSize = 12f
            setTextColor(if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782"))
            gravity = Gravity.CENTER
            setPadding((dp * 16).toInt(), (dp * 10).toInt(), (dp * 16).toInt(), (dp * 12).toInt())
        }
        cardContainer.addView(hintTv)

        val cardLayoutParams = FrameLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        dialogRoot.addView(cardContainer, cardLayoutParams)

        // Toggle Fullscreen action
        fullscreenBtn.setOnClickListener {
            isFullscreen = !isFullscreen
            if (isFullscreen) {
                fullscreenBtn.setImageResource(R.drawable.ic_fullscreen_exit)
                header.visibility = View.GONE
                hintTv.visibility = View.GONE
                (cardContainer.background as? GradientDrawable)?.cornerRadius = 0f
                cardLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                cardLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                videoFrame.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            } else {
                fullscreenBtn.setImageResource(R.drawable.ic_fullscreen)
                header.visibility = View.VISIBLE
                hintTv.visibility = View.VISIBLE
                (cardContainer.background as? GradientDrawable)?.cornerRadius = dp * 20f
                cardLayoutParams.width = (resources.displayMetrics.widthPixels * 0.92f).toInt()
                cardLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                videoFrame.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (dp * 240).toInt()
                )
            }
            cardContainer.layoutParams = cardLayoutParams
        }

        // Tap background outside to dismiss (when not fullscreen)
        dialogRoot.setOnClickListener {
            if (!isFullscreen) {
                dialog.dismiss()
            }
        }
        cardContainer.setOnClickListener { /* prevent dismiss when tapping container */ }

        // Video Setup
        val rawUri = Uri.parse("android.resource://$packageName/${R.raw.eli5_tutorial_voiceover}")
        val fileFallback = File("/home/aloo/Downloads/eli5_tutorial_voiceover.mp4")
        val videoUri = if (fileFallback.exists()) Uri.fromFile(fileFallback) else rawUri

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoView.start()
        }

        dialog.setOnDismissListener {
            try {
                videoView.stopPlayback()
            } catch (_: Exception) {}
        }

        dialog.setContentView(dialogRoot)
        dialog.show()
    }

    /**
     * Creates an authentic-looking demo screen with interesting sample texts
     * so users can immediately experience "Circle to ELI5" even inside the app demo.
     */
    private fun createDemoScreenBitmap(): Bitmap {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val dp = displayMetrics.density

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isDark = SettingsManager.isDarkTheme(this)

        // Persistent theme background (Dark or Light)
        canvas.drawColor(if (isDark) Color.parseColor("#120E1A") else Color.parseColor("#F8F5FF"))

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#1C1826") else Color.WHITE
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#35234E") else Color.parseColor("#EFE9F7")
            style = Paint.Style.STROKE
            strokeWidth = 1f * dp
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782")
            textSize = 10.5f * dp
            isFakeBoldText = true
            letterSpacing = 0.08f
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.WHITE else Color.parseColor("#21112F")
            textSize = 14f * dp
            isFakeBoldText = true
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#4A3E56")
            textSize = 11.5f * dp
        }

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#A855F7") else Color.parseColor("#6A0DAD")
            textSize = 9.5f * dp
            isFakeBoldText = true
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#C4B5FD") else Color.parseColor("#A855F7")
            textSize = 9.5f * dp
        }

        // Section header
        canvas.drawText("DISCOVER FEED", 20f * dp, 85f * dp, headerPaint)

        val cardMargin = 16f * dp
        val cardW = width - (cardMargin * 2)
        val cardH = 114f * dp
        val cardR = 16f * dp
        val cardGap = 12f * dp
        val innerPad = 14f * dp
        val leftText = cardMargin + innerPad

        // Card 1: Quantum Computing
        val c1Top = 98f * dp
        val c1Bottom = c1Top + cardH
        val card1 = RectF(cardMargin, c1Top, cardMargin + cardW, c1Bottom)
        canvas.drawRoundRect(card1, cardR, cardR, cardPaint)
        canvas.drawRoundRect(card1, cardR, cardR, borderPaint)

        canvas.drawText("TECH EXPLAINER", leftText, c1Top + 20f * dp, tagPaint)
        canvas.drawText("2h ago", cardMargin + cardW - innerPad - 36f * dp, c1Top + 20f * dp, metaPaint)
        canvas.drawText("Quantum Entanglement", leftText, c1Top + 40f * dp, titlePaint)
        canvas.drawText("When two particles remain connected so", leftText, c1Top + 58f * dp, bodyPaint)
        canvas.drawText("actions on one affect the other instantly,", leftText, c1Top + 74f * dp, bodyPaint)
        canvas.drawText("even across light-years of distance.", leftText, c1Top + 90f * dp, bodyPaint)
        canvas.drawText("4.5k Likes • Share", leftText, c1Top + 105f * dp, metaPaint)

        // Card 2: Legal Terms
        val c2Top = c1Bottom + cardGap
        val c2Bottom = c2Top + cardH
        val card2 = RectF(cardMargin, c2Top, cardMargin + cardW, c2Bottom)
        canvas.drawRoundRect(card2, cardR, cardR, cardPaint)
        canvas.drawRoundRect(card2, cardR, cardR, borderPaint)

        canvas.drawText("LEGAL TERMS", leftText, c2Top + 20f * dp, tagPaint)
        canvas.drawText("Yesterday", cardMargin + cardW - innerPad - 46f * dp, c2Top + 20f * dp, metaPaint)
        canvas.drawText("Force Majeure Clause", leftText, c2Top + 40f * dp, titlePaint)
        canvas.drawText("An unforeseeable circumstance preventing", leftText, c2Top + 58f * dp, bodyPaint)
        canvas.drawText("someone from fulfilling a contractual pact,", leftText, c2Top + 74f * dp, bodyPaint)
        canvas.drawText("such as natural disasters or war.", leftText, c2Top + 90f * dp, bodyPaint)
        canvas.drawText("1.2k Replies • Retweet", leftText, c2Top + 105f * dp, metaPaint)

        // Card 3: Adaptive localized demo card
        val c3Top = c2Bottom + cardGap
        val c3Bottom = c3Top + cardH
        val card3 = RectF(cardMargin, c3Top, cardMargin + cardW, c3Bottom)
        canvas.drawRoundRect(card3, cardR, cardR, cardPaint)
        canvas.drawRoundRect(card3, cardR, cardR, borderPaint)

        val lang = SettingsManager.getLanguage(this).lowercase()
        when {
            lang.contains("urdu") || lang == "ur" -> {
                canvas.drawText("سائنس اور ٹیکنالوجی", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("کوانٹم الجھاؤ", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("جب دو ذرات اس طرح آپس میں جڑ جائیں کہ", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("ایک پر عمل کرنے سے دوسرا فوری متاثر ہو،", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("چاہے وہ نوری سالوں کی دوری پر ہی کیوں نہ ہوں۔", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("persian") || lang.contains("farsi") || lang == "fa" -> {
                canvas.drawText("علم و فناوری", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("درهم‌تنیدگی کوانتومی", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("پدیده‌ای که در آن دو ذره چنان به هم پیوسته‌اند", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("که تغییر در یکی بلافاصله بر دیگری اثر می‌گذارد،", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("حتی اگر سال‌های نوری از هم فاصله داشته باشند.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("bengali") || lang == "bn" -> {
                canvas.drawText("বিজ্ঞান ও প্রযুক্তি", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("কোয়ান্টাম এন্ট্যাঙ্গলমেন্ট", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("যখন দুটি কণা এমনভাবে সংযুক্ত থাকে যে", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("একটির অবস্থা পরিবর্তনের সাথে সাথেই", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("দূরত্ব নির্বিশেষে অন্যটিতে প্রভাব পড়ে।", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("tamil") || lang == "ta" -> {
                canvas.drawText("அறிவியல் & தொழில்நுட்பம்", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("குவாண்டம் பின்னல்", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("இரண்டு துகள்கள் மிகத் தொலைவில் இருந்தாலும்,", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("ஒன்றின் மாற்றம் உடனடியாக மற்றொன்றை", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("பாதிக்கும் விசித்திரமான நிகழ்வு.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("telugu") || lang == "te" -> {
                canvas.drawText("సైన్స్ & టెక్నాలజీ", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("క్వాంటమ్ ఎంటాంగిల్మెంట్", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("రెండు కణాలు ఎంత దూరంలో ఉన్నప్పటికీ,", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("ఒకదానిలో మార్పు వెంటనే రెండవదానిపై", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("ప్రభావం చూపే భౌతిక దృగ్విషయం.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("arabic") || lang == "ar" -> {
                canvas.drawText("العلوم والتكنولوجيا", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("التشابك الكمي", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("عندما يرتبط جسيمان بحيث تؤثر التغيرات", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("في أحدهما فوراً على الآخر بغض النظر", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("عن المسافة الفاصلة بينهما.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("russian") || lang == "ru" -> {
                canvas.drawText("НАУКА И ТЕХНОЛОГИИ", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("Квантовая запутанность", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("Явление, при котором квантовые состояния", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("двух частиц оказываются взаимосвязанными", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("даже на колоссальных расстояниях.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("hindi") || lang.contains("marathi") || lang == "hi" || lang == "mr" -> {
                canvas.drawText("विज्ञान एवं तकनीक", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("क्वांटम उलझाव", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("जब दो कण एक दूसरे से जुड़े होते हैं,", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("तो एक की स्थिति बदलने पर दूसरे पर तुरंत", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("असर होता है, चाहे कितनी भी दूरी हो।", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("japanese") || lang == "ja" -> {
                canvas.drawText("テクノロジー", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("量子もつれの原理", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("2つの粒子が強く结びつき、互いの", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("状態がどれほど離れていても瞬時に", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("影響し合う物理現象です。", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("chinese") || lang == "zh" -> {
                canvas.drawText("科学技术", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("量子纠缠现象", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("当两个微观粒子相互纠缠在一起时，", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("即使相隔遥远，测量其中一个也会", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("瞬间影响另一个粒子的状态。", leftText, c3Top + 90f * dp, bodyPaint)
            }
            lang.contains("spanish") || lang == "es" -> {
                canvas.drawText("FINANZAS", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("Paradoja de Estanflación", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("Una combinación de estancamiento económico", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("con alta inflación y desempleo sostenido,", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("afectando el poder adquisitivo.", leftText, c3Top + 90f * dp, bodyPaint)
            }
            else -> {
                canvas.drawText("FINANCE", leftText, c3Top + 20f * dp, tagPaint)
                canvas.drawText("Stagflation Paradox", leftText, c3Top + 40f * dp, titlePaint)
                canvas.drawText("A rare toxic economic combination of", leftText, c3Top + 58f * dp, bodyPaint)
                canvas.drawText("stagnant growth, high unemployment, and", leftText, c3Top + 74f * dp, bodyPaint)
                canvas.drawText("rapidly escalating consumer inflation.", leftText, c3Top + 90f * dp, bodyPaint)
            }
        }
        canvas.drawText("3.1k Reads • Discussion", leftText, c3Top + 105f * dp, metaPaint)

        return bitmap
    }
}

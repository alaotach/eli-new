package com.alaotach.eli5.assistant

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.alaotach.eli5.AnalyticsManager
import com.alaotach.eli5.ExplainOverlayActivity
import com.alaotach.eli5.SettingsManager

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
        setContentView(root)

        // Generate a rich demo canvas if no bitmap passed
        val demoBitmap = createDemoScreenBitmap()
        selectionView.setScreenshot(demoBitmap)
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
                finish()
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

        // Redesigned Light Purple & Lavender theme
        canvas.drawColor(Color.parseColor("#F8F5FF"))

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EFE9F7")
            style = Paint.Style.STROKE
            strokeWidth = 1f * dp
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#766782")
            textSize = 10.5f * dp
            isFakeBoldText = true
            letterSpacing = 0.08f
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#21112F")
            textSize = 14f * dp
            isFakeBoldText = true
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A3E56")
            textSize = 11.5f * dp
        }

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6A0DAD")
            textSize = 9.5f * dp
            isFakeBoldText = true
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A855F7")
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

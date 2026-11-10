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

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark background
        canvas.drawColor(Color.parseColor("#0F172A"))

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            isFakeBoldText = true
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 38f
        }

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A78BFA")
            textSize = 32f
            isFakeBoldText = true
        }

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 28f
        }

        // Card 1: Quantum Computing
        val card1 = RectF(60f, 260f, width - 60f, 620f)
        canvas.drawRoundRect(card1, 32f, 32f, cardPaint)
        canvas.drawRoundRect(card1, 32f, 32f, borderPaint)
        canvas.drawText("TECH EXPLAINER", 100f, 330f, tagPaint)
        canvas.drawText("2h ago", width - 200f, 330f, metaPaint)
        canvas.drawText("Quantum Entanglement", 100f, 400f, titlePaint)
        canvas.drawText("When two particles remain connected so", 100f, 470f, bodyPaint)
        canvas.drawText("actions on one affect the other instantly,", 100f, 520f, bodyPaint)
        canvas.drawText("even across light-years of distance.", 100f, 570f, bodyPaint)
        canvas.drawText("4.5k Likes • Share", 100f, 650f, metaPaint)

        // Card 2: Legal Jargon
        val card2 = RectF(60f, 700f, width - 60f, 1060f)
        canvas.drawRoundRect(card2, 32f, 32f, cardPaint)
        canvas.drawRoundRect(card2, 32f, 32f, borderPaint)
        canvas.drawText("LEGAL TERMS", 100f, 770f, tagPaint)
        canvas.drawText("Yesterday", width - 240f, 770f, metaPaint)
        canvas.drawText("Force Majeure Clause", 100f, 840f, titlePaint)
        canvas.drawText("An unforeseeable circumstance preventing", 100f, 910f, bodyPaint)
        canvas.drawText("someone from fulfilling a contractual pact,", 100f, 960f, bodyPaint)
        canvas.drawText("such as natural disasters or war.", 100f, 1010f, bodyPaint)
        canvas.drawText("1.2k Replies • Retweet", 100f, 1090f, metaPaint)

        // Card 3: Adaptive localized demo card
        val card3 = RectF(60f, 1100f, width - 60f, 1460f)
        canvas.drawRoundRect(card3, 32f, 32f, cardPaint)
        canvas.drawRoundRect(card3, 32f, 32f, borderPaint)

        val lang = SettingsManager.getLanguage(this).lowercase()
        if (lang.contains("bengali") || lang == "bn") {
            canvas.drawText("বিজ্ঞান ও প্রযুক্তি", 100f, 1170f, tagPaint)
            canvas.drawText("কোয়ান্টাম এন্ট্যাঙ্গলমেন্ট", 100f, 1240f, titlePaint)
            canvas.drawText("যখন দুটি কণা এমনভাবে সংযুক্ত থাকে যে", 100f, 1310f, bodyPaint)
            canvas.drawText("একটির অবস্থা পরিবর্তনের সাথে সাথেই", 100f, 1360f, bodyPaint)
            canvas.drawText("দূরত্ব নির্বিশেষে অন্যটিতে প্রভাব পড়ে।", 100f, 1410f, bodyPaint)
        } else if (lang.contains("tamil") || lang == "ta") {
            canvas.drawText("அறிவியல் & தொழில்நுட்பம்", 100f, 1170f, tagPaint)
            canvas.drawText("குவாண்டம் பின்னல்", 100f, 1240f, titlePaint)
            canvas.drawText("இரண்டு துகள்கள் மிகத் தொலைவில் இருந்தாலும்,", 100f, 1310f, bodyPaint)
            canvas.drawText("ஒன்றின் மாற்றம் உடனடியாக மற்றொன்றை", 100f, 1360f, bodyPaint)
            canvas.drawText("பாதிக்கும் விசித்திரமான நிகழ்வு.", 100f, 1410f, bodyPaint)
        } else if (lang.contains("telugu") || lang == "te") {
            canvas.drawText("సైన్స్ & టెక్నాలజీ", 100f, 1170f, tagPaint)
            canvas.drawText("క్వాంటమ్ ఎంటాంగిల్మెంట్", 100f, 1240f, titlePaint)
            canvas.drawText("రెండు కణాలు ఎంత దూరంలో ఉన్నప్పటికీ,", 100f, 1310f, bodyPaint)
            canvas.drawText("ఒకదానిలో మార్పు వెంటనే రెండవదానిపై", 100f, 1360f, bodyPaint)
            canvas.drawText("ప్రభావం చూపే భౌతిక దృగ్విషయం.", 100f, 1410f, bodyPaint)
        } else if (lang.contains("arabic") || lang == "ar") {
            canvas.drawText("العلوم والتكنولوجيا", 100f, 1170f, tagPaint)
            canvas.drawText("التشابك الكمي", 100f, 1240f, titlePaint)
            canvas.drawText("عندما يرتبط جسيمان بحيث تؤثر التغيرات", 100f, 1310f, bodyPaint)
            canvas.drawText("في أحدهما فوراً على الآخر بغض النظر", 100f, 1360f, bodyPaint)
            canvas.drawText("عن المسافة الفاصلة بينهما.", 100f, 1410f, bodyPaint)
        } else if (lang.contains("russian") || lang == "ru") {
            canvas.drawText("НАУКА И ТЕХНОЛОГИИ", 100f, 1170f, tagPaint)
            canvas.drawText("Квантовая запутанность", 100f, 1240f, titlePaint)
            canvas.drawText("Явление, при котором квантовые состояния", 100f, 1310f, bodyPaint)
            canvas.drawText("двух частиц оказываются взаимосвязанными", 100f, 1360f, bodyPaint)
            canvas.drawText("даже на колоссальных расстояниях.", 100f, 1410f, bodyPaint)
        } else if (lang.contains("hindi") || lang.contains("marathi") || lang == "hi" || lang == "mr") {
            canvas.drawText("विज्ञान एवं तकनीक", 100f, 1170f, tagPaint)
            canvas.drawText("क्वांटम उलझाव", 100f, 1240f, titlePaint)
            canvas.drawText("जब दो कण एक दूसरे से जुड़े होते हैं,", 100f, 1310f, bodyPaint)
            canvas.drawText("तो एक की स्थिति बदलने पर दूसरे पर तुरंत", 100f, 1360f, bodyPaint)
            canvas.drawText("असर होता है, चाहे कितनी भी दूरी हो।", 100f, 1410f, bodyPaint)
        } else if (lang.contains("japanese") || lang == "ja") {
            canvas.drawText("テクノロジー", 100f, 1170f, tagPaint)
            canvas.drawText("量子もつれの原理", 100f, 1240f, titlePaint)
            canvas.drawText("2つの粒子が強く結びつき、互いの", 100f, 1310f, bodyPaint)
            canvas.drawText("状態がどれほど離れていても瞬時に", 100f, 1360f, bodyPaint)
            canvas.drawText("影響し合う物理現象です。", 100f, 1410f, bodyPaint)
        } else if (lang.contains("chinese") || lang == "zh") {
            canvas.drawText("科学技术", 100f, 1170f, tagPaint)
            canvas.drawText("量子纠缠现象", 100f, 1240f, titlePaint)
            canvas.drawText("当两个微观粒子相互纠缠在一起时，", 100f, 1310f, bodyPaint)
            canvas.drawText("即使相隔遥远，测量其中一个也会", 100f, 1360f, bodyPaint)
            canvas.drawText("瞬间影响另一个粒子的状态。", 100f, 1410f, bodyPaint)
        } else if (lang.contains("spanish") || lang == "es") {
            canvas.drawText("FINANZAS", 100f, 1170f, tagPaint)
            canvas.drawText("Paradoja de Estanflación", 100f, 1240f, titlePaint)
            canvas.drawText("Una combinación de estancamiento económico", 100f, 1310f, bodyPaint)
            canvas.drawText("con alta inflación y desempleo sostenido,", 100f, 1360f, bodyPaint)
            canvas.drawText("afectando el poder adquisitivo.", 100f, 1410f, bodyPaint)
        } else {
            canvas.drawText("FINANCE", 100f, 1170f, tagPaint)
            canvas.drawText("Stagflation Paradox", 100f, 1240f, titlePaint)
            canvas.drawText("A rare toxic economic combination of", 100f, 1310f, bodyPaint)
            canvas.drawText("stagnant growth, high unemployment, and", 100f, 1360f, bodyPaint)
            canvas.drawText("rapidly escalating consumer inflation.", 100f, 1410f, bodyPaint)
        }

        return bitmap
    }
}

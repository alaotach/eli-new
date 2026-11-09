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

    private fun processCroppedBitmap(croppedBitmap: Bitmap, localPath: android.graphics.Path?) {
        OcrHelper.extractText(croppedBitmap, localPath) { extractedText ->
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

        // Card 3: Finance
        val card3 = RectF(60f, 1100f, width - 60f, 1460f)
        canvas.drawRoundRect(card3, 32f, 32f, cardPaint)
        canvas.drawRoundRect(card3, 32f, 32f, borderPaint)
        canvas.drawText("FINANCE", 100f, 1170f, tagPaint)
        canvas.drawText("Stagflation Paradox", 100f, 1240f, titlePaint)
        canvas.drawText("A rare toxic economic combination of", 100f, 1310f, bodyPaint)
        canvas.drawText("stagnant growth, high unemployment, and", 100f, 1360f, bodyPaint)
        canvas.drawText("rapidly escalating consumer inflation.", 100f, 1410f, bodyPaint)

        return bitmap
    }
}

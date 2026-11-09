package com.alaotach.eli5.assistant

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.max
import kotlin.math.min

class CircleSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var screenshot: Bitmap? = null
    private var isAnalyzing = false

    private val touchPath = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE
    private var hasDrawn = false

    // Outer glow paint
    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        color = Color.parseColor("#998B5CF6") // Translucent Neon Purple
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Core neon line paint
    private val coreLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#FFEDE9FE") // Bright Lavender / White-Violet
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Bounding box highlight paint (after release)
    private val boundingBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#8B5CF6")
        pathEffect = DashPathEffect(floatArrayOf(20f, 12f), 0f)
    }

    // Dimming overlay paint
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#66000000") // 40% translucent black
    }

    // Clear mode paint to punch out the selected region
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Top Header & instructions pill paint
    private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E61E1B4B") // Deep dark violet
        style = Paint.Style.FILL
    }

    private val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#668B5CF6")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4B5FD")
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private var selectionRect: RectF? = null
    private var pulseAlpha = 255
    private var pulseAnimator: ValueAnimator? = null

    // Callbacks
    var onSelectionCompleted: ((Bitmap, Rect) -> Unit)? = null
    var onDismissRequested: (() -> Unit)? = null

    init {
        // Prepare pulse animation for processing state
        pulseAnimator = ValueAnimator.ofInt(120, 255).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                pulseAlpha = it.animatedValue as Int
                invalidate()
            }
        }
    }

    fun setScreenshot(bitmap: Bitmap?) {
        this.screenshot = bitmap
        resetSelection()
        invalidate()
    }

    fun resetSelection() {
        isAnalyzing = false
        touchPath.reset()
        selectionRect = null
        minX = Float.MAX_VALUE
        minY = Float.MAX_VALUE
        maxX = Float.MIN_VALUE
        maxY = Float.MIN_VALUE
        hasDrawn = false
        pulseAnimator?.cancel()
        invalidate()
    }

    private fun triggerHaptic(type: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Vibrator::class.java)
                val effect = when (type) {
                    0 -> VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
                    1 -> VibrationEffect.createOneShot(25, 200)
                    else -> VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator?.vibrate(effect)
            } else {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isAnalyzing) return true

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // If tapped the close area or outside after selection
                if (y < 220f && x > width - 200f) {
                    onDismissRequested?.invoke()
                    return true
                }

                resetSelection()
                touchPath.moveTo(x, y)
                lastX = x
                lastY = y
                minX = x
                minY = y
                maxX = x
                maxY = y
                hasDrawn = true
                triggerHaptic(0)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = kotlin.math.abs(x - lastX)
                val dy = kotlin.math.abs(y - lastY)
                if (dx >= 4f || dy >= 4f) {
                    touchPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                    lastX = x
                    lastY = y

                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)

                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!hasDrawn) return true

                val rectWidth = maxX - minX
                val rectHeight = maxY - minY

                // If user just tapped without dragging, dismiss or clear
                if (rectWidth < 40f && rectHeight < 40f) {
                    resetSelection()
                    onDismissRequested?.invoke()
                    return true
                }

                // Add padding around selection
                val padding = 24f
                val left = max(0f, minX - padding)
                val top = max(0f, minY - padding)
                val right = min(width.toFloat(), maxX + padding)
                val bottom = min(height.toFloat(), maxY + padding)

                val finalRect = RectF(left, top, right, bottom)
                selectionRect = finalRect
                isAnalyzing = true
                triggerHaptic(1)
                pulseAnimator?.start()
                invalidate()

                // Crop bitmap and pass to listener
                processSelection(finalRect)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                resetSelection()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun processSelection(rectF: RectF) {
        val bmp = screenshot ?: return

        // Scale rect from View coordinates to actual Bitmap coordinates
        val scaleX = bmp.width.toFloat() / width.toFloat()
        val scaleY = bmp.height.toFloat() / height.toFloat()

        val cropX = max(0, (rectF.left * scaleX).toInt())
        val cropY = max(0, (rectF.top * scaleY).toInt())
        val cropW = min(bmp.width - cropX, ((rectF.width()) * scaleX).toInt())
        val cropH = min(bmp.height - cropY, ((rectF.height()) * scaleY).toInt())

        if (cropW > 10 && cropH > 10) {
            try {
                val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropW, cropH)
                val androidRect = Rect(cropX, cropY, cropX + cropW, cropY + cropH)
                post {
                    onSelectionCompleted?.invoke(cropped, androidRect)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resetSelection()
            }
        } else {
            resetSelection()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw the screenshot if available
        screenshot?.let {
            val destRect = Rect(0, 0, width, height)
            canvas.drawBitmap(it, null, destRect, null)
        }

        // 2. Dim the entire screen
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // 3. Draw active drawing path
        if (hasDrawn && !touchPath.isEmpty) {
            canvas.drawPath(touchPath, outerGlowPaint)
            canvas.drawPath(touchPath, coreLinePaint)
        }

        // 4. Draw bounding box highlight if analyzing
        selectionRect?.let { rect ->
            boundingBoxPaint.alpha = pulseAlpha
            canvas.drawRoundRect(rect, 20f, 20f, boundingBoxPaint)
        }

        // 5. Draw Top Instruction Pill
        drawInstructionPill(canvas)
    }

    private fun drawInstructionPill(canvas: Canvas) {
        val pillWidth = width * 0.85f
        val pillHeight = 120f
        val pillLeft = (width - pillWidth) / 2
        val pillTop = 80f
        val pillRight = pillLeft + pillWidth
        val pillBottom = pillTop + pillHeight
        val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)

        // Background
        canvas.drawRoundRect(pillRect, 60f, 60f, pillBgPaint)
        canvas.drawRoundRect(pillRect, 60f, 60f, pillBorderPaint)

        // Text
        val centerX = width / 2f
        if (isAnalyzing) {
            textPaint.color = Color.parseColor("#A78BFA")
            canvas.drawText("✨ Analyzing with ELI5...", centerX, pillTop + 65f, textPaint)
            canvas.drawText("Reading text on screen", centerX, pillTop + 102f, subTextPaint)
        } else {
            textPaint.color = Color.WHITE
            canvas.drawText("⚡ Circle to ELI5", centerX, pillTop + 60f, textPaint)
            canvas.drawText("Circle or box any text to explain it", centerX, pillTop + 98f, subTextPaint)
        }

        // Close '✕' indicator on top-right
        val closeCenterX = pillRight - 60f
        val closeCenterY = pillTop + 60f
        textPaint.color = Color.parseColor("#A78BFA")
        canvas.drawText("✕", closeCenterX, closeCenterY + 14f, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}

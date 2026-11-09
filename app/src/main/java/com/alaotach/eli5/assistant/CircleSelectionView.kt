package com.alaotach.eli5.assistant

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class CircleSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp: Float get() = context.resources.displayMetrics.density

    private var screenshot: Bitmap? = null
    private var isAnalyzing = false
    private var isCircleGesture = false

    private val touchPath = Path()
    private val touchPoints = mutableListOf<PointF>()
    private var lastX = 0f
    private var lastY = 0f
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE
    private var hasDrawn = false

    // Signature ELI5 Luminous Neon Stroke
    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f * dp
        color = Color.parseColor("#808B5CF6") // 50% Translucent Neon Purple Glow
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val coreLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * dp
        color = Color.parseColor("#FFF5F3FF") // Crisp White with faint Lavender warmth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Touch anchor orb at start point
    private val orbCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        setShadowLayer(6f * dp, 0f, 2f * dp, Color.parseColor("#668B5CF6"))
    }

    private val orbGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4D8B5CF6")
    }

    // Background Dimming Scrim
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#59000000") // 35% translucent dark scrim
    }

    // Clear mode paint to punch out the selected region inside the circle
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // ELI5 Floating Island Header
    private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F20F172A") // Deep frosted slate / dark violet
        style = Paint.Style.FILL
        setShadowLayer(16f * dp, 0f, 6f * dp, Color.parseColor("#80000000"))
    }

    private val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#668B5CF6") // Neon violet border
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * dp
    }

    private val pillIconBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C3AED") // Vibrant violet badge
        style = Paint.Style.FILL
    }

    private val pillIconTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f * dp
        textAlign = Paint.Align.CENTER
    }

    private val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14.5f * dp
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val subtitleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4B5FD") // Light lavender
        textSize = 11.5f * dp
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val closeButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4B5FD")
        textSize = 18f * dp
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var selectionRect: RectF? = null
    private var pulseAlpha = 255
    private var pulseAnimator: ValueAnimator? = null

    // Callbacks: (croppedBitmap, localPathWithinBitmap?, boundingBox)
    var onSelectionCompleted: ((Bitmap, Path?, Rect) -> Unit)? = null
    var onDismissRequested: (() -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        pulseAnimator = ValueAnimator.ofInt(130, 255).apply {
            duration = 750
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
        isCircleGesture = false
        touchPath.reset()
        touchPoints.clear()
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

        val pillHeight = 56f * dp
        val pillWidth = min(width * 0.88f, 380f * dp)
        val pillLeft = (width - pillWidth) / 2f
        val pillTop = 48f * dp
        val pillRight = pillLeft + pillWidth
        val pillBottom = pillTop + pillHeight
        val centerY = pillTop + (pillHeight / 2f)
        val closeCenterX = pillRight - 24f * dp

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if tapped the '✕' close button
                val distToClose = hypot((x - closeCenterX).toDouble(), (y - centerY).toDouble())
                if (distToClose <= 28f * dp) {
                    onDismissRequested?.invoke()
                    return true
                }

                // Check if tapped the pill body
                if (x in pillLeft..pillRight && y in pillTop..pillBottom) {
                    return true
                }

                resetSelection()
                touchPath.moveTo(x, y)
                touchPoints.add(PointF(x, y))
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
                    touchPoints.add(PointF(x, y))
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
                if (!hasDrawn || touchPoints.size < 2) {
                    // Tapped outside without drawing: dismiss
                    onDismissRequested?.invoke()
                    return true
                }

                val rectWidth = maxX - minX
                val rectHeight = maxY - minY

                // If user just tapped or tiny drag, dismiss
                if (rectWidth < 40f && rectHeight < 40f) {
                    resetSelection()
                    onDismissRequested?.invoke()
                    return true
                }

                // Determine if gesture is a circle/lasso
                val firstPt = touchPoints.first()
                val lastPt = touchPoints.last()
                val directDist = hypot((lastPt.x - firstPt.x).toDouble(), (lastPt.y - firstPt.y).toDouble())

                var totalPathLength = 0.0
                for (i in 1 until touchPoints.size) {
                    val p1 = touchPoints[i - 1]
                    val p2 = touchPoints[i]
                    totalPathLength += hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble())
                }

                val diagonal = hypot(rectWidth.toDouble(), rectHeight.toDouble())
                isCircleGesture = directDist < (diagonal * 0.5) || totalPathLength > (diagonal * 1.8)

                val padding = 20f * dp
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

                val localPath = if (isCircleGesture) {
                    val matrix = Matrix().apply {
                        postScale(scaleX, scaleY)
                        postTranslate(-cropX.toFloat(), -cropY.toFloat())
                    }
                    val transformed = Path()
                    touchPath.transform(matrix, transformed)
                    transformed
                } else null

                post {
                    onSelectionCompleted?.invoke(cropped, localPath, androidRect)
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

        // 2. Dim the background with subtle scrim
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // 3. Punch out the selected region inside the user's circle
        if (hasDrawn && touchPoints.size > 2) {
            val closedPath = Path(touchPath).apply { close() }
            canvas.drawPath(closedPath, clearPaint)
        }

        // 4. Draw signature ELI5 glowing stroke & touch orb
        if (hasDrawn && !touchPath.isEmpty) {
            val alphaGlow = if (isAnalyzing) pulseAlpha else 180
            outerGlowPaint.alpha = alphaGlow
            canvas.drawPath(touchPath, outerGlowPaint)

            coreLinePaint.alpha = if (isAnalyzing) pulseAlpha else 255
            canvas.drawPath(touchPath, coreLinePaint)

            // Draw start-point handle orb
            val firstPt = touchPoints.firstOrNull()
            if (firstPt != null) {
                orbGlowPaint.alpha = alphaGlow
                canvas.drawCircle(firstPt.x, firstPt.y, 14f * dp, orbGlowPaint)
                canvas.drawCircle(firstPt.x, firstPt.y, 7f * dp, orbCorePaint)
            }
        }

        // 5. Draw the ELI5 Floating Island Header Pill
        drawInstructionPill(canvas)
    }

    private fun drawInstructionPill(canvas: Canvas) {
        val pillHeight = 56f * dp
        val pillWidth = min(width * 0.88f, 380f * dp)
        val pillLeft = (width - pillWidth) / 2f
        val pillTop = 48f * dp
        val pillRight = pillLeft + pillWidth
        val pillBottom = pillTop + pillHeight
        val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)
        val cornerRadius = 28f * dp

        // Island Background & Border
        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillBgPaint)
        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillBorderPaint)

        val centerY = pillTop + (pillHeight / 2f)

        // Left Icon Badge
        val badgeSize = 36f * dp
        val badgeLeft = pillLeft + 10f * dp
        val badgeRect = RectF(badgeLeft, centerY - (badgeSize / 2f), badgeLeft + badgeSize, centerY + (badgeSize / 2f))
        canvas.drawRoundRect(badgeRect, 18f * dp, 18f * dp, pillIconBadgePaint)

        val iconSymbol = if (isAnalyzing) "✨" else "⚡"
        canvas.drawText(iconSymbol, badgeRect.centerX(), centerY + 5.5f * dp, pillIconTextPaint)

        // Center Title & Subtitle
        val textStartX = badgeLeft + badgeSize + 12f * dp
        if (isAnalyzing) {
            titleTextPaint.color = Color.parseColor("#A78BFA")
            canvas.drawText("Analyzing with ELI5…", textStartX, centerY - 2f * dp, titleTextPaint)
            subtitleTextPaint.color = Color.parseColor("#E2E8F0")
            canvas.drawText("Focusing on circled content", textStartX, centerY + 14f * dp, subtitleTextPaint)
        } else {
            titleTextPaint.color = Color.WHITE
            canvas.drawText("Circle to ELI5", textStartX, centerY - 2f * dp, titleTextPaint)
            subtitleTextPaint.color = Color.parseColor("#C4B5FD")
            canvas.drawText("Circle or box anything to explain", textStartX, centerY + 14f * dp, subtitleTextPaint)
        }

        // Right Close '✕' Button
        val closeCenterX = pillRight - 24f * dp
        canvas.drawText("✕", closeCenterX, centerY + 6f * dp, closeButtonPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}

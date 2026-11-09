package com.alaotach.eli5.assistant

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.alaotach.eli5.R
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

    // Authentic Google Circle to Search vector icons
    private val iconGoogleG = ContextCompat.getDrawable(context, R.drawable.ic_google_g)
    private val iconGoogleMic = ContextCompat.getDrawable(context, R.drawable.ic_google_mic)
    private val iconGoogleLens = ContextCompat.getDrawable(context, R.drawable.ic_google_lens)

    // Google-style luminous white stroke
    private val strokeOuterGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f * dp
        color = Color.parseColor("#4DFFFFFF") // Soft luminous ambient aura
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val strokeCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * dp
        color = Color.WHITE // Pure crisp white core
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Circular start handle / orb (iconic Google Lens touch puck)
    private val orbCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        setShadowLayer(8f * dp, 0f, 2f * dp, Color.parseColor("#4D000000"))
    }

    private val orbGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#33FFFFFF")
    }

    // Base light scrim and gradient shaders
    private val scrimBasePaint = Paint().apply {
        color = Color.parseColor("#26000000") // ~15% subtle translucent black
    }

    private val topVignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bottomVignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Clear mode paint to punch out the selected region inside the circle
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Top Header: Minimal clean glyphs matching Google Circle to Search
    private val headerIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 21f * dp
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        setShadowLayer(8f * dp, 0f, 2f * dp, Color.parseColor("#80000000"))
    }

    private val headerBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 23f * dp
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f * dp, 0f, 2f * dp, Color.parseColor("#80000000"))
    }

    // Bottom Google Search Bar Stadium Capsule
    private val bottomPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(14f * dp, 0f, 3f * dp, Color.parseColor("#33000000"))
    }

    private val bottomPillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#70757A") // Official Google search bar hint color
        textSize = 15f * dp
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val bottomPillAnalyzingTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A73E8") // Google Blue
        textSize = 15f * dp
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private var selectionRect: RectF? = null
    private var pulseAlpha = 255
    private var pulseAnimator: ValueAnimator? = null

    // Callbacks: (croppedBitmap, localPathWithinBitmap?, boundingBox)
    var onSelectionCompleted: ((Bitmap, Path?, Rect) -> Unit)? = null
    var onDismissRequested: (() -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        pulseAnimator = ValueAnimator.ofInt(140, 255).apply {
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            topVignettePaint.shader = LinearGradient(
                0f, 0f, 0f, 150f * dp,
                Color.parseColor("#73000000"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            bottomVignettePaint.shader = LinearGradient(
                0f, h - 170f * dp, 0f, h.toFloat(),
                Color.TRANSPARENT, Color.parseColor("#73000000"),
                Shader.TileMode.CLAMP
            )
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

        val topBarY = 62f * dp
        val closeBtnCx = 32f * dp
        val closeBtnCy = topBarY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if tapped the top-left '✕' button
                val distToClose = hypot((x - closeBtnCx).toDouble(), (y - closeBtnCy).toDouble())
                if (distToClose <= 32f * dp) {
                    onDismissRequested?.invoke()
                    return true
                }

                // Check if tapped the top-right '⋮' button
                val menuBtnCx = width - 32f * dp
                val distToMenu = hypot((x - menuBtnCx).toDouble(), (y - topBarY).toDouble())
                if (distToMenu <= 32f * dp) {
                    onDismissRequested?.invoke()
                    return true
                }

                // Check if tapped the bottom pill area
                val pillHeight = 52f * dp
                val pillBottom = height - 28f * dp
                val pillTop = pillBottom - pillHeight
                if (y in pillTop..pillBottom && x in (20f * dp)..(width - 20f * dp)) {
                    // Tap on bottom bar
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

                isCircleGesture = (totalPathLength > 80.0 && directDist < totalPathLength * 0.65)
                if (isCircleGesture) {
                    touchPath.close()
                }

                // Tight padding: 4px only
                val padding = 4f
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

                processSelection(finalRect, isCircleGesture)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                resetSelection()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun processSelection(rectF: RectF, isCircle: Boolean) {
        val bmp = screenshot ?: return

        val scaleX = bmp.width.toFloat() / width.toFloat()
        val scaleY = bmp.height.toFloat() / height.toFloat()

        val cropX = max(0, (rectF.left * scaleX).toInt())
        val cropY = max(0, (rectF.top * scaleY).toInt())
        val cropW = min(bmp.width - cropX, ((rectF.width()) * scaleX).toInt())
        val cropH = min(bmp.height - cropY, ((rectF.height()) * scaleY).toInt())

        if (cropW > 10 && cropH > 10) {
            try {
                val androidRect = Rect(cropX, cropY, cropX + cropW, cropY + cropH)
                val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropW, cropH)

                val localPath = if (isCircle) {
                    val p = Path(touchPath).apply { close() }
                    val matrix = Matrix().apply {
                        postScale(scaleX, scaleY)
                        postTranslate(-cropX.toFloat(), -cropY.toFloat())
                    }
                    p.transform(matrix)
                    p
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

        // 1. Draw the screenshot underneath
        screenshot?.let {
            val destRect = Rect(0, 0, width, height)
            canvas.drawBitmap(it, null, destRect, null)
        }

        // 2. Cinematic dimming scrim: light base dim + top and bottom soft vignettes
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimBasePaint)
        canvas.drawRect(0f, 0f, width.toFloat(), 150f * dp, topVignettePaint)
        canvas.drawRect(0f, height - 170f * dp, width.toFloat(), height.toFloat(), bottomVignettePaint)

        // 3. Clear the area inside the circle so selected text illuminates brightly!
        if (isAnalyzing && isCircleGesture) {
            canvas.drawPath(touchPath, clearPaint)
        }

        // 4. Draw Google-style glowing white path & handle orb
        if (hasDrawn && !touchPath.isEmpty) {
            val alphaGlow = if (isAnalyzing) (pulseAlpha * 0.6f).toInt() else 120
            strokeOuterGlowPaint.alpha = alphaGlow
            canvas.drawPath(touchPath, strokeOuterGlowPaint)

            strokeCorePaint.alpha = if (isAnalyzing) pulseAlpha else 255
            canvas.drawPath(touchPath, strokeCorePaint)

            // Draw start-point handle orb (iconic Google Lens touch handle)
            val firstPt = touchPoints.firstOrNull()
            if (firstPt != null) {
                orbGlowPaint.alpha = alphaGlow
                canvas.drawCircle(firstPt.x, firstPt.y, 18f * dp, orbGlowPaint)
                canvas.drawCircle(firstPt.x, firstPt.y, 11f * dp, orbCorePaint)
            }
        }

        // 5. Draw minimal floating Header (Google style)
        drawGoogleStyleHeader(canvas)

        // 6. Draw floating bottom capsule search bar (Google style)
        drawGoogleStyleBottomBar(canvas)
    }

    private fun drawGoogleStyleHeader(canvas: Canvas) {
        val topBarY = 62f * dp

        // Top-left Close '✕' minimal icon with soft shadow
        val closeBtnCx = 32f * dp
        canvas.drawText("✕", closeBtnCx, topBarY + 7f * dp, headerIconPaint)

        // Centered Brand Title "Google"
        val centerCx = width / 2f
        canvas.drawText("Google", centerCx, topBarY + 8f * dp, headerBrandPaint)

        // Top-right Menu '⋮' minimal icon with soft shadow
        val menuBtnCx = width - 32f * dp
        canvas.drawText("⋮", menuBtnCx, topBarY + 7f * dp, headerIconPaint)
    }

    private fun drawGoogleStyleBottomBar(canvas: Canvas) {
        val pillHeight = 52f * dp
        val pillMarginHorizontal = 20f * dp
        val pillBottom = height - 28f * dp
        val pillTop = pillBottom - pillHeight
        val pillRect = RectF(pillMarginHorizontal, pillTop, width - pillMarginHorizontal, pillBottom)
        val cornerRadius = 26f * dp

        // Floating White Stadium Pill with Material elevation drop shadow
        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, bottomPillBgPaint)

        val centerY = pillTop + (pillHeight / 2f)

        // 1. Left: Google 'G' 4-color authentic vector icon
        val iconGSize = (24f * dp).toInt()
        val gLeft = (pillMarginHorizontal + 16f * dp).toInt()
        val gTop = (centerY - iconGSize / 2f).toInt()
        iconGoogleG?.setBounds(gLeft, gTop, gLeft + iconGSize, gTop + iconGSize)
        iconGoogleG?.draw(canvas)

        // 2. Right: Google Lens & Google Mic vector icons
        val lensSize = (22f * dp).toInt()
        val lensRight = (width - pillMarginHorizontal - 16f * dp).toInt()
        val lensLeft = lensRight - lensSize
        val lensTop = (centerY - lensSize / 2f).toInt()
        iconGoogleLens?.setBounds(lensLeft, lensTop, lensRight, lensTop + lensSize)
        iconGoogleLens?.draw(canvas)

        val micSize = (22f * dp).toInt()
        val micRight = (lensLeft - 18f * dp).toInt()
        val micLeft = micRight - micSize
        val micTop = (centerY - micSize / 2f).toInt()
        iconGoogleMic?.setBounds(micLeft, micTop, micRight, micTop + micSize)
        iconGoogleMic?.draw(canvas)

        // 3. Center: Subtle Google search hint placeholder
        val textStartX = pillMarginHorizontal + 52f * dp
        if (isAnalyzing) {
            canvas.drawText("Searching…", textStartX, centerY + 5.5f * dp, bottomPillAnalyzingTextPaint)
        } else {
            canvas.drawText("Search or circle screen", textStartX, centerY + 5.5f * dp, bottomPillTextPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}

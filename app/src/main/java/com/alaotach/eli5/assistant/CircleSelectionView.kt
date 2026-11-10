package com.alaotach.eli5.assistant

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Typeface
import android.os.Build
import android.os.Vibrator
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.alaotach.eli5.R
import com.alaotach.eli5.SettingsManager
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

    // Screen Translation state (Google Lens style)
    private var isTranslating = false
    private var isShowingTranslation = false
    private var showOriginalOnly = false
    private var isSharingTranslation = false
    private var translatedBlocks = listOf<TranslatedBlock>()

    private val touchPath = Path()
    private val touchPoints = mutableListOf<PointF>()
    private var lastX = 0f
    private var lastY = 0f
    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE
    private var hasDrawn = false

    // Hit-testing rects
    private val pillRect = RectF()
    private val translateBtnRect = RectF()
    private val toggleOriginalBtnRect = RectF()
    private val shareBtnRect = RectF()
    private val closeBtnRect = RectF()
    private var isInteractingWithPill = false

    // Authentic ELI5 App Logo
    private val eli5Logo = ContextCompat.getDrawable(context, R.drawable.ic_eli5_logo)

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
        color = Color.parseColor("#40000000") // Soft translucent dark scrim
    }

    // ELI5 Floating Island Header - Redesigned Light Purple Surface
    private val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(14f * dp, 0f, 4f * dp, Color.parseColor("#26000000"))
    }

    private val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E9D5FF") // Light lavender border
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * dp
    }

    private val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#21112F") // Deep plum primary text
        textSize = 14f * dp
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val subtitleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#766782") // Muted purple-gray
        textSize = 11f * dp
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val closeButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#766782")
        textSize = 18f * dp
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // Translate Button & Badge Paints
    private val translateBtnBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#EDE9FE") // Lavender pill
    }

    private val translateBtnBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * dp
        color = Color.parseColor("#A855F7")
    }

    private val translateBtnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6A0DAD") // Primary purple
        textSize = 12f * dp
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    // Google Lens Style Overlay Paints
    private val lensBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val lensBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * dp
    }

    private val lensTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private var selectionRect: RectF? = null
    private var pulseAlpha = 255
    private var pulseAnimator: ValueAnimator? = null

    // Callbacks: (croppedBitmap, localPathWithinBitmap?, boundingBox)
    var onSelectionCompleted: ((Bitmap, Path?, Rect) -> Unit)? = null
    var onDismissRequested: (() -> Unit)? = null

    private val targetLanguage: String
        get() = SettingsManager.getLanguage(context)

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
        resetTranslation()
        invalidate()
    }

    fun resetTranslation() {
        isTranslating = false
        isShowingTranslation = false
        showOriginalOnly = false
        isSharingTranslation = false
        translatedBlocks = emptyList()
        pulseAnimator?.cancel()
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

    fun startScreenTranslation() {
        val bmp = screenshot ?: return
        if (isTranslating) return

        isTranslating = true
        isShowingTranslation = false
        showOriginalOnly = false
        triggerHaptic(0)
        pulseAnimator?.start()
        invalidate()

        ScreenTranslator.translateScreen(bmp, targetLanguage) { blocks ->
            isTranslating = false
            pulseAnimator?.cancel()
            if (blocks.isNotEmpty()) {
                translatedBlocks = blocks
                isShowingTranslation = true
                showOriginalOnly = false
                triggerHaptic(1)
            } else {
                Toast.makeText(context, "No text found on screen to translate", Toast.LENGTH_SHORT).show()
            }
            invalidate()
        }
    }

    private fun shareCurrentTranslation() {
        val bmp = screenshot ?: return
        if (translatedBlocks.isEmpty()) return
        if (isSharingTranslation) return
        isSharingTranslation = true
        triggerHaptic(0)
        invalidate()

        com.alaotach.eli5.ShareImageGenerator.shareTranslatedScreen(
            context,
            bmp,
            translatedBlocks,
            targetLanguage,
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
            onShareStarted = {
                isSharingTranslation = false
                // Dismiss overlay so system share popup/sheet is visible on top
                onDismissRequested?.invoke()
            },
            onError = {
                isSharingTranslation = false
                invalidate()
            }
        )
    }

    private fun triggerHaptic(type: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Vibrator::class.java)
                val effect = when (type) {
                    1 -> android.os.VibrationEffect.createOneShot(35, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    else -> android.os.VibrationEffect.createOneShot(15, 80)
                }
                vibrator?.vibrate(effect)
            } else {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (_: Exception) {}
    }

    private fun isPointInExpandedRect(rect: RectF, x: Float, y: Float, paddingDp: Float = 10f): Boolean {
        if (rect.isEmpty) return false
        val pad = paddingDp * dp
        return x >= rect.left - pad && x <= rect.right + pad &&
                y >= rect.top - pad && y <= rect.bottom + pad
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isInteractingWithPill = false

                // 1. Check if tapped the '✕' close button
                if (isPointInExpandedRect(closeBtnRect, x, y, 12f)) {
                    isInteractingWithPill = true
                    if (isShowingTranslation) {
                        resetTranslation()
                    } else {
                        onDismissRequested?.invoke()
                    }
                    return true
                }

                // 2. Check if in translated mode and tapped the Original/Translated toggle chip
                if (isShowingTranslation && isPointInExpandedRect(toggleOriginalBtnRect, x, y, 8f)) {
                    isInteractingWithPill = true
                    showOriginalOnly = !showOriginalOnly
                    triggerHaptic(0)
                    invalidate()
                    return true
                }

                // 2b. Check if in translated mode and tapped the Share button
                if (isShowingTranslation && isPointInExpandedRect(shareBtnRect, x, y, 8f)) {
                    isInteractingWithPill = true
                    shareCurrentTranslation()
                    return true
                }

                // 3. Check if tapped the Translate button
                if (!isShowingTranslation && !isTranslating && isPointInExpandedRect(translateBtnRect, x, y, 8f)) {
                    isInteractingWithPill = true
                    startScreenTranslation()
                    return true
                }

                // 4. Check if tapped inside the floating pill body
                if (pillRect.contains(x, y)) {
                    isInteractingWithPill = true
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
                if (isInteractingWithPill) return true

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
                if (isInteractingWithPill) {
                    isInteractingWithPill = false
                    return true
                }

                // Never dismiss if currently translating
                if (isTranslating) {
                    resetSelection()
                    invalidate()
                    return true
                }

                if (!hasDrawn || touchPoints.size < 2) {
                    // Tapped outside without drawing: dismiss if not viewing translation
                    if (!isShowingTranslation) {
                        onDismissRequested?.invoke()
                    }
                    return true
                }

                val rectWidth = maxX - minX
                val rectHeight = maxY - minY

                // If user just tapped or tiny drag, dismiss if not viewing translation
                if (rectWidth < 40f && rectHeight < 40f) {
                    resetSelection()
                    if (!isShowingTranslation) {
                        onDismissRequested?.invoke()
                    }
                    return true
                }

                // Add margin around selected area
                val margin = 20f * dp
                val left = max(0f, minX - margin)
                val top = max(0f, minY - margin)
                val right = min(width.toFloat(), maxX + margin)
                val bottom = min(height.toFloat(), maxY + margin)
                selectionRect = RectF(left, top, right, bottom)

                val startPt = touchPoints.first()
                val endPt = touchPoints.last()
                val dist = hypot((endPt.x - startPt.x).toDouble(), (endPt.y - startPt.y).toDouble())
                isCircleGesture = dist < (rectWidth * 0.45)

                isAnalyzing = true
                pulseAnimator?.start()
                triggerHaptic(1)
                invalidate()

                postDelayed({
                    completeSelection(selectionRect!!)
                }, 150)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isInteractingWithPill = false
                resetSelection()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun completeSelection(cropRect: RectF) {
        val bmp = screenshot
        if (bmp != null) {
            try {
                val scaleX = bmp.width.toFloat() / width.toFloat()
                val scaleY = bmp.height.toFloat() / height.toFloat()

                val cropX = (cropRect.left * scaleX).toInt().coerceIn(0, bmp.width - 1)
                val cropY = (cropRect.top * scaleY).toInt().coerceIn(0, bmp.height - 1)
                val cropW = (cropRect.width() * scaleX).toInt().coerceAtMost(bmp.width - cropX).coerceAtLeast(1)
                val cropH = (cropRect.height() * scaleY).toInt().coerceAtMost(bmp.height - cropY).coerceAtLeast(1)

                val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropW, cropH)
                val androidRect = Rect(cropX, cropY, cropX + cropW, cropY + cropH)

                // Build localized path inside the cropped bitmap coordinate space
                val localPath = Path().apply {
                    val m = android.graphics.Matrix().apply {
                        postTranslate(-cropRect.left, -cropRect.top)
                        postScale(scaleX, scaleY)
                    }
                    touchPath.transform(m, this)
                }

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

        // 2. Draw Google Lens style Translated Text Overlays on top of original text
        if (isShowingTranslation && !showOriginalOnly && screenshot != null) {
            val bmp = screenshot!!
            val scaleX = width.toFloat() / bmp.width
            val scaleY = height.toFloat() / bmp.height

            for (block in translatedBlocks) {
                val bLeft = block.bounds.left * scaleX - 3f * dp
                val bTop = block.bounds.top * scaleY - 2.5f * dp
                val bRight = block.bounds.right * scaleX + 3f * dp
                val bBottom = block.bounds.bottom * scaleY + 2.5f * dp
                val blockRect = RectF(bLeft, bTop, bRight, bBottom)

                // Draw background badge covering original text with sampled solid color
                val solidBg = Color.rgb(Color.red(block.bgColor), Color.green(block.bgColor), Color.blue(block.bgColor))
                lensBgPaint.color = solidBg
                lensBgPaint.alpha = 248 // 97% opacity for clean coverage like Google Lens
                canvas.drawRoundRect(blockRect, 6f * dp, 6f * dp, lensBgPaint)

                // Subtle contrasting border
                lensBorderPaint.color = Color.argb(45, Color.red(block.textColor), Color.green(block.textColor), Color.blue(block.textColor))
                canvas.drawRoundRect(blockRect, 6f * dp, 6f * dp, lensBorderPaint)

                // Format & render translated text inside the box
                val padH = 4f * dp
                val padV = 2f * dp
                val availW = max(12f * dp, blockRect.width() - (padH * 2))
                val availH = max(10f * dp, blockRect.height() - (padV * 2))

                val optimalSize = ScreenTranslator.calculateOptimalTextSize(
                    block.translatedText,
                    availW,
                    availH,
                    dp,
                    lensTextPaint
                )
                lensTextPaint.textSize = optimalSize
                lensTextPaint.color = block.textColor

                val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(block.translatedText, 0, block.translatedText.length, lensTextPaint, max(1, availW.toInt()))
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.15f)
                        .setIncludePad(false)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(block.translatedText, lensTextPaint, max(1, availW.toInt()), Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
                }

                canvas.save()
                val offsetY = if (layout.height < availH) (availH - layout.height) / 2f else 0f
                canvas.translate(blockRect.left + padH, blockRect.top + padV + offsetY)
                layout.draw(canvas)
                canvas.restore()
            }
        }

        // 3. Dim the background with subtle scrim during circle selection
        if (hasDrawn && touchPoints.size > 2) {
            canvas.save()
            val closedPath = Path(touchPath).apply { close() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(closedPath)
            } else {
                @Suppress("DEPRECATION")
                canvas.clipPath(closedPath, Region.Op.DIFFERENCE)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
            canvas.restore()
        } else if (!isShowingTranslation) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
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
        val pillHeight = 58f * dp
        val pillWidth = min(width * 0.94f, 410f * dp)
        val pillLeft = (width - pillWidth) / 2f
        val pillTop = 48f * dp
        val pillRight = pillLeft + pillWidth
        val pillBottom = pillTop + pillHeight
        pillRect.set(pillLeft, pillTop, pillRight, pillBottom)
        val cornerRadius = 29f * dp

        val isDark = SettingsManager.isDarkTheme(context)

        // Dynamic theme persistence
        pillBgPaint.color = if (isDark) Color.parseColor("#1C1826") else Color.WHITE
        pillBorderPaint.color = if (isDark) Color.parseColor("#35234E") else Color.parseColor("#E9D5FF")
        closeButtonPaint.color = if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782")

        // Island Background & Border
        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillBgPaint)
        canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillBorderPaint)

        val centerY = pillTop + (pillHeight / 2f)

        // Close '✕' Button on far right
        val closeCenterX = pillRight - 22f * dp
        closeBtnRect.set(closeCenterX - 20f * dp, centerY - 20f * dp, closeCenterX + 20f * dp, centerY + 20f * dp)
        canvas.drawText("✕", closeCenterX, centerY + 6f * dp, closeButtonPaint)

        // Left ELI5 App Logo
        val logoSize = 32f * dp
        val logoLeft = pillLeft + 12f * dp
        val logoTop = centerY - (logoSize / 2f)
        val logoRight = logoLeft + logoSize
        val logoBottom = logoTop + logoSize
        eli5Logo?.setBounds(logoLeft.toInt(), logoTop.toInt(), logoRight.toInt(), logoBottom.toInt())
        eli5Logo?.draw(canvas)

        val textStartX = logoRight + 10f * dp

        when {
            isShowingTranslation -> {
                // Translation Result State:
                // Show language badge and Original / Translated toggle chip
                titleTextPaint.color = Color.parseColor("#A78BFA")
                canvas.drawText(targetLanguage, textStartX, centerY - 2f * dp, titleTextPaint)
                subtitleTextPaint.color = if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782")
                val blockCountText = "${translatedBlocks.size} blocks translated"
                canvas.drawText(blockCountText, textStartX, centerY + 13f * dp, subtitleTextPaint)

                // Toggle chip: [Show Original] vs [Show Translated] (no emojis)
                val toggleWidth = 92f * dp
                val toggleHeight = 32f * dp
                val toggleRight = closeBtnRect.left - 6f * dp
                val toggleLeft = toggleRight - toggleWidth
                val toggleTop = centerY - (toggleHeight / 2f)
                val toggleBottom = centerY + (toggleHeight / 2f)
                toggleOriginalBtnRect.set(toggleLeft, toggleTop, toggleRight, toggleBottom)

                translateBtnBgPaint.color = if (isDark) Color.parseColor("#261D3B") else Color.parseColor("#EDE9FE")
                canvas.drawRoundRect(toggleOriginalBtnRect, 16f * dp, 16f * dp, translateBtnBgPaint)
                translateBtnBorderPaint.color = if (isDark) Color.parseColor("#8B5CF6") else Color.parseColor("#A855F7")
                canvas.drawRoundRect(toggleOriginalBtnRect, 16f * dp, 16f * dp, translateBtnBorderPaint)

                translateBtnTextPaint.textSize = 11.5f * dp
                translateBtnTextPaint.color = if (isDark) Color.parseColor("#DDD6FE") else Color.parseColor("#6A0DAD")
                val toggleLabel = if (showOriginalOnly) "Translated" else "Original"
                canvas.drawText(toggleLabel, toggleOriginalBtnRect.centerX(), centerY + 4.5f * dp, translateBtnTextPaint)

                // Share button: [Share] / [Sharing…] (no emojis)
                val shareWidth = if (isSharingTranslation) 74f * dp else 64f * dp
                val shareRight = toggleLeft - 6f * dp
                val shareLeft = shareRight - shareWidth
                shareBtnRect.set(shareLeft, toggleTop, shareRight, toggleBottom)

                translateBtnBgPaint.color = if (isDark) Color.parseColor("#261D3B") else Color.parseColor("#EDE9FE")
                canvas.drawRoundRect(shareBtnRect, 16f * dp, 16f * dp, translateBtnBgPaint)
                translateBtnBorderPaint.color = if (isDark) Color.parseColor("#8B5CF6") else Color.parseColor("#A855F7")
                canvas.drawRoundRect(shareBtnRect, 16f * dp, 16f * dp, translateBtnBorderPaint)

                val shareLabel = if (isSharingTranslation) "Sharing…" else "Share"
                canvas.drawText(shareLabel, shareBtnRect.centerX(), centerY + 4.5f * dp, translateBtnTextPaint)
            }

            isTranslating -> {
                // Translating in-progress state
                titleTextPaint.color = Color.parseColor("#A78BFA")
                canvas.drawText("Translating screen…", textStartX, centerY - 2f * dp, titleTextPaint)
                subtitleTextPaint.color = if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782")
                canvas.drawText("Translating to $targetLanguage", textStartX, centerY + 13f * dp, subtitleTextPaint)

                // Translating progress capsule (no emojis)
                val transPillW = 86f * dp
                val transPillH = 32f * dp
                val transRight = closeBtnRect.left - 8f * dp
                val transLeft = transRight - transPillW
                val transRect = RectF(transLeft, centerY - transPillH / 2f, transRight, centerY + transPillH / 2f)

                translateBtnBgPaint.color = Color.argb((pulseAlpha * 0.35f).toInt(), 139, 92, 246)
                canvas.drawRoundRect(transRect, 16f * dp, 16f * dp, translateBtnBgPaint)
                translateBtnBorderPaint.color = Color.argb(pulseAlpha, 167, 139, 250)
                canvas.drawRoundRect(transRect, 16f * dp, 16f * dp, translateBtnBorderPaint)

                translateBtnTextPaint.textSize = 11.5f * dp
                translateBtnTextPaint.color = Color.WHITE
                canvas.drawText("Translating…", transRect.centerX(), centerY + 4.5f * dp, translateBtnTextPaint)
            }

            isAnalyzing -> {
                // Circling analysis in-progress
                titleTextPaint.color = Color.parseColor("#A78BFA")
                canvas.drawText("Analyzing with ELI5…", textStartX, centerY - 2f * dp, titleTextPaint)
                subtitleTextPaint.color = if (isDark) Color.parseColor("#9E9AA8") else Color.parseColor("#766782")
                canvas.drawText("Focusing on circled content", textStartX, centerY + 13f * dp, subtitleTextPaint)
            }

            else -> {
                // Normal Circle to Explain State:
                // Prominent title & clear, legible "Circle anything to explain" subtitle
                titleTextPaint.color = if (isDark) Color.WHITE else Color.parseColor("#21112F")
                titleTextPaint.textSize = 14.5f * dp
                canvas.drawText("Circle to ELI5", textStartX, centerY - 2.5f * dp, titleTextPaint)
                subtitleTextPaint.color = if (isDark) Color.parseColor("#C4B5FD") else Color.parseColor("#6A0DAD")
                subtitleTextPaint.textSize = 12f * dp
                subtitleTextPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                canvas.drawText("Circle anything to explain", textStartX, centerY + 13.5f * dp, subtitleTextPaint)

                // Translate Button Capsule (clean text without emojis)
                val btnW = 86f * dp
                val btnH = 32f * dp
                val btnRight = closeBtnRect.left - 6f * dp
                val btnLeft = btnRight - btnW
                val btnTop = centerY - (btnH / 2f)
                val btnBottom = centerY + (btnH / 2f)
                translateBtnRect.set(btnLeft, btnTop, btnRight, btnBottom)

                translateBtnBgPaint.color = if (isDark) Color.parseColor("#261D3B") else Color.parseColor("#EDE9FE")
                canvas.drawRoundRect(translateBtnRect, 16f * dp, 16f * dp, translateBtnBgPaint)
                translateBtnBorderPaint.color = if (isDark) Color.parseColor("#8B5CF6") else Color.parseColor("#A855F7")
                canvas.drawRoundRect(translateBtnRect, 16f * dp, 16f * dp, translateBtnBorderPaint)

                translateBtnTextPaint.textSize = 12f * dp
                translateBtnTextPaint.color = if (isDark) Color.parseColor("#DDD6FE") else Color.parseColor("#6A0DAD")
                canvas.drawText("Translate", translateBtnRect.centerX(), centerY + 4.5f * dp, translateBtnTextPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}

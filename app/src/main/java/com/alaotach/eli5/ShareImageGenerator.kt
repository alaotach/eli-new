package com.alaotach.eli5

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap

object ShareImageGenerator {
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.alaotach.eli5"

    fun generateQrBitmap(url: String, size: Int): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
        }
        val bitMatrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size, hints)
        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)
        val darkColor = Color.parseColor("#161224")
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    private fun cleanMarkdown(text: String): String {
        return text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
            .trim()
    }

    suspend fun createShareImage(
        context: Context,
        originalText: String,
        explanationText: String,
        mode: String
    ): File = withContext(Dispatchers.Default) {
        val cardWidth = 1080
        val padding = 64
        val contentWidth = cardWidth - (padding * 2)

        val cleanExplanation = cleanMarkdown(explanationText)
        val cleanQuote = cleanMarkdown(originalText).replace('\n', ' ')

        // 1. Setup Paints
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9AA8")
            textSize = 24f
            typeface = Typeface.DEFAULT
        }

        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A8A2B5")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val footerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val footerSubtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9AA8")
            textSize = 22f
            typeface = Typeface.DEFAULT
        }

        val footerCtaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A78BFA")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // 2. Measure Heights via StaticLayout
        val quoteDisplay = if (cleanQuote.length > 200) {
            "“${cleanQuote.take(200)}...”"
        } else {
            "“$cleanQuote”"
        }
        val quoteLayout = createStaticLayout(quoteDisplay, quotePaint, contentWidth - 36)
        val bodyLayout = createStaticLayout(cleanExplanation, bodyPaint, contentWidth)

        val headerHeight = 90
        val quoteSectionHeight = if (cleanQuote.isNotBlank()) quoteLayout.height + 40 else 0
        val bodySectionHeight = bodyLayout.height + 48
        val qrSize = 150
        val footerSectionHeight = qrSize + 40

        val totalHeight = padding + headerHeight + quoteSectionHeight + bodySectionHeight + footerSectionHeight + padding

        // 3. Create Bitmap & Canvas
        val bitmap = Bitmap.createBitmap(cardWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Gradient: Deep obsidian purple
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, cardWidth.toFloat(), totalHeight.toFloat(),
                Color.parseColor("#130F1F"),
                Color.parseColor("#1D172E"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, cardWidth.toFloat(), totalHeight.toFloat(), bgPaint)

        // Subtle ambient accent glow at top right
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                cardWidth.toFloat() - 100f, 100f, 400f,
                Color.parseColor("#2E1F54"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cardWidth.toFloat() - 100f, 100f, 400f, glowPaint)

        // Outer border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4C3D70")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(16f, 16f, cardWidth - 16f, totalHeight - 16f), 32f, 32f, borderPaint)

        var currentY = padding.toFloat()

        // --- HEADER ---
        // Logo icon box
        val iconRect = RectF(padding.toFloat(), currentY, padding + 72f, currentY + 72f)
        val iconBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7C3AED")
        }
        canvas.drawRoundRect(iconRect, 18f, 18f, iconBoxPaint)

        val iconTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("eli5", iconRect.centerX(), iconRect.centerY() + 10f, iconTextPaint)

        // Brand titles
        val textStartX = padding + 92f
        canvas.drawText("ELI5", textStartX, currentY + 40f, titlePaint)
        canvas.drawText("Explain Like I'm 5", textStartX, currentY + 70f, subtitlePaint)

        // Mode badge on right
        val modeText = "${mode.uppercase()} EXPLANATION"
        val modeWidth = badgePaint.measureText(modeText)
        val badgeRect = RectF(
            cardWidth - padding - modeWidth - 36f,
            currentY + 12f,
            cardWidth - padding.toFloat(),
            currentY + 60f
        )
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38295E")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 14f, 14f, badgeBgPaint)
        canvas.drawText(modeText, badgeRect.left + 18f, badgeRect.centerY() + 8f, badgePaint)

        currentY += headerHeight + 20f

        // --- ORIGINAL TEXT QUOTE (if present) ---
        if (cleanQuote.isNotBlank()) {
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#7C3AED")
                strokeWidth = 6f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(
                padding + 4f,
                currentY + 6f,
                padding + 4f,
                currentY + quoteLayout.height - 4f,
                barPaint
            )

            canvas.save()
            canvas.translate(padding + 26f, currentY)
            quoteLayout.draw(canvas)
            canvas.restore()

            currentY += quoteSectionHeight
        }

        // --- EXPLANATION BODY ---
        canvas.save()
        canvas.translate(padding.toFloat(), currentY)
        bodyLayout.draw(canvas)
        canvas.restore()

        currentY += bodySectionHeight

        // --- FOOTER DIVIDER ---
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E2544")
            strokeWidth = 2f
        }
        canvas.drawLine(padding.toFloat(), currentY, (cardWidth - padding).toFloat(), currentY, dividerPaint)
        currentY += 28f

        // --- FOOTER & QR CODE ---
        // Left Column: Branding & Tagline
        canvas.drawText("ELI5 for Android", padding.toFloat(), currentY + 40f, footerTitlePaint)
        canvas.drawText("Instant, clear explanations in any app.", padding.toFloat(), currentY + 74f, footerSubtitlePaint)
        canvas.drawText("Scan QR to get it on Google Play →", padding.toFloat(), currentY + 112f, footerCtaPaint)

        // Right Column: QR Code Card
        val qrBitmap = generateQrBitmap(PLAY_STORE_URL, qrSize)
        val qrCardRect = RectF(
            cardWidth - padding - qrSize - 16f,
            currentY,
            cardWidth - padding.toFloat(),
            currentY + qrSize + 16f
        )
        val qrCardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawRoundRect(qrCardRect, 16f, 16f, qrCardBgPaint)
        canvas.drawBitmap(qrBitmap, qrCardRect.left + 8f, qrCardRect.top + 8f, null)

        // 4. Save to cache
        val sharedDir = File(context.cacheDir, "shared")
        if (!sharedDir.exists()) sharedDir.mkdirs()
        val imageFile = File(sharedDir, "eli5_share_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        imageFile.setReadable(true, false)

        imageFile
    }

    private fun grantPermissionsToReceivers(context: Context, shareIntent: Intent, uri: Uri) {
        try {
            val resolveInfoList = context.packageManager.queryIntentActivities(
                shareIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resolveInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        } catch (_: Exception) {}

        // Fallback explicit grant to known messaging/social apps in case queries didn't resolve them
        val knownPackages = listOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "com.google.android.apps.messaging",
            "com.facebook.orca",
            "com.instagram.android",
            "com.twitter.android"
        )
        for (pkg in knownPackages) {
            try {
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
        }
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.35f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                paint,
                width,
                Layout.Alignment.ALIGN_NORMAL,
                1.35f,
                0f,
                false
            )
        }
    }

    fun shareExplanation(
        context: Context,
        originalText: String,
        explanationText: String,
        mode: String,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        if (explanationText.isBlank()) {
            Toast.makeText(context, "No explanation to share", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Generating share image...", Toast.LENGTH_SHORT).show()

        coroutineScope.launch(Dispatchers.Main) {
            try {
                val imageFile = createShareImage(context, originalText, explanationText, mode)
                val imageUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )

                val caption = "Explained with ELI5: $PLAY_STORE_URL"

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    clipData = ClipData.newRawUri("ELI5 Explanation", imageUri)
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    putExtra(Intent.EXTRA_SUBJECT, "ELI5 Explanation")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                grantPermissionsToReceivers(context, shareIntent, imageUri)

                val chooserIntent = Intent.createChooser(shareIntent, "Share ELI5 Explanation").apply {
                    clipData = ClipData.newRawUri("ELI5 Explanation", imageUri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooserIntent)
            } catch (e: Exception) {
                // Fallback to text if image generation encounters any issue
                val textFallback = "$explanationText\n\nExplained with ELI5: $PLAY_STORE_URL"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textFallback)
                }
                val chooserIntent = Intent.createChooser(shareIntent, "Share ELI5 Explanation").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooserIntent)
            }
        }
    }

    suspend fun createTranslatedShareImage(
        context: Context,
        screenshot: Bitmap,
        blocks: List<com.alaotach.eli5.assistant.TranslatedBlock>,
        targetLanguage: String
    ): File = withContext(Dispatchers.Default) {
        val width = screenshot.width
        val height = screenshot.height
        val dp = width / 390f
        val bannerHeight = (68f * dp).toInt().coerceIn(160, 240)

        val resultBitmap = Bitmap.createBitmap(width, height + bannerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // 1. Draw base screenshot
        canvas.drawBitmap(screenshot, 0f, 0f, null)

        // 2. Draw translated overlays
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * dp
        }
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val padding = 4f * dp
        val cornerRadius = 6f * dp

        for (block in blocks) {
            val r = block.bounds
            val rect = RectF(
                r.left.toFloat() - padding,
                r.top.toFloat() - padding,
                r.right.toFloat() + padding,
                r.bottom.toFloat() + padding
            )

            // Background card with 97% opacity
            bgPaint.color = block.bgColor
            bgPaint.alpha = 248
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

            // Border
            val isDarkCard = Color.luminance(block.bgColor) < 0.5f
            borderPaint.color = if (isDarkCard) Color.argb(80, 255, 255, 255) else Color.argb(60, 0, 0, 0)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

            // Text
            textPaint.color = block.textColor
            val availW = rect.width() - (padding * 2)
            val availH = rect.height() - (padding * 2)
            val optimalSize = com.alaotach.eli5.assistant.ScreenTranslator.calculateOptimalTextSize(
                block.translatedText,
                availW,
                availH,
                dp,
                textPaint
            )
            textPaint.textSize = optimalSize

            val widthInt = maxOf(1, availW.toInt())
            val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(block.translatedText, 0, block.translatedText.length, textPaint, widthInt)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(block.translatedText, textPaint, widthInt, Layout.Alignment.ALIGN_NORMAL, 1.15f, 0f, false)
            }

            canvas.save()
            val textY = rect.centerY() - (layout.height / 2f)
            canvas.translate(rect.left + padding, textY)
            layout.draw(canvas)
            canvas.restore()
        }

        // 3. Draw Branded Advertisement Footer Banner
        val bannerTop = height.toFloat()
        val bannerBottom = (height + bannerHeight).toFloat()
        val bannerRect = RectF(0f, bannerTop, width.toFloat(), bannerBottom)

        // Gradient dark background
        val bannerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, bannerTop, width.toFloat(), bannerBottom,
                intArrayOf(Color.parseColor("#0F172A"), Color.parseColor("#1E1B4B")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(bannerRect, bannerBgPaint)

        // Top neon separator line
        val neonLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8B5CF6")
            strokeWidth = 3f * dp
        }
        canvas.drawLine(0f, bannerTop, width.toFloat(), bannerTop, neonLinePaint)

        // Logo
        val logoDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_eli5_logo)
        val logoSize = (36f * dp).toInt()
        val logoLeft = (16f * dp).toInt()
        val logoTopPos = (bannerTop + (bannerHeight - logoSize) / 2f).toInt()
        logoDrawable?.setBounds(logoLeft, logoTopPos, logoLeft + logoSize, logoTopPos + logoSize)
        logoDrawable?.draw(canvas)

        // Text paints for advertisement
        val adTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f * dp
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val adSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C4B5FD")
            textSize = 10.5f * dp
            typeface = Typeface.DEFAULT
        }

        val textLeft = logoLeft + logoSize + (12f * dp)
        val adCenterY = bannerTop + (bannerHeight / 2f)
        canvas.drawText("✨ Translated with ELI5", textLeft, adCenterY - (2f * dp), adTitlePaint)
        canvas.drawText("Explain & Translate screen instantly • Get ELI5", textLeft, adCenterY + (13f * dp), adSubPaint)

        // QR Code on right
        try {
            val qrSize = (bannerHeight * 0.72f).toInt()
            val qrBmp = generateQrBitmap(PLAY_STORE_URL, qrSize)
            val qrLeft = width - qrSize - (16f * dp).toInt()
            val qrTop = (bannerTop + (bannerHeight - qrSize) / 2f).toInt()

            val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
            }
            val qrBgRect = RectF(
                qrLeft.toFloat() - 4f,
                qrTop.toFloat() - 4f,
                qrLeft.toFloat() + qrSize + 4f,
                qrTop.toFloat() + qrSize + 4f
            )
            canvas.drawRoundRect(qrBgRect, 6f * dp, 6f * dp, qrBgPaint)
            canvas.drawBitmap(qrBmp, qrLeft.toFloat(), qrTop.toFloat(), null)
        } catch (_: Exception) {}

        // 4. Save to shared cache file
        val sharedDir = File(context.cacheDir, "shared")
        if (!sharedDir.exists()) sharedDir.mkdirs()
        val imageFile = File(sharedDir, "eli5_translation_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        imageFile.setReadable(true, false)

        imageFile
    }

    fun shareTranslatedScreen(
        context: Context,
        screenshot: Bitmap,
        blocks: List<com.alaotach.eli5.assistant.TranslatedBlock>,
        targetLanguage: String,
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        onShareStarted: (() -> Unit)? = null,
        onError: (() -> Unit)? = null
    ) {
        if (blocks.isEmpty()) {
            Toast.makeText(context, "No translation to share", Toast.LENGTH_SHORT).show()
            onError?.invoke()
            return
        }

        Toast.makeText(context, "Preparing screenshot to share...", Toast.LENGTH_SHORT).show()

        coroutineScope.launch(Dispatchers.Main) {
            try {
                val imageFile = createTranslatedShareImage(context, screenshot, blocks, targetLanguage)
                val imageUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )

                val caption = "✨ Translated into $targetLanguage with ELI5! Try ELI5 now: $PLAY_STORE_URL"

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    clipData = ClipData.newRawUri("ELI5 Translation", imageUri)
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    putExtra(Intent.EXTRA_SUBJECT, "ELI5 Screen Translation")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                grantPermissionsToReceivers(context, shareIntent, imageUri)

                val chooserIntent = Intent.createChooser(shareIntent, "Share Translated Screen").apply {
                    clipData = ClipData.newRawUri("ELI5 Translation", imageUri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(chooserIntent)
                onShareStarted?.invoke()
            } catch (e: Exception) {
                onError?.invoke()
                Toast.makeText(context, "Could not share screenshot: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

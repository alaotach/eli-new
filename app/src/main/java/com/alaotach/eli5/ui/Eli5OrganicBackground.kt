package com.alaotach.eli5.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.alaotach.eli5.ui.theme.LavenderBackground
import com.alaotach.eli5.ui.theme.LavenderLight
import com.alaotach.eli5.ui.theme.PurplePrimary
import com.alaotach.eli5.ui.theme.PurpleSecondary

@Composable
fun Eli5OrganicBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF120E1A) else LavenderBackground
    val outerBlobColor = if (isDark) Color(0xFF231835) else LavenderLight.copy(alpha = 0.85f)
    val innerBlobColor = if (isDark) Color(0xFF352054) else PurplePrimary.copy(alpha = 0.28f)
    val deepAccentColor = if (isDark) PurpleSecondary.copy(alpha = 0.35f) else PurplePrimary.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // --- Top-Right Organic Blobs ---
            // Outer sweeping lavender wave
            val pathTopOuter = Path().apply {
                moveTo(w * 0.45f, 0f)
                cubicTo(
                    w * 0.60f, h * 0.04f,
                    w * 0.70f, h * 0.12f,
                    w, h * 0.14f
                )
                lineTo(w, 0f)
                close()
            }
            drawPath(pathTopOuter, outerBlobColor)

            // Inner vibrant purple curve
            val pathTopInner = Path().apply {
                moveTo(w * 0.68f, 0f)
                cubicTo(
                    w * 0.76f, h * 0.05f,
                    w * 0.84f, h * 0.09f,
                    w, h * 0.09f
                )
                lineTo(w, 0f)
                close()
            }
            drawPath(pathTopInner, innerBlobColor)

            // Top-right corner deep accent pill/blob
            val pathTopCorner = Path().apply {
                moveTo(w * 0.82f, 0f)
                cubicTo(
                    w * 0.88f, h * 0.03f,
                    w * 0.94f, h * 0.05f,
                    w, h * 0.05f
                )
                lineTo(w, 0f)
                close()
            }
            drawPath(pathTopCorner, deepAccentColor)

            // --- Bottom-Left / Footer Organic Waves ---
            val pathBottomOuter = Path().apply {
                moveTo(0f, h * 0.88f)
                cubicTo(
                    w * 0.20f, h * 0.86f,
                    w * 0.35f, h * 0.94f,
                    w * 0.55f, h
                )
                lineTo(0f, h)
                close()
            }
            drawPath(pathBottomOuter, outerBlobColor.copy(alpha = if (isDark) 0.5f else 0.7f))

            val pathBottomInner = Path().apply {
                moveTo(0f, h * 0.93f)
                cubicTo(
                    w * 0.15f, h * 0.92f,
                    w * 0.28f, h * 0.97f,
                    w * 0.38f, h
                )
                lineTo(0f, h)
                close()
            }
            drawPath(pathBottomInner, innerBlobColor)
        }

        content()
    }
}

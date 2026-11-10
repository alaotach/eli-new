package com.alaotach.eli5.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.alaotach.eli5.ui.theme.LavenderBackground

@Composable
fun Eli5OrganicBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF120E1A) else LavenderBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // -------------------------------------------------------------------------
            // TOP-RIGHT CURVED BLENDED WAVES (Smooth cubic Bézier contours)
            // -------------------------------------------------------------------------

            // 1. Broad Lavender Backdrop Sweep
            val outerPath = Path().apply {
                moveTo(w * 0.35f, 0f)
                cubicTo(
                    w * 0.52f, h * 0.035f,
                    w * 0.68f, h * 0.135f,
                    w, h * 0.155f
                )
                lineTo(w, 0f)
                close()
            }
            val outerBrush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF2E1C45), Color(0xFF1E1430))
                } else {
                    listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE).copy(alpha = 0.9f))
                },
                start = Offset(w * 0.4f, 0f),
                end = Offset(w, h * 0.16f)
            )
            drawPath(outerPath, outerBrush)

            // 2. Middle Violet Organic Spline
            val midPath = Path().apply {
                moveTo(w * 0.52f, 0f)
                cubicTo(
                    w * 0.64f, h * 0.04f,
                    w * 0.74f, h * 0.11f,
                    w, h * 0.115f
                )
                lineTo(w, 0f)
                close()
            }
            val midBrush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF4C1D95).copy(alpha = 0.6f), Color(0xFF3B1878).copy(alpha = 0.8f))
                } else {
                    listOf(Color(0xFFC084FC).copy(alpha = 0.55f), Color(0xFFA855F7).copy(alpha = 0.45f))
                },
                start = Offset(w * 0.52f, 0f),
                end = Offset(w, h * 0.12f)
            )
            drawPath(midPath, midBrush)

            // 3. Deep Royal Purple Corner Contour
            val innerPath = Path().apply {
                moveTo(w * 0.66f, 0f)
                cubicTo(
                    w * 0.76f, h * 0.03f,
                    w * 0.84f, h * 0.075f,
                    w, h * 0.078f
                )
                lineTo(w, 0f)
                close()
            }
            val innerBrush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF581C87), Color(0xFF3B0764))
                } else {
                    listOf(Color(0xFF6B21A8), Color(0xFF581C87))
                },
                start = Offset(w * 0.66f, 0f),
                end = Offset(w, h * 0.08f)
            )
            drawPath(innerPath, innerBrush)

            // -------------------------------------------------------------------------
            // BOTTOM ORGANIC CURVED FOOTER WAVES
            // -------------------------------------------------------------------------

            // 1. Broad Lavender Floor Sweep
            val bottomOuterPath = Path().apply {
                moveTo(0f, h * 0.83f)
                cubicTo(
                    w * 0.25f, h * 0.81f,
                    w * 0.50f, h * 0.91f,
                    w * 0.85f, h
                )
                lineTo(0f, h)
                close()
            }
            val bottomOuterBrush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF28183E), Color(0xFF1E132E))
                } else {
                    listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE).copy(alpha = 0.8f))
                },
                start = Offset(0f, h * 0.83f),
                end = Offset(w * 0.85f, h)
            )
            drawPath(bottomOuterPath, bottomOuterBrush)

            // 2. Rich Deep Purple Crest Wave
            val bottomInnerPath = Path().apply {
                moveTo(0f, h * 0.885f)
                cubicTo(
                    w * 0.20f, h * 0.875f,
                    w * 0.38f, h * 0.935f,
                    w * 0.65f, h
                )
                lineTo(0f, h)
                close()
            }
            val bottomInnerBrush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF581C87), Color(0xFF3B0764))
                } else {
                    listOf(Color(0xFF6B21A8), Color(0xFF581C87))
                },
                start = Offset(0f, h * 0.885f),
                end = Offset(w * 0.65f, h)
            )
            drawPath(bottomInnerPath, bottomInnerBrush)
        }

        content()
    }
}

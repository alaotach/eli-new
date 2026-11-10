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

@Composable
fun Eli5OrganicBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF120E1A) else LavenderBackground

    // Solid rich colors matching the design reference image exactly
    val topOuterWaveColor = if (isDark) Color(0xFF261938) else Color(0xFFE9D5FF)
    val topMidWaveColor = if (isDark) Color(0xFF3B2256) else Color(0xFFD8B4FE).copy(alpha = 0.65f)
    val topCornerDarkBlobColor = if (isDark) Color(0xFF4C1D95) else Color(0xFF581C87)

    val bottomOuterWaveColor = if (isDark) Color(0xFF261938) else Color(0xFFE9D5FF)
    val bottomDarkWaveColor = if (isDark) Color(0xFF4C1D95) else Color(0xFF581C87)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // =========================================================================
            // TOP-RIGHT LAYERED ORGANIC WAVES
            // Matches the curved purple accent in the top right of the reference image
            // =========================================================================

            // 1. Outer broad lavender sweep
            val pathTopOuter = Path().apply {
                moveTo(w * 0.38f, 0f)
                cubicTo(
                    w * 0.52f, h * 0.04f,
                    w * 0.65f, h * 0.14f,
                    w, h * 0.15f
                )
                lineTo(w, 0f)
                close()
            }
            drawPath(pathTopOuter, topOuterWaveColor)

            // 2. Middle violet transition curve
            val pathTopMid = Path().apply {
                moveTo(w * 0.52f, 0f)
                cubicTo(
                    w * 0.62f, h * 0.04f,
                    w * 0.72f, h * 0.11f,
                    w, h * 0.115f
                )
                lineTo(w, 0f)
                close()
            }
            drawPath(pathTopMid, topMidWaveColor)

            // 3. Deep rich royal purple corner blob
            val pathTopCorner = Path().apply {
                moveTo(w * 0.66f, 0f)
                cubicTo(
                    w * 0.75f, h * 0.035f,
                    w * 0.82f, h * 0.075f,
                    w, h * 0.08f
                )
                lineTo(w, 0f)
                close()
            }
            drawPath(pathTopCorner, topCornerDarkBlobColor)

            // =========================================================================
            // BOTTOM ORGANIC CURVED FOOTER WAVES
            // Matches the sweeping dark purple and lavender curves at the bottom of the screens
            // =========================================================================

            // 1. Broad lavender bottom wave
            val pathBottomOuter = Path().apply {
                moveTo(0f, h * 0.84f)
                cubicTo(
                    w * 0.22f, h * 0.82f,
                    w * 0.45f, h * 0.92f,
                    w * 0.75f, h
                )
                lineTo(0f, h)
                close()
            }
            drawPath(pathBottomOuter, bottomOuterWaveColor)

            // 2. Rich royal purple bottom wave crest
            val pathBottomDark = Path().apply {
                moveTo(0f, h * 0.89f)
                cubicTo(
                    w * 0.18f, h * 0.88f,
                    w * 0.36f, h * 0.94f,
                    w * 0.58f, h
                )
                lineTo(0f, h)
                close()
            }
            drawPath(pathBottomDark, bottomDarkWaveColor)
        }

        content()
    }
}

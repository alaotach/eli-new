package com.alaotach.eli5

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Custom vector view rendering the purple ELI5 security shield
 * with a crisp white checkmark for the permission screen.
 */
class ShieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C3AED")
    }

    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = context.dpToPx(5f).toFloat()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()
    private val checkPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        path.reset()
        path.moveTo(cx, 0f)
        path.lineTo(w, 0.2f * h)
        path.lineTo(w, 0.6f * h)
        path.quadTo(w, 0.85f * h, cx, h)
        path.quadTo(0f, 0.85f * h, 0f, 0.6f * h)
        path.lineTo(0f, 0.2f * h)
        path.close()
        canvas.drawPath(path, paint)

        checkPath.reset()
        checkPath.moveTo(0.28f * w, 0.52f * h)
        checkPath.lineTo(0.44f * w, 0.67f * h)
        checkPath.lineTo(0.72f * w, 0.35f * h)
        canvas.drawPath(checkPath, checkPaint)
    }
}

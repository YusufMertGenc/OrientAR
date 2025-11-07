package com.example.ardeneme.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        setShadowLayer(6f, 0f, 0f, Color.BLACK)
    }

    // 3 adet üst üste mavi chevron ok
    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA42C3F7")
        style = Paint.Style.STROKE
        strokeWidth = 28f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var distanceM: Float = 0f
    private var turnDeltaDeg: Float = 0f   // -180 .. +180

    /** ARCore'dan gelen uzaklık ve dönüş açısını buraya veriyoruz */
    fun setNavigation(distanceMeters: Float, deltaDeg: Float) {
        distanceM = distanceMeters
        turnDeltaDeg = deltaDeg
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val baseY = height * 0.70f

        // Ok grubunu hedef yönüne göre döndür
        canvas.save()
        canvas.rotate(turnDeltaDeg, cx, baseY)

        val size = width * 0.18f
        val gap = size * 0.40f

        drawChevron(canvas, cx, baseY, size)
        drawChevron(canvas, cx, baseY - (size + gap), size * 0.85f)
        drawChevron(canvas, cx, baseY - 2 * (size + gap), size * 0.72f)

        canvas.restore()

        // Mesafe metni
        val distTxt = if (distanceM < 1000f) {
            "${distanceM.toInt()} m"
        } else {
            String.format("%.2f km", distanceM / 1000f)
        }
        canvas.drawText("Distance: $distTxt", 32f, 72f, textPaint)

        // Dönüş yönü
        val arrowChar = if (turnDeltaDeg >= 0f) "→" else "←"
        canvas.drawText(
            "Turn: $arrowChar ${abs(turnDeltaDeg).toInt()}°",
            32f,
            132f,
            textPaint
        )
    }

    private fun drawChevron(c: Canvas, cx: Float, cy: Float, size: Float) {
        val half = size * 0.6f
        val leg = size
        // sol bacak
        c.drawLine(cx - half, cy, cx, cy - leg, chevronPaint)
        // sağ bacak
        c.drawLine(cx + half, cy, cx, cy - leg, chevronPaint)
    }
}

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

    // 3 tane üst üste mavi chevron ok
    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA42C3F7")
        style = Paint.Style.STROKE
        strokeWidth = 28f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var deviceAzimuthDeg: Float = 0f        // Pusuladan gelen yön
    private var bearingToTargetDeg: Float = 0f      // Hedefe olan yön
    private var distanceM: Float = 0f

    fun setDeviceAzimuth(deg: Float) {
        deviceAzimuthDeg = deg
        invalidate()
    }

    fun setNavigationData(distanceMeters: Float, bearingToDeg: Float) {
        distanceM = distanceMeters
        bearingToTargetDeg = bearingToDeg
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val baseY = height * 0.70f

        // Hedef yönü - baktığın yön = ne kadar dönmen gerek?
        val delta = normalize(bearingToTargetDeg - deviceAzimuthDeg)

        // Ok grubunu bu açı kadar döndür
        canvas.save()
        canvas.rotate(delta, cx, baseY)

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

        // Sağa mı sola mı dön?
        val arrowChar = if (delta >= 0f) "→" else "←"
        canvas.drawText(
            "Turn: $arrowChar ${abs(delta).toInt()}°",
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

    private fun normalize(d: Float): Float {
        var x = d
        while (x < -180f) x += 360f
        while (x > 180f) x -= 360f
        return x
    }
}

package com.example.ardeneme.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = 8f
        color = 0xFF28C3FF.toInt()   // mavi ok
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 40f
    }

    private var distanceM: Float = 0f
    private var bearingToTarget: Float = 0f
    private var deviceAzimuth: Float = 0f

    fun setNavigationData(distanceM: Float, bearingDeg: Float) {
        this.distanceM = distanceM
        this.bearingToTarget = bearingDeg
        invalidate()
    }

    fun setDeviceAzimuth(azimuthDeg: Float) {
        this.deviceAzimuth = azimuthDeg
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Basit metin
        canvas.drawText(
            "Distance: ${distanceM.toInt()} m",
            40f,
            height - 120f,
            textPaint
        )

        // Hedef yönü hesabı
        val heading = normalizeAngle(bearingToTarget - deviceAzimuth)

        // Ok ekranın ortasında olsun
        val cx = width / 2f
        val cy = height * 0.75f

        drawArrow(canvas, cx, cy, heading)
    }

    private fun drawArrow(canvas: Canvas, cx: Float, cy: Float, angleDeg: Float) {
        val path = Path()

        val len = 180f
        val rad = Math.toRadians(angleDeg.toDouble())

        // Ucun koordinatı
        val tipX = cx + len * sin(rad).toFloat()
        val tipY = cy - len * cos(rad).toFloat()

        // Kuyruk
        val tailLen = 60f
        val tailX = cx - tailLen * sin(rad).toFloat()
        val tailY = cy + tailLen * cos(rad).toFloat()

        // Kanatlar
        val wing = 35f
        val leftRad = Math.toRadians((angleDeg - 25).toDouble())
        val rightRad = Math.toRadians((angleDeg + 25).toDouble())

        val leftX = cx + wing * sin(leftRad).toFloat()
        val leftY = cy - wing * cos(leftRad).toFloat()
        val rightX = cx + wing * sin(rightRad).toFloat()
        val rightY = cy - wing * cos(rightRad).toFloat()

        path.moveTo(tailX, tailY)
        path.lineTo(leftX, leftY)
        path.lineTo(tipX, tipY)
        path.lineTo(rightX, rightY)
        path.close()

        canvas.drawPath(path, arrowPaint)
    }

    private fun normalizeAngle(a: Float): Float {
        var x = a
        while (x < -180f) x += 360f
        while (x > 180f) x -= 360f
        return x
    }
}

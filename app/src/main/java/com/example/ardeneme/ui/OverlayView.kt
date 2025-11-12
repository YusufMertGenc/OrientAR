package com.example.ardeneme.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.ardeneme.R
import kotlin.math.cos
import kotlin.math.sin

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var distanceM: Float = 0f
    private var bearingTo: Float = 0f
    private var deviceAzimuth: Float = 0f
    private var arrowHeading: Float = 0f   // ekranda çizilecek ok açısı

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.holo_purple)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        textSize = 40f
    }

    private val arrowPath = Path()

    fun setNavigationData(distance: Float, bearing: Float) {
        distanceM = distance
        bearingTo = bearing
        invalidate()
    }

    fun setDeviceAzimuth(azimuth: Float) {
        deviceAzimuth = azimuth
        invalidate()
    }

    fun setArrowHeading(headingDeg: Float) {
        arrowHeading = headingDeg
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height * 0.75f   // oku biraz aşağıya koy

        // --- Ok geometrisi (bir üçgen gibi) ---
        val arrowLength = width * 0.15f
        val arrowWidth  = arrowLength * 0.5f

        // heading’i radyana çevir
        val rad = Math.toRadians(arrowHeading.toDouble())
        val dirX = cos(rad).toFloat()
        val dirY = sin(rad).toFloat()

        // Uç noktası (ileri)
        val tipX = cx + dirX * arrowLength
        val tipY = cy - dirY * arrowLength

        // Kuyruk noktası
        val tailX = cx - dirX * (arrowLength * 0.4f)
        val tailY = cy + dirY * (arrowLength * 0.4f)

        // Ok genişliği için sağ/sol normal
        val nx = -dirY
        val ny = dirX

        val leftX  = tailX + nx * (arrowWidth / 2f)
        val leftY  = tailY + ny * (arrowWidth / 2f)
        val rightX = tailX - nx * (arrowWidth / 2f)
        val rightY = tailY - ny * (arrowWidth / 2f)

        arrowPath.reset()
        arrowPath.moveTo(tipX, tipY)
        arrowPath.lineTo(leftX, leftY)
        arrowPath.lineTo(rightX, rightY)
        arrowPath.close()

        // Ok çiz
        canvas.drawPath(arrowPath, arrowPaint)

        // Yazı: mesafe & global bearing
        val text = "Heading: ${arrowHeading.toInt()}°   Dist: ${distanceM.toInt()} m"
        canvas.drawText(text, 40f, 80f, textPaint)
    }
}

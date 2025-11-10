package com.example.ardeneme.sensors

import android.content.Context
import android.hardware.*
import android.view.Surface
import android.view.WindowManager
import kotlin.math.round

class CompassHelper(private val context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val R = FloatArray(9)
    private val I = FloatArray(9)
    private val gravity = FloatArray(3)
    private val geomag = FloatArray(3)

    private var callback: ((Float) -> Unit)? = null

    fun start(onAzimuth: (Float) -> Unit) {
        callback = onAzimuth
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        callback = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER ->
                System.arraycopy(event.values, 0, gravity, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD ->
                System.arraycopy(event.values, 0, geomag, 0, 3)
        }

        if (SensorManager.getRotationMatrix(R, I, gravity, geomag)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(R, orientation)

            var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

            // Ekran dönüşünü telafi et
            azimuth = (azimuth + getDisplayRotationCompensation()).mod(360f)
            if (azimuth < 0) azimuth += 360f

            callback?.invoke(round(azimuth))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun getDisplayRotationCompensation(): Float {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = wm.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_180 -> 180f
            Surface.ROTATION_270 -> 270f
            else -> 0f
        }
    }
}

package com.example.ardeneme

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ardeneme.ui.OverlayView
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import kotlin.concurrent.thread
import kotlin.math.*

class MainActivity : ComponentActivity() {

    private var arSession: Session? = null
    private lateinit var overlayView: OverlayView
    private lateinit var infoText: TextView

    // Hedef koordinat (KENDİ HEDEFİNLE DEĞİŞTİR)
    private val targetLat = 39.9036     // enlem
    private val targetLng = 32.6227     // boylam

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayView = findViewById(R.id.overlayView)
        infoText = findViewById(R.id.infoText)

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                123
            )
        } else {
            tryCreateSessionAndStart()
        }
    }

    private fun hasPermissions(): Boolean {
        val cam = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return cam && fine
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123 && hasPermissions()) {
            tryCreateSessionAndStart()
        } else {
            infoText.text = "Camera & location permission required for ARCore."
        }
    }

    private fun tryCreateSessionAndStart() {
        try {
            if (arSession == null) {
                arSession = Session(this)
            }
        } catch (e: UnavailableException) {
            infoText.text = "ARCore not available: ${e.javaClass.simpleName}"
            Log.e("MainActivity", "ARCore session error", e)
            return
        }

        val session = arSession ?: return
        val config = session.config
        config.geospatialMode = Config.GeospatialMode.ENABLED
        session.configure(config)

        try {
            session.resume()
        } catch (e: CameraNotAvailableException) {
            infoText.text = "Camera not available."
            return
        }

        infoText.text = "ARCore Geospatial running. Move device so it can localize..."
        startGeospatialLoop()
    }

    override fun onResume() {
        super.onResume()
        arSession?.let { session ->
            try {
                session.resume()
                startGeospatialLoop()
            } catch (e: CameraNotAvailableException) {
                infoText.text = "Camera not available."
            }
        }
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
        renderLoopRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        arSession?.close()
        arSession = null
    }

    @SuppressLint("SetTextI18n")
    private fun startGeospatialLoop() {
        if (renderLoopRunning) return

        renderLoopRunning = true
        thread(start = true) {
            while (renderLoopRunning && !isFinishing) {
                val session = arSession ?: break
                try {
                    val frame = session.update()
                    val earth = session.earth

                    if (earth == null || earth.trackingState != TrackingState.TRACKING) {
                        runOnUiThread {
                            infoText.text =
                                "Point the camera around so ARCore can localize (Geospatial)..."
                        }
                        continue
                    }

                    val geoPose = earth.cameraGeospatialPose
                    val currentLat = geoPose.latitude
                    val currentLng = geoPose.longitude
                    val headingDeg = geoPose.heading.toFloat() // 0 = kuzey, saat yönü

                    // Hedefe mesafe ve bearing
                    val distM = haversineDistanceKm(
                        currentLat,
                        currentLng,
                        targetLat,
                        targetLng
                    ) * 1000.0

                    val bearingTo = bearingDeg(
                        currentLat,
                        currentLng,
                        targetLat,
                        targetLng
                    ).toFloat()

                    // Dönüş açısı: hedef yönü - baktığın yön (−180..+180)
                    val delta = normalizeDelta(bearingTo - headingDeg)

                    runOnUiThread {
                        overlayView.setNavigation(distM.toFloat(), delta)
                        infoText.text =
                            "ARCore Geospatial\n" +
                                    "Current: %.5f, %.5f (heading: %.1f°)\n".format(
                                        currentLat,
                                        currentLng,
                                        headingDeg
                                    ) +
                                    "Target : %.5f, %.5f\n".format(targetLat, targetLng) +
                                    "Distance ≈ %.1f m, turn Δ=%.1f°".format(distM, delta)
                    }

                } catch (e: CameraNotAvailableException) {
                    Log.e("MainActivity", "Camera not available in loop", e)
                    break
                } catch (e: Exception) {
                    Log.e("MainActivity", "Exception in loop", e)
                }

                try {
                    Thread.sleep(33)
                } catch (_: InterruptedException) {
                }
            }
            renderLoopRunning = false
        }
    }

    companion object {
        @Volatile
        private var renderLoopRunning = false

        // Haversine ile km cinsinden mesafe
        private fun haversineDistanceKm(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val R = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                    cos(Math.toRadians(lat1)) *
                    cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return R * c
        }

        // Bearing (0..360)
        private fun bearingDeg(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val φ1 = Math.toRadians(lat1)
            val φ2 = Math.toRadians(lat2)
            val λ1 = Math.toRadians(lon1)
            val λ2 = Math.toRadians(lon2)
            val y = sin(λ2 - λ1) * cos(φ2)
            val x = cos(φ1) * sin(φ2) -
                    sin(φ1) * cos(φ2) * cos(λ2 - λ1)
            val θ = atan2(y, x)
            return (Math.toDegrees(θ) + 360.0) % 360.0
        }

        // -180..+180 aralığına normalize
        private fun normalizeDelta(d: Float): Float {
            var x = d
            while (x < -180f) x += 360f
            while (x > 180f) x -= 360f
            return x
        }
    }
}

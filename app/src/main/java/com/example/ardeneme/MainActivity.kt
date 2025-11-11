package com.example.ardeneme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ardeneme.location.LocationHelper
import com.example.ardeneme.sensors.CompassHelper
import com.example.ardeneme.ui.OverlayView
import com.google.ar.sceneform.ux.ArFragment


class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var overlayView: OverlayView
    private lateinit var infoText: TextView

    private lateinit var locationHelper: LocationHelper
    private lateinit var compassHelper: CompassHelper

    // Hedef nokta
    private val targetLat = 39.9036
    private val targetLng = 32.6227

    private val locPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            startSensors()
        } else {
            infoText.text = "Konum izni verilmedi."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager
            .findFragmentById(R.id.ux_fragment) as ArFragment

        overlayView = findViewById(R.id.overlayView)
        infoText = findViewById(R.id.infoText)

        locationHelper = LocationHelper(this)
        compassHelper = CompassHelper(this)

        requestLocationPerms()
        // Kamera iznini ve ARCore oturumunu ArFragment kendi hallediyor.
    }

    private fun requestLocationPerms() {
        val needFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val needCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        if (needFine && needCoarse) {
            locPermLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            startSensors()
        }
    }

    private fun startSensors() {
        compassHelper.start { azimuthDeg ->
            overlayView.setDeviceAzimuth(azimuthDeg)
        }

        locationHelper.startLocationUpdates { loc ->
            val res = FloatArray(2)

            Location.distanceBetween(
                loc.latitude, loc.longitude,
                targetLat, targetLng,
                res
            )

            val distanceM = res[0]
            val bearingTo = res[1]
            overlayView.setNavigationData(distanceM, bearingTo)

            infoText.text = "Lat:${"%.5f".format(loc.latitude)} " +
                    "Lon:${"%.5f".format(loc.longitude)} " +
                    "Dist:${distanceM.toInt()}m"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compassHelper.stop()
        locationHelper.stopLocationUpdates()
    }
}

package com.example.ardeneme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ardeneme.location.LocationHelper
import com.example.ardeneme.sensors.CompassHelper
import com.example.ardeneme.ui.OverlayView
import com.google.ar.sceneform.ux.ArFragment

class MainActivity : FragmentActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var overlayView: OverlayView
    private lateinit var infoText: TextView

    private lateinit var locationHelper: LocationHelper
    private lateinit var compassHelper: CompassHelper

    // Hedef konum (Google Maps'ten al)
    private val targetLat = 39.9036
    private val targetLng = 32.6227

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.CAMERA] == true &&
            (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        ) {
            startSensors()
        } else {
            infoText.text = "Camera & location permissions are required."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment =
            supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        overlayView = findViewById(R.id.overlayView)
        infoText = findViewById(R.id.infoText)

        locationHelper = LocationHelper(this)
        compassHelper = CompassHelper(this)

        requestPerms()

        // İstersek ARCore frame'lerini dinleyebiliriz (şimdilik navigasyonu sensör+GPS ile yapıyoruz)
        arFragment.arSceneView.scene.addOnUpdateListener {
            // ARCore burada çalışıyor, kamera ve tracking ARCore tarafından yönetiliyor
        }
    }

    private fun requestPerms() {
        val needCam = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED

        val needFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        val needCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

        if (needCam || (needFine && needCoarse)) {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            startSensors()
        }
    }

    private fun startSensors() {
        // Pusula: cihazın baktığı yön
        compassHelper.start { azimuthDeg ->
            overlayView.setDeviceAzimuth(azimuthDeg)
            infoText.text = "Heading: ${azimuthDeg.toInt()}°"
        }

        // Konum: hedefe uzaklık ve bearing
        locationHelper.startLocationUpdates { loc ->
            val res = FloatArray(2)

            Location.distanceBetween(
                loc.latitude,
                loc.longitude,
                targetLat,
                targetLng,
                res
            )

            val distanceM = res[0]   // metre
            val bearingTo = res[1]   // derece

            overlayView.setNavigationData(distanceM, bearingTo)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compassHelper.stop()
        locationHelper.stopLocationUpdates()
    }
}

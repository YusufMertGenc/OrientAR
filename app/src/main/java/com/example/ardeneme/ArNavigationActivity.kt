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
import com.google.ar.core.Config
import com.google.ar.core.Session
import io.github.sceneview.ar.ARSceneView

class ArNavigationActivity : AppCompatActivity() {

    private lateinit var arView: ARSceneView
    private lateinit var overlayView: OverlayView
    private lateinit var infoText: TextView
    private lateinit var locationHelper: LocationHelper
    private lateinit var compassHelper: CompassHelper

    // Varsayılan hedef (ODTÜ KKK Meydanı)
    private var targetLat = 35.186
    private var targetLng = 33.024

    private var lastDistanceM = 0f
    private var lastBearingTo = 0f
    private var lastAzimuth = 0f

    private val locPermLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) startSensors()
    }

    private val camPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { initUi(); requestLocationPerms() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Haritadan gelen hedefi al
        targetLat = intent.getDoubleExtra("targetLat", 35.186)
        targetLng = intent.getDoubleExtra("targetLng", 33.024)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            initUi(); requestLocationPerms()
        } else {
            camPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun initUi() {
        setContentView(R.layout.activity_ar)
        arView = findViewById(R.id.arView)
        overlayView = findViewById(R.id.overlayView)
        infoText = findViewById(R.id.infoText)
        locationHelper = LocationHelper(this)
        compassHelper = CompassHelper(this)

        arView.lifecycle = lifecycle
        arView.onSessionCreated = { session: Session ->
            val config = Config(session).apply {
                focusMode = Config.FocusMode.AUTO
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            }
            session.configure(config)
            infoText.text = "Navigasyon Başlıyor..."
        }
    }

    private fun requestLocationPerms() {
        locPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    private fun startSensors() {
        compassHelper.start { az -> lastAzimuth = az; updateArrow() }
        locationHelper.startLocationUpdates { loc ->
            val res = FloatArray(2)
            Location.distanceBetween(loc.latitude, loc.longitude, targetLat, targetLng, res)
            lastDistanceM = res[0]; lastBearingTo = res[1]
            overlayView.setNavigationData(lastDistanceM, lastBearingTo)
            infoText.text = "Mesafe: ${lastDistanceM.toInt()}m"
            updateArrow()
        }
    }

    private fun updateArrow() {
        var heading = lastBearingTo - lastAzimuth
        while (heading < -180f) heading += 360f
        while (heading > 180f) heading -= 360f
        overlayView.setDeviceAzimuth(lastAzimuth)
        overlayView.setArrowHeading(heading)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::locationHelper.isInitialized) locationHelper.stopLocationUpdates()
        if(::compassHelper.isInitialized) compassHelper.stop()
    }
}
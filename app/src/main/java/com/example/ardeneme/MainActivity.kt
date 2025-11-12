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
import com.google.ar.core.Frame
import com.google.ar.core.Session
import io.github.sceneview.ar.ARSceneView

class MainActivity : AppCompatActivity() {

    private lateinit var arView: ARSceneView
    private lateinit var overlayView: OverlayView
    private lateinit var infoText: TextView

    private lateinit var locationHelper: LocationHelper
    private lateinit var compassHelper: CompassHelper

    // Hedef koordinat (kampüs)
    private val targetLat = 39.9036
    private val targetLng = 32.6227

    // Navigation state
    private var lastDistanceM = 0f
    private var lastBearingTo = 0f
    private var lastAzimuth = 0f

    // ---------- İzin launcher'ları ----------

    private val locPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) startSensors()
        else infoText.text = "Konum izni verilmedi."
    }

    private val camPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            initUi()
            requestLocationPerms()
        } else {
            setContentView(R.layout.activity_main)
            findViewById<TextView>(R.id.infoText).text = "Kamera izni verilmedi."
        }
    }

    // ---------- Activity lifecycle ----------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasCameraPermission()) {
            initUi()
            requestLocationPerms()
        } else {
            setContentView(R.layout.activity_main)
            findViewById<TextView>(R.id.infoText).text = "Kamera izni bekleniyor…"
            camPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    // ---------- UI + AR kurulumu ----------

    private fun initUi() {
        setContentView(R.layout.activity_main)

        arView      = findViewById(R.id.arView)
        overlayView = findViewById(R.id.overlayView)
        infoText    = findViewById(R.id.infoText)

        locationHelper = LocationHelper(this)
        compassHelper  = CompassHelper(this)

        // SceneView yaşam döngüsünü Activity lifecycle’a bağla
        arView.lifecycle = lifecycle

        // ARCore session oluşturulduğunda çağrılır -> Config burada
        arView.onSessionCreated = { session: Session ->
            val config = Config(session).apply {
                // *** ÖNEMLİ: HDR yok, ENVIRONMENTAL_HDR kullanmıyoruz ***
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY

                focusMode            = Config.FocusMode.AUTO
                instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                planeFindingMode     = Config.PlaneFindingMode.HORIZONTAL

                depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }
            }
            session.configure(config)
            infoText.text = "AR session hazır, yüzeyi tara."
        }

        // Her frame’de çağrılır – burada sadece tracking durumu vs. kontrol ediyoruz
        arView.onSessionUpdated = { _: Session, frame: Frame ->
            val camera = frame.camera
            // İstersen burada TrackingState'e göre uyarı verebilirsin
            // (NOT_TRACKING ise “Telefonu yavaşça hareket ettir” gibi)
        }
    }

    // ---------- Konum & pusula ----------

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
        // Pusula
        compassHelper.start { azimuthDeg ->
            lastAzimuth = azimuthDeg
            overlayView.setDeviceAzimuth(azimuthDeg)
            updateArrowHeading()
        }

        // Konum
        locationHelper.startLocationUpdates { loc ->
            val res = FloatArray(2)
            Location.distanceBetween(
                loc.latitude, loc.longitude,
                targetLat, targetLng,
                res
            )
            lastDistanceM = res[0]
            lastBearingTo = res[1]

            overlayView.setNavigationData(lastDistanceM, lastBearingTo)

            infoText.text = "Lat:${"%.5f".format(loc.latitude)}  " +
                    "Lon:${"%.5f".format(loc.longitude)}  " +
                    "Dist:${lastDistanceM.toInt()} m"
            updateArrowHeading()
        }
    }

    private fun updateArrowHeading() {
        // Hedef yönü = hedef bearing – cihazın azimuth’u
        val heading = normalizeAngle(lastBearingTo - lastAzimuth)
        overlayView.setArrowHeading(heading)
    }

    private fun normalizeAngle(a: Float): Float {
        var x = a
        while (x < -180f) x += 360f
        while (x > 180f)  x -= 360f
        return x
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::compassHelper.isInitialized) compassHelper.stop()
        if (::locationHelper.isInitialized) locationHelper.stopLocationUpdates()
        if (::arView.isInitialized) arView.destroy()
    }
}

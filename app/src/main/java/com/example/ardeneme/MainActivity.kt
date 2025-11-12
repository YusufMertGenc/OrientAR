package com.example.ardeneme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.ardeneme.location.LocationHelper
import com.example.ardeneme.sensors.CompassHelper
import com.example.ardeneme.ui.OverlayView
import com.google.ar.core.Config
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private lateinit var overlayView: OverlayView
    private lateinit var infoText: TextView

    private lateinit var locationHelper: LocationHelper
    private lateinit var compassHelper: CompassHelper

    // Örnek hedef koordinat
    private val targetLat = 39.9036
    private val targetLng = 32.6227

    // Navigation state
    private var lastDistanceM = 0f
    private var lastBearingTo = 0f
    private var lastAzimuth = 0f

    // 3D ok node'u
    private var arrowNode: Node? = null

    // Konum izinleri
    private val locPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) startSensors() else infoText.text = "Konum izni verilmedi."
    }

    // Kamera izni
    private val camPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            initArUi()
            requestLocationPerms()
        } else {
            setContentView(R.layout.activity_main)
            findViewById<TextView>(R.id.infoText).text = "Kamera izni verilmedi."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasCameraPermission()) {
            initArUi()
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

    /** Kamera izni geldikten sonra AR + UI kurulum */
    private fun initArUi() {
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment
        overlayView = findViewById(R.id.overlayView)
        infoText = findViewById(R.id.infoText)

        locationHelper = LocationHelper(this)
        compassHelper  = CompassHelper(this)

        // --- ÖNEMLİ: arSceneView hazır olana kadar bekle ---
        arFragment.viewLifecycleOwnerLiveData.observe(this, Observer { owner ->
            if (owner != null) {
                // 1) HDR’li oturum konfigürasyonu (Sceneform 1.23 ile uyumlu)
                arFragment.setOnSessionConfigurationListener { session, config ->
                    config.apply {
                        lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                            Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                        instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                        focusMode = Config.FocusMode.AUTO
                        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    }
                }

                // 2) Sahne hazır: 3D oku kur ve update listener ekle
                setup3DArrow()
                setupSceneUpdate()
            }
        })
    }

    // -------- İzin/Sensörler --------

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
        } else startSensors()
    }

    private fun startSensors() {
        // Pusula
        compassHelper.start { azimuthDeg ->
            lastAzimuth = azimuthDeg
            overlayView.setDeviceAzimuth(azimuthDeg)
        }

        // Konum
        locationHelper.startLocationUpdates { loc ->
            val res = FloatArray(2)
            Location.distanceBetween(
                loc.latitude, loc.longitude,
                targetLat, targetLng,
                res
            )
            lastDistanceM  = res[0]
            lastBearingTo  = res[1]

            overlayView.setNavigationData(lastDistanceM, lastBearingTo)
            infoText.text = "Lat:${"%.5f".format(loc.latitude)}  " +
                    "Lon:${"%.5f".format(loc.longitude)}  " +
                    "Dist:${lastDistanceM.toInt()} m"
        }
    }

    // -------- 3D Ok --------

    private fun setup3DArrow() {
        val sceneView = arFragment.arSceneView ?: return
        MaterialFactory.makeOpaqueWithColor(
            this,
            Color(android.graphics.Color.CYAN)
        ).thenAccept { mat ->
            val height = 0.30f
            val radius = 0.02f
            val center = Vector3(0f, height / 2f, 0f)

            val cyl = ShapeFactory.makeCylinder(radius, height, center, mat)
            arrowNode = Node().apply { renderable = cyl }
            sceneView.scene.addChild(arrowNode)
        }
    }

    private fun setupSceneUpdate() {
        val sceneView = arFragment.arSceneView ?: return
        sceneView.scene.addOnUpdateListener {
            val node   = arrowNode ?: return@addOnUpdateListener
            val camera = sceneView.scene.camera

            // Oku kameranın ~1 m önünde tut
            val forward = camera.forward
            val pos     = Vector3.add(camera.worldPosition, forward.scaled(1.0f))
            node.worldPosition = Vector3(pos.x, camera.worldPosition.y - 0.1f, pos.z)

            // Hedef yönü = bearing - azimuth
            val heading = normalizeAngle(lastBearingTo - lastAzimuth)
            node.worldRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), -heading)
        }
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
    }
}

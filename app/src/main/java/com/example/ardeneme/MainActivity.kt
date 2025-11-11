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

    // Hedef nokta
    private val targetLat = 39.9036
    private val targetLng = 32.6227

    // Navigation için son değerler
    private var lastDistanceM: Float = 0f
    private var lastBearingTo: Float = 0f
    private var lastAzimuth: Float = 0f

    // 3D ok node’u
    private var arrowNode: Node? = null

    // Konum izin launcher’ı
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

    // Kamera izin launcher’ı (ARCore’dan önce biz hallediyoruz)
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

        // Önce kamera iznini hallet → sonra ArFragment’i oluştur
        if (hasCameraPermission()) {
            initArUi()
            requestLocationPerms()
        } else {
            // Basit geçici layout, sonra izin gelince initArUi çağrılacak
            setContentView(R.layout.activity_main)
            infoText = findViewById(R.id.infoText)
            infoText.text = "Kamera izni bekleniyor..."
            camPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Kamera izni geldikten sonra AR UI’ı kur */
    private fun initArUi() {
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager
            .findFragmentById(R.id.ux_fragment) as ArFragment

        overlayView = findViewById(R.id.overlayView)
        infoText = findViewById(R.id.infoText)

        locationHelper = LocationHelper(this)
        compassHelper = CompassHelper(this)

        setup3DArrow()
        setupSceneUpdate()
    }

    // -------- İZİNLER / SENSÖRLER --------

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
        // Pusula → hem OverlayView hem 3D ok bunu kullanıyor
        compassHelper.start { azimuthDeg ->
            lastAzimuth = azimuthDeg
            overlayView.setDeviceAzimuth(azimuthDeg)
        }

        // Konum → mesafe + bearing
        locationHelper.startLocationUpdates { loc ->
            val res = FloatArray(2)

            Location.distanceBetween(
                loc.latitude, loc.longitude,
                targetLat, targetLng,
                res
            )

            val distanceM = res[0]
            val bearingTo = res[1]

            lastDistanceM = distanceM
            lastBearingTo = bearingTo

            overlayView.setNavigationData(distanceM, bearingTo)

            infoText.text = "Lat:${"%.5f".format(loc.latitude)} " +
                    "Lon:${"%.5f".format(loc.longitude)} " +
                    "Dist:${distanceM.toInt()}m"
        }
    }

    // -------- 3D OK (Sceneform) --------

    private fun setup3DArrow() {
        MaterialFactory.makeOpaqueWithColor(
            this,
            Color(android.graphics.Color.CYAN)
        ).thenAccept { mat ->
            val height = 0.3f
            val radius = 0.02f
            val center = Vector3(0f, height / 2f, 0f)

            val cyl = ShapeFactory.makeCylinder(radius, height, center, mat)

            val node = Node().apply {
                renderable = cyl
            }

            arFragment.arSceneView.scene.addChild(node)
            arrowNode = node
        }
    }

    private fun setupSceneUpdate() {
        arFragment.arSceneView.scene.addOnUpdateListener {
            val node = arrowNode ?: return@addOnUpdateListener

            val camera = arFragment.arSceneView.scene.camera

            // Kameranın önünde 1 metre
            val forward = camera.forward
            val pos = Vector3.add(
                camera.worldPosition,
                forward.scaled(1.0f)
            )
            node.worldPosition = pos

            // Hedef yönü = bearingTo - azimuth
            val heading = normalizeAngle(lastBearingTo - lastAzimuth)

            node.worldRotation = Quaternion.axisAngle(
                Vector3(0f, 1f, 0f),
                -heading
            )
        }
    }

    private fun normalizeAngle(a: Float): Float {
        var x = a
        while (x < -180f) x += 360f
        while (x > 180f) x -= 360f
        return x
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::compassHelper.isInitialized) compassHelper.stop()
        if (::locationHelper.isInitialized) locationHelper.stopLocationUpdates()
    }
}

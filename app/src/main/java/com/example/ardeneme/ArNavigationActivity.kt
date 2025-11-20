package com.example.ardeneme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ardeneme.location.LocationHelper
import com.example.ardeneme.sensors.CompassHelper
// SceneView 2.0.3 Importları
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.ViewNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import com.google.ar.core.Config
import com.google.ar.sceneform.rendering.ViewAttachmentManager   // ⬅️ yeni import
import kotlinx.coroutines.launch

class ArNavigationActivity : AppCompatActivity() {

    private lateinit var arView: ARSceneView
    private lateinit var infoText: TextView

    private lateinit var locationHelper: LocationHelper
    private lateinit var compassHelper: CompassHelper

    // ViewNode için AttachmentManager
    private lateinit var viewAttachmentManager: ViewAttachmentManager

    private var arrowNode: ViewNode? = null

    private var targetLat = 35.24812
    private var targetLng = 33.02244
    private var lastBearingTo = 0f
    private var lastAzimuth = 0f

    private val locPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) startSensors()
        }

    private val camPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                initUi()
                requestLocationPerms()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        targetLat = intent.getDoubleExtra("targetLat", 35.24812)
        targetLng = intent.getDoubleExtra("targetLng", 33.02244)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            initUi()
            requestLocationPerms()
        } else {
            camPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun initUi() {
        setContentView(R.layout.activity_ar)
        arView = findViewById(R.id.arView)
        infoText = findViewById(R.id.infoText)

        locationHelper = LocationHelper(this)
        compassHelper = CompassHelper(this)

        // Lifecycle yönetimi
        arView.lifecycle = lifecycle

        arView.configureSession { _, config ->
            config.focusMode = Config.FocusMode.AUTO
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
        }

        arView.onSessionFailed = { exception ->
            infoText.text = "Hata: ${exception.message}"
        }

        // 🔹 ViewAttachmentManager oluştur
        viewAttachmentManager = ViewAttachmentManager(this, arView)
        viewAttachmentManager.onResume()

        setupArrowNode()
        startSensors()
    }

    private fun setupArrowNode() {
        arrowNode = ViewNode(
            engine = arView.engine,
            modelLoader = arView.modelLoader,
            viewAttachmentManager = viewAttachmentManager
        ).apply {
            isEditable = false
            position = Position(x = 0.0f, y = -0.5f, z = -1.5f)
            rotation = Rotation(x = 0f, y = 0f, z = 0f)

            lifecycleScope.launch {
                loadView(
                    context = this@ArNavigationActivity,
                    layoutResId = R.layout.layout_ar_arrow
                )
            }
        }

        // ⬅️ Yine addChild değil, addChildNode
        arView.addChildNode(arrowNode!!)
    }

    private fun requestLocationPerms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locPermLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        } else {
            startSensors()
        }
    }

    private fun startSensors() {
        compassHelper.start { az ->
            lastAzimuth = az
            updateArrowRotation()
        }

        locationHelper.startLocationUpdates { loc ->
            val res = FloatArray(2)
            Location.distanceBetween(
                loc.latitude,
                loc.longitude,
                targetLat,
                targetLng,
                res
            )
            lastBearingTo = res[1]
            infoText.text = "Hedefe: ${res[0].toInt()}m"
            updateArrowRotation()
        }
    }

    private fun updateArrowRotation() {
        val node = arrowNode ?: return
        var heading = lastBearingTo - lastAzimuth

        while (heading < -180f) heading += 360f
        while (heading > 180f) heading -= 360f

        node.rotation = Rotation(0f, 0f, -heading)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationHelper.isInitialized) locationHelper.stopLocationUpdates()
        if (::compassHelper.isInitialized) compassHelper.stop()
    }
}

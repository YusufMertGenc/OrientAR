package com.example.ardeneme

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ardeneme.location.LocationHelper
// SceneView 2.0.3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.node.ViewNode
import io.github.sceneview.math.Position
import com.google.ar.core.Config
import com.google.ar.sceneform.rendering.ViewAttachmentManager   // ⬅️ ÖNEMLİ: yeni import
import kotlinx.coroutines.launch

class TreasureHuntActivity : AppCompatActivity() {

    private lateinit var arView: ARSceneView
    private lateinit var infoText: TextView
    private lateinit var locationHelper: LocationHelper

    // ViewNode için kendi AttachmentManager’ımız
    private lateinit var viewAttachmentManager: ViewAttachmentManager

    private var treasureNode: ViewNode? = null
    private var isTreasureFound = false

    private val treasureLat = 35.24812
    private val treasureLng = 33.02244

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) {
                initGame()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        arView = findViewById(R.id.arView)
        infoText = findViewById(R.id.infoText)

        checkPermissions()
    }

    private fun checkPermissions() {
        val perms = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            initGame()
        } else {
            permLauncher.launch(perms)
        }
    }

    private fun initGame() {
        locationHelper = LocationHelper(this)

        // ARSceneView lifecycle
        arView.lifecycle = lifecycle

        arView.configureSession { _, config ->
            config.focusMode = Config.FocusMode.AUTO
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
        }

        arView.onSessionFailed = { exception ->
            infoText.text = "Hata: ${exception.message}"
        }

        // 🔹 Kendi ViewAttachmentManager'ımızı oluşturuyoruz
        viewAttachmentManager = ViewAttachmentManager(this, arView)
        viewAttachmentManager.onResume()

        setupTreasureObject()
        startTracking()
    }

    private fun setupTreasureObject() {
        // 2.0.3 için tüm parametreler
        treasureNode = ViewNode(
            engine = arView.engine,
            modelLoader = arView.modelLoader,
            viewAttachmentManager = viewAttachmentManager   // ⬅️ artık burası
        ).apply {
            isEditable = false
            position = Position(x = 0.0f, y = -0.5f, z = -2.0f)
            isVisible = false

            lifecycleScope.launch {
                loadView(
                    context = this@TreasureHuntActivity,
                    layoutResId = R.layout.layout_ar_treasure
                )
            }

            onSingleTapConfirmed = {
                collectTreasure()
                true
            }
        }

        // ⬅️ addChild YOK, 2.0.3'te addChildNode kullanılıyor
        arView.addChildNode(treasureNode!!)
    }

    private fun startTracking() {
        locationHelper.startLocationUpdates { loc ->
            if (isTreasureFound) return@startLocationUpdates

            val results = FloatArray(1)
            Location.distanceBetween(
                loc.latitude,
                loc.longitude,
                treasureLat,
                treasureLng,
                results
            )
            val distance = results[0]

            if (distance < 15.0) {
                infoText.text = "Hazine çok yakın! Etrafına bak!"
                if (treasureNode?.isVisible == false) {
                    treasureNode?.isVisible = true
                    Toast.makeText(this, "Bir şeyler belirdi!", Toast.LENGTH_SHORT).show()
                }
            } else {
                infoText.text = "Hazineye Mesafe: ${distance.toInt()}m"
                treasureNode?.isVisible = false
            }
        }
    }

    private fun collectTreasure() {
        isTreasureFound = true
        infoText.text = "TEBRİKLER! BULDUN!"
        Toast.makeText(this, "+100 Puan!", Toast.LENGTH_LONG).show()
        treasureNode?.isVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationHelper.isInitialized) locationHelper.stopLocationUpdates()
    }
}

package com.example.ardeneme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class CampusTourActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var btnModeMap: Button
    private lateinit var btnModeAR: Button
    private lateinit var btnStart: Button

    private val currentLocationName = "Current Location"
    private var isARMode = false // Varsayılan 2D

    // Konum İzni
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        ) {
            enableMyLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_tour)

        // UI
        spinnerFrom = findViewById(R.id.spinnerFrom)
        spinnerTo = findViewById(R.id.spinnerTo)
        btnModeMap = findViewById(R.id.btnModeMap)
        btnModeAR = findViewById(R.id.btnModeAR)
        btnStart = findViewById(R.id.btnStartNavigation)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupSpinners()
        setupButtons()
    }

    private fun setupSpinners() {
        val placeNames = CampusData.places.map { it.name }.toMutableList()

        // FROM
        val fromList = mutableListOf(currentLocationName)
        fromList.addAll(placeNames)
        val fromAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fromList)
        fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrom.adapter = fromAdapter

        // TO
        val toAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, placeNames)
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTo.adapter = toAdapter

        // Dinleyici: From değişirse AR butonunu kontrol et
        spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString()
                if (selected != currentLocationName) {
                    // AR devre dışı
                    btnModeAR.isEnabled = false
                    btnModeAR.alpha = 0.5f
                    selectMode(false) // Map'e zorla
                } else {
                    // AR aktif
                    btnModeAR.isEnabled = true
                    btnModeAR.alpha = 1.0f
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        btnModeMap.setOnClickListener { selectMode(false) }
        btnModeAR.setOnClickListener { selectMode(true) }

        btnStart.setOnClickListener {
            startNavigation()
        }
    }

    private fun selectMode(arMode: Boolean) {
        isARMode = arMode
        if (arMode) {
            // AR Seçili: AR butonu Kırmızı, Map butonu Gri
            btnModeAR.setBackgroundColor(Color.parseColor("#D32F2F"))
            btnModeAR.setTextColor(Color.WHITE)
            btnModeMap.setBackgroundColor(Color.parseColor("#EEEEEE"))
            btnModeMap.setTextColor(Color.BLACK)
        } else {
            // Map Seçili: Map butonu Kırmızı, AR butonu Gri
            btnModeMap.setBackgroundColor(Color.parseColor("#D32F2F"))
            btnModeMap.setTextColor(Color.WHITE)
            btnModeAR.setBackgroundColor(Color.parseColor("#EEEEEE"))
            btnModeAR.setTextColor(Color.BLACK)
        }
    }

    private fun startNavigation() {
        val fromName = spinnerFrom.selectedItem.toString()
        val toName = spinnerTo.selectedItem.toString()
        val targetPlace = CampusData.places.find { it.name == toName } ?: return

        // 1. AR MODU (3D)
        if (isARMode) {
            val intent = Intent(this, ArNavigationActivity::class.java)
            intent.putExtra("targetLat", targetPlace.location.latitude)
            intent.putExtra("targetLng", targetPlace.location.longitude)
            intent.putExtra("targetName", targetPlace.name)
            startActivity(intent)
        }
        // 2. HARİTA MODU (2D) - Google Maps Navigasyonu
        else {
            val uriString = if (fromName == currentLocationName) {
                // Mevcut konumdan hedefe yürüyüş rotası
                "google.navigation:q=${targetPlace.location.latitude},${targetPlace.location.longitude}&mode=w"
            } else {
                // İki nokta arası rota (From -> To)
                val fromPlace = CampusData.places.find { it.name == fromName }
                if (fromPlace != null) {
                    "http://maps.google.com/maps?saddr=${fromPlace.location.latitude},${fromPlace.location.longitude}&daddr=${targetPlace.location.latitude},${targetPlace.location.longitude}&mode=w"
                } else {
                    return
                }
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Google Maps yüklü değilse tarayıcıda aç
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                startActivity(browserIntent)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val center = LatLng(35.248, 33.021)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 16f))
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
        } else {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}
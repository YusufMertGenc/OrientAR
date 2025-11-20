package com.example.ardeneme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class CampusTourActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    // GÜNCEL KOORDİNAT: ODTÜ KKK
    private val campusCenter = LatLng(35.24812, 33.02244)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_tour)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<Button>(R.id.btnSwitchToAR).setOnClickListener {
            val intent = Intent(this, ArNavigationActivity::class.java)
            intent.putExtra("targetLat", campusCenter.latitude)
            intent.putExtra("targetLng", campusCenter.longitude)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 17f))
        mMap.addMarker(MarkerOptions().position(campusCenter).title("ODTÜ KKK Hedef"))
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
    }
}
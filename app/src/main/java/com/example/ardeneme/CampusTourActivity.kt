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
    // ODTÜ KKK Kampüs Meydanı Koordinatı
    private val campusCenter = LatLng(35.186, 33.024)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campus_tour)

        // Harita Fragmentini Yükle
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // AR Moduna Geçiş Butonu
        findViewById<Button>(R.id.btnSwitchToAR).setOnClickListener {
            val intent = Intent(this, ArNavigationActivity::class.java)
            // Hedef koordinatı AR ekranına gönderiyoruz
            intent.putExtra("targetLat", campusCenter.latitude)
            intent.putExtra("targetLng", campusCenter.longitude)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Kamerayı kampüse odakla
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 16f))
        // Kırmızı bir pin ekle
        mMap.addMarker(MarkerOptions().position(campusCenter).title("Kampüs Meydanı"))
    }
}
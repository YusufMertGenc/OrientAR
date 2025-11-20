package com.example.ardeneme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnCampusTour).setOnClickListener {
            startActivity(Intent(this, CampusTourActivity::class.java))
        }

        findViewById<Button>(R.id.btnTreasureHunt).setOnClickListener {
            startActivity(Intent(this, TreasureHuntActivity::class.java))
        }

        findViewById<Button>(R.id.btnFaq).setOnClickListener {
            Toast.makeText(this, "Chatbot yakında...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSocieties).setOnClickListener {
            Toast.makeText(this, "Topluluklar yakında...", Toast.LENGTH_SHORT).show()
        }
    }
}
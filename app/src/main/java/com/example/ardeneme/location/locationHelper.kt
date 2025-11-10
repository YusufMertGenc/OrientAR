package com.example.ardeneme.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*

class LocationHelper(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L // 1 saniyede bir konum
    )
        .setMinUpdateIntervalMillis(500L)
        .setMaxUpdateDelayMillis(1500L)
        .build()

    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onUpdate: (Location) -> Unit) {
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(onUpdate)
            }
        }
        callback = cb
        client.requestLocationUpdates(request, cb, context.mainLooper)
    }

    fun stopLocationUpdates() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }
}

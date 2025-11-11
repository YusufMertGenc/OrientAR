package com.example.ardeneme.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*

class LocationHelper(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 2000L
    ).build()

    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocation: (Location) -> Unit) {
        if (callback != null) return

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(onLocation)
            }
        }
        client.requestLocationUpdates(request, callback!!, null)
    }

    fun stopLocationUpdates() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }
}

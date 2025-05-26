package com.example.eform.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    interface LocationCallback {
        fun onLocationResult(lat: Double, lng: Double)
        fun onError(message: String)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: LocationCallback) {
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                callback.onLocationResult(location.latitude, location.longitude)
            } else {
                callback.onError("Lokasi tidak ditemukan")
            }
        }.addOnFailureListener {
            callback.onError("Gagal mendapatkan lokasi: ${it.localizedMessage}")
        }
    }

    fun isWithinRadius(
        currentLat: Double,
        currentLng: Double,
        targetLat: Double,
        targetLng: Double,
        radiusInMeters: Double = 100.0
    ): Boolean {
        val result = FloatArray(1)
        Location.distanceBetween(currentLat, currentLng, targetLat, targetLng, result)
        return result[0] <= radiusInMeters
    }
}

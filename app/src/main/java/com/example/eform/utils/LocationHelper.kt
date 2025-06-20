package com.example.eform.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority // Import Priority

// Import Constants
import com.example.eform.utils.Constants // Tambahkan import ini

class LocationHelper(private val context: Context) {

    // Interface callback KUSTOM untuk menerima update lokasi dari HELPER ini
    interface LocationCallback {
        fun onLocationResult(lat: Double, lng: Double)
        fun onError(message: String)
    }

    private var fusedLocationClient: FusedLocationProviderClient? = null
    // Variabel untuk menyimpan instance LocationCallback DARI GMS SERVICES
    private var gmsLocationCallback: com.google.android.gms.location.LocationCallback? = null


    // Metode untuk mendapatkan lokasi saat ini sekali
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: LocationCallback) { // Menerima callback KUSTOM Anda
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (fusedLocationClient != null) {
            fusedLocationClient?.lastLocation
                ?.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        callback.onLocationResult(location.latitude, location.longitude)
                    } else {
                        callback.onError("Lokasi tidak tersedia.")
                    }
                }
                ?.addOnFailureListener { e ->
                    callback.onError("Gagal mendapatkan lokasi: ${e.message}")
                }
        } else {
            callback.onError("Izin lokasi tidak diberikan atau FusedLocationProviderClient tidak siap.")
        }
    }

    // Metode untuk meminta pembaruan lokasi secara terus-menerus
    @SuppressLint("MissingPermission") // Izin akan diperiksa di Composable
    fun requestLocationUpdates(callback: LocationCallback) { // Menerima callback KUSTOM Anda
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Konfigurasi LocationRequest
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, // Akurasi tinggi
            Constants.LOCATION_UPDATE_INTERVAL.toLong() // Menggunakan konstanta dan konversi ke Long
        )
            .setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL.toLong()) // Menggunakan konstanta dan konversi ke Long
            .build()

        // Inisialisasi LocationCallback DARI GMS SERVICES
        gmsLocationCallback = object : com.google.android.gms.location.LocationCallback() { // Perhatikan explicit package
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    callback.onLocationResult(location.latitude, location.longitude) // Meneruskan ke callback KUSTOM Anda
                } ?: callback.onError("Lokasi tidak tersedia.")
            }

            override fun onLocationAvailability(locationAvailability: com.google.android.gms.location.LocationAvailability) {
                if (!locationAvailability.isLocationAvailable) {
                    callback.onError("Lokasi tidak tersedia.")
                }
            }
        }

        // Memanggil requestLocationUpdates dengan GMS LocationCallback
        fusedLocationClient?.requestLocationUpdates(locationRequest, gmsLocationCallback!!, Looper.getMainLooper())
    }

    // Metode untuk menghentikan pembaruan lokasi
    fun stopLocationUpdates() {
        if (fusedLocationClient != null && gmsLocationCallback != null) {
            fusedLocationClient?.removeLocationUpdates(gmsLocationCallback!!)
            fusedLocationClient = null
            gmsLocationCallback = null
        }
    }

    // Fungsi untuk memeriksa apakah lokasi berada dalam radius
    fun isWithinRadius(
        currentLat: Double,
        currentLng: Double,
        targetLat: Double,
        targetLng: Double,
        radiusInMeters: Double
    ): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(currentLat, currentLng, targetLat, targetLng, results)
        val distanceInMeters = results[0]
        return distanceInMeters <= radiusInMeters
    }
}
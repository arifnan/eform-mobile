// File: app/src/main/java/com/example/eform/utils/Constants.kt
package com.example.eform.utils

object Constants {
    const val BASE_URL = "https://e-form2.ilta-services.tech/api/"
    //    const val APP_DEEP_LINK_SCHEME = "eformapp"
//    const val APP_DEEP_LINK_HOST = "openform"
    const val APP_DEEP_LINK_SCHEME = "https"
    const val APP_DEEP_LINK_HOST = "e-form2.ilta-services.tech"
    // Tambahkan konstanta untuk validasi    lokasi
    const val LOCATION_VALIDATION_RADIUS_METERS = 100.0
    // Contoh target lokasi default (misalnya, titik tengah area sekolah/kantor)
    // Anda mungkin ingin mengambil target lokasi ini dari server atau konfigurasi lain nantinya.
    const val DEFAULT_TARGET_LATITUDE = 3.5917 // Contoh: Medan
    const val DEFAULT_TARGET_LONGITUDE = 98.6753 // Contoh: Medan

    // --- TAMBAHKAN KONSTANTA INTERVAL LOKASI INI ---
    const val LOCATION_UPDATE_INTERVAL = 10000 // 10 seconds
    const val LOCATION_FASTEST_INTERVAL = 5000 // 5 seconds
    // --- AKHIR PENAMBAHAN ---
}
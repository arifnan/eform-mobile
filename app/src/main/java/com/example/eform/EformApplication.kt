package com.example.eform

import android.app.Application
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.utils.NotificationHelper

class EformApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitInstance.initialize(this)
        NotificationHelper.createNotificationChannel(this) // Inisialisasi channel notifikasi juga di sini
    }
}
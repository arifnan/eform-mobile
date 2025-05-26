package com.example.eform.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.eform.MainActivity // Asumsi MainActivity adalah entry point Anda
import com.example.eform.R // Pastikan Anda punya ikon notifikasi di drawable
import com.example.eform.navigation.Screen // Untuk deep link ke NotificationScreen

object NotificationHelper {

    private const val CHANNEL_ID = "eform_notification_channel"
    private const val CHANNEL_NAME = "E-Form Notifications"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel untuk notifikasi aplikasi E-Form"
                // Atur properti channel lain jika perlu (lampu, getar, dll.)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int, // ID unik untuk setiap notifikasi
        title: String,
        message: String,
        targetScreenRoute: String? = null // Rute tujuan saat notifikasi diklik
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent untuk membuka aplikasi saat notifikasi diklik
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Jika ada targetScreenRoute, kita bisa tambahkan sebagai data ke intent
            // untuk ditangani oleh MainActivity (deep linking ke NotificationScreen)
            if (targetScreenRoute != null) {
                putExtra("target_screen", targetScreenRoute)
            }
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlag)


        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon) // GANTI DENGAN IKON NOTIFIKASI ANDA
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // Aksi saat notifikasi diklik
            .setAutoCancel(true) // Hapus notifikasi setelah diklik

        notificationManager.notify(notificationId, builder.build())
    }
}
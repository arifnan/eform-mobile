package com.example.eform

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.eform.navigation.AppNavHost
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.utils.Constants
import com.example.eform.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private var loggedInUserIdentifier: String? = null // Simpan userIdentifier yang login

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(applicationContext) // Buat channel notifikasi

        setContent {
            EformTheme {
                navController = rememberNavController()

                AppNavHost(
                    navController = navController,
                    onLoginSuccess = { identifier -> // Callback untuk menyimpan identifier setelah login
                        loggedInUserIdentifier = identifier
                    }
                )
            }
        }
        // Tangani intent yang mungkin membuka aplikasi (baik deep link atau notifikasi)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update intent yang ada di activity
        // Tangani intent baru jika aplikasi sudah berjalan
        handleIntent(intent)
    }

    /**
     * Fungsi pusat untuk menangani semua jenis intent (deep link & notifikasi).
     * Ini dipanggil di onCreate dan onNewIntent.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null || !::navController.isInitialized) {
            return
        }

        // Prioritaskan penanganan deep link. Jika intent adalah deep link,
        // fungsi handleDeepLink akan mengembalikan true dan kita tidak perlu lanjut.
        val isDeepLinkHandled = handleDeepLink(intent)

        if (!isDeepLinkHandled) {
            // Jika bukan deep link, coba tangani sebagai intent notifikasi.
            handleNotificationIntent(intent)
        }
    }

    /**
     * Merevisi logika untuk hanya mem-parsing deep link dan menavigasi
     * ke layar login siswa dengan membawa 'formCode'. Logika selanjutnya
     * (setelah login berhasil) akan ditangani oleh LoginStudentsScreen.
     *
     * @return True jika intent berhasil ditangani sebagai deep link, false jika tidak.
     */
    private fun handleDeepLink(intent: Intent): Boolean {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            // Cek apakah URI cocok dengan skema deep link aplikasi
            if (uri != null && uri.scheme == Constants.APP_DEEP_LINK_SCHEME && uri.host == Constants.APP_DEEP_LINK_HOST) {
                // Ambil segmen terakhir dari path, yaitu kode formulir (contoh: "BRZNXUCO")
                val formCode = uri.lastPathSegment
                if (!formCode.isNullOrEmpty()) {
                    // Langsung arahkan ke halaman Login Siswa dan sertakan formCode sebagai argumen.
                    // Screen.LoginStudents.createRoute() adalah fungsi helper yang sudah kita siapkan.
                    navController.navigate(Screen.LoginStudents.createRoute(formCode)) {
                        // Hapus semua backstack sebelumnya agar pengguna tidak bisa kembali ke halaman awal
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                    // Kosongkan intent setelah ditangani agar tidak diproses ulang saat activity dibuat ulang (misal: rotasi layar)
                    setIntent(Intent())
                    return true // Mengindikasikan deep link telah ditangani
                }
            }
        }
        return false // Bukan deep link yang valid
    }

    /**
     * Logika untuk menangani intent dari notifikasi. Logika ini dipertahankan
     * seperti sebelumnya tanpa perubahan.
     */
    private fun handleNotificationIntent(intent: Intent) {
        if (intent.hasExtra("target_screen")) {
            val targetScreen = intent.getStringExtra("target_screen")
            // Cek apakah targetnya adalah halaman notifikasi
            if (targetScreen == Screen.Notification.route) { // Ini adalah "notification/{userIdentifier}"
                if (loggedInUserIdentifier != null) {
                    // Jika pengguna sudah login, buka halaman notifikasi
                    navController.navigate(
                        Screen.Notification.route.replace("{userIdentifier}", loggedInUserIdentifier!!)
                    ) {
                        launchSingleTop = true
                    }
                } else {
                    // Jika tidak ada user yang login, arahkan ke halaman pemilihan peran
                    navController.navigate(Screen.Role.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                    Toast.makeText(this, "Silakan login untuk melihat notifikasi", Toast.LENGTH_SHORT).show()
                }
                // Hapus extra dari intent agar tidak diproses berulang kali
                intent.removeExtra("target_screen")
            }
        }
    }
}
package com.example.eform

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.navigation.AppNavHost
import com.example.eform.navigation.Screen
import com.example.eform.utils.Constants
import com.example.eform.utils.NotificationHelper
import com.example.eform.ui.theme.EformTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private var loggedInUserIdentifier: String? = null // Simpan userIdentifier yang login

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(applicationContext) // Buat channel notifikasi

        setContent {
            EformTheme {
                navController = rememberNavController()
                // Fungsi untuk mendapatkan userIdentifier dari suatu tempat (misal, setelah login berhasil)
                // Untuk saat ini, kita akan asumsikan ini di-set setelah login
                // dan bisa diakses untuk meneruskannya ke NotificationScreen jika diperlukan.
                // Ini adalah bagian yang perlu Anda integrasikan dengan sistem login Anda.
                // Contoh: loggedInUserIdentifier = viewModel.currentUser.value?.identifier

                AppNavHost(navController = navController,
                    onLoginSuccess = { identifier -> // Tambahkan callback untuk menyimpan identifier
                        loggedInUserIdentifier = identifier
                    }
                )
                handleDeepLink(intent, navController)
                handleNotificationIntent(intent, navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLink(intent, navController)
            handleNotificationIntent(intent, navController)
        }
    }

    private fun handleNotificationIntent(intent: Intent?, currentNavController: NavHostController) {
        if (intent?.hasExtra("target_screen") == true) {
            val targetScreen = intent.getStringExtra("target_screen")
            if (targetScreen == Screen.Notification.route) { // Ini adalah "notification/{userIdentifier}"
                // Coba dapatkan userIdentifier yang sedang login
                // Jika tidak ada, mungkin arahkan ke login dulu atau tampilkan pesan
                if (loggedInUserIdentifier != null) {
                    currentNavController.navigate(
                        Screen.Notification.route.replace("{userIdentifier}", loggedInUserIdentifier!!)
                    ) {
                        launchSingleTop = true
                    }
                } else {
                    // Jika tidak ada user yang login, arahkan ke halaman pemilihan peran atau login
                    currentNavController.navigate(Screen.Role.route) {
                        popUpTo(currentNavController.graph.startDestinationId) { inclusive = true}
                        launchSingleTop = true
                    }
                    Toast.makeText(this, "Silakan login untuk melihat notifikasi", Toast.LENGTH_SHORT).show()
                }
                intent.removeExtra("target_screen") // Hapus extra agar tidak diproses lagi
            }
        }
    }

    private fun handleDeepLink(intent: Intent?, currentNavController: NavHostController) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null && uri.scheme == Constants.APP_DEEP_LINK_SCHEME && uri.host == Constants.APP_DEEP_LINK_HOST) {
                val pathSegments = uri.pathSegments
                if (pathSegments.isNotEmpty()) {
                    val formCodeFromLink = pathSegments[0]

                    val db = AppDatabase.getDatabase(this)
                    val formDao = db.formDao()

                    lifecycleScope.launch {
                        val formEntity = formDao.getFormByCode(formCodeFromLink)
                        if (formEntity != null) {
                            // Saat membuka form dari deep link, kita perlu userIdentifier siswa
                            // Ini juga perlu mekanisme untuk mengetahui siapa siswa yang login
                            // atau meminta login jika belum.
                            if (loggedInUserIdentifier != null) { // Asumsi ini adalah identifier siswa jika relevan
                                currentNavController.navigate(
                                    Screen.FormAnswer.route
                                        .replace("{formId}", "${formEntity.id}")
                                        .replace("{userIdentifier}", loggedInUserIdentifier!!) // Kirim juga userIdentifier siswa
                                ) {
                                    launchSingleTop = true
                                }
                            } else {
                                // Jika siswa belum login, arahkan ke login siswa,
                                // mungkin dengan parameter tambahan untuk kembali ke form ini setelah login
                                currentNavController.navigate(Screen.LoginStudents.route) {
                                    popUpTo(currentNavController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                                Toast.makeText(this@MainActivity, "Silakan login sebagai siswa untuk mengisi formulir", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Formulir dari link tidak ditemukan.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}
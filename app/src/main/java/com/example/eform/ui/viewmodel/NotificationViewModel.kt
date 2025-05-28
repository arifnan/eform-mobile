package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    // Inisialisasi DAO
    private val notificationDao = AppDatabase.getDatabase(application).notificationDao()
    private val userDao = AppDatabase.getDatabase(application).userDao()

    // StateFlow untuk daftar notifikasi
    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications

    // StateFlow untuk status loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentUserId: Int? = null // Untuk menyimpan ID pengguna yang notifikasinya dimuat

    // Fungsi untuk memuat notifikasi berdasarkan userIdentifier (NIP atau Email)
    fun loadNotifications(userIdentifier: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Dapatkan userId dari userIdentifier
            currentUserId = withContext(Dispatchers.IO) {
                // Coba NIP (guru) dulu, lalu email (siswa)
                userDao.getUserByNip(userIdentifier)?.id ?: userDao.getUserByEmail(userIdentifier)?.id
            }

            currentUserId?.let { userId ->
                // Ambil notifikasi dari database lokal
                _notifications.value = withContext(Dispatchers.IO) {
                    notificationDao.getNotificationsForUser(userId)
                }
            } ?: run {
                _notifications.value = emptyList() // Jika user tidak ditemukan, kosongkan list
            }
            _isLoading.value = false
        }
    }

    // Fungsi untuk menghapus notifikasi berdasarkan ID-nya
    fun deleteNotification(notificationId: Int) {
        val userIdForRefresh = currentUserId // Simpan userId saat ini untuk refresh
        if (userIdForRefresh == null) return // Tidak bisa refresh jika userId tidak diketahui

        viewModelScope.launch(Dispatchers.IO) { // Operasi database di IO thread
            notificationDao.deleteNotificationById(notificationId)
            // Muat ulang daftar notifikasi setelah menghapus untuk memperbarui UI
            _notifications.value = notificationDao.getNotificationsForUser(userIdForRefresh)
        }
    }

    // ViewModelProvider.Factory untuk NotificationViewModel
    class NotificationViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NotificationViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
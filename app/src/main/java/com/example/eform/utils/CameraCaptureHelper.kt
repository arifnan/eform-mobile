// File: app/src/main/java/com/example/eform/utils/CameraCaptureHelper.kt
package com.example.eform.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraCaptureHelper(private val context: Context) {

    private val _photoUri = MutableStateFlow<Uri?>(null)
    val photoUri: StateFlow<Uri?> = _photoUri.asStateFlow()

    // Metode publik untuk mengatur URI foto dari luar kelas
    fun setPhotoUri(uri: Uri?) {
        _photoUri.value = uri
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun createCameraIntent(): Intent {
        val photoFile = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
        photoFile.createNewFile()
        // Set URI ke _photoUri internal helper ini agar bisa diakses oleh launcher
        _photoUri.value = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, _photoUri.value)
    }

    // Method untuk membuat URI baru tanpa langsung memicu Intent
    @SuppressLint("SimpleDateFormat")
    fun createImageUri(): Uri? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File(context.cacheDir, "JPEG_${timeStamp}_.jpg")
        photoFile.createNewFile()
        // Set URI ke _photoUri internal helper ini
        _photoUri.value = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        return _photoUri.value
    }
}
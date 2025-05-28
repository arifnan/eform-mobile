package com.example.eform.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.local.UserPreferences
import com.example.eform.data.model.api.UserApiModel
import com.example.eform.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val user: UserApiModel) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class UpdateProfileResult {
    object Idle : UpdateProfileResult()
    object Loading : UpdateProfileResult()
    data class Success(val updatedUser: UserApiModel, val message: String) : UpdateProfileResult()
    data class Error(val message: String) : UpdateProfileResult()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val authRepository = AuthRepository(RetrofitInstance.api, userPreferences)

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _updateResult = MutableStateFlow<UpdateProfileResult>(UpdateProfileResult.Idle)
    val updateResult: StateFlow<UpdateProfileResult> = _updateResult.asStateFlow()

    val editableName = MutableStateFlow("")
    val editableAddress = MutableStateFlow("")
    val profileImageUri = MutableStateFlow<Uri?>(null) // URI dari image picker
    val currentProfilePhotoUrl = MutableStateFlow<String?>(null) // URL foto dari server

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = authRepository.getAuthenticatedUser()
            result.fold(
                onSuccess = { userApiModel ->
                    _uiState.value = ProfileUiState.Success(userApiModel)
                    editableName.value = userApiModel.name
                    editableAddress.value = userApiModel.address ?: ""
                    currentProfilePhotoUrl.value = userApiModel.profilePhotoUrl
                    profileImageUri.value = null // Reset pilihan foto baru
                },
                onFailure = { exception ->
                    _uiState.value = ProfileUiState.Error(exception.message ?: "Gagal memuat profil pengguna")
                }
            )
        }
    }

    fun onNameChanged(newName: String) {
        editableName.value = newName
    }

    fun onAddressChanged(newAddress: String) {
        editableAddress.value = newAddress
    }

    fun onProfileImageUriChanged(uri: Uri?) {
        profileImageUri.value = uri
    }

    // Helper function to convert URI to File (harus dijalankan di context yang sesuai, misal dari Composable)
    private fun getFileFromUri(context: Context, uri: Uri, fileNamePrefix: String): File? {
        return try {
            val tempFile = File(context.cacheDir, "${fileNamePrefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error converting URI to File: ${e.message}")
            null
        }
    }


    fun saveProfileChanges(contextForFile: Context /* Diperlukan untuk konversi URI ke File */) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) {
            _updateResult.value = UpdateProfileResult.Error("Data pengguna saat ini tidak tersedia.")
            return
        }

        val newName = editableName.value
        val newAddress = editableAddress.value
        val newImageUri = profileImageUri.value
        var photoFileToUpload: File? = null

        if (newName.isBlank()) {
            _updateResult.value = UpdateProfileResult.Error("Nama tidak boleh kosong.")
            return
        }

        newImageUri?.let { uri ->
            photoFileToUpload = getFileFromUri(contextForFile, uri, "profile_photo")
            if (photoFileToUpload == null) {
                _updateResult.value = UpdateProfileResult.Error("Gagal memproses file gambar profil.")
                return // Hentikan jika file tidak bisa dibuat
            }
        }

        _updateResult.value = UpdateProfileResult.Loading
        viewModelScope.launch {
            // Hanya kirim field yang berubah atau ada nilainya.
            // API Laravel Anda mungkin mengabaikan field null atau string kosong.
            val nameToSend = if (newName != currentState.user.name) newName else null
            val addressToSend = if (newAddress != (currentState.user.address ?: "")) newAddress.ifBlank { null } else null


            val result = authRepository.updateUserProfile(
                name = nameToSend,
                address = addressToSend,
                profilePhotoFile = photoFileToUpload
            )

            result.fold(
                onSuccess = { updatedUserApiModel ->
                    _uiState.value = ProfileUiState.Success(updatedUserApiModel)
                    editableName.value = updatedUserApiModel.name
                    editableAddress.value = updatedUserApiModel.address ?: ""
                    currentProfilePhotoUrl.value = updatedUserApiModel.profilePhotoUrl
                    profileImageUri.value = null // Reset URI setelah sukses
                    photoFileToUpload?.delete() // Hapus file temporer
                    _updateResult.value = UpdateProfileResult.Success(updatedUserApiModel, "Profil berhasil diperbarui.")
                },
                onFailure = { exception ->
                    photoFileToUpload?.delete() // Hapus file temporer jika gagal juga
                    _updateResult.value = UpdateProfileResult.Error(exception.message ?: "Gagal memperbarui profil.")
                }
            )
        }
    }

    fun resetUpdateResult() {
        _updateResult.value = UpdateProfileResult.Idle
    }

    class ProfileViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
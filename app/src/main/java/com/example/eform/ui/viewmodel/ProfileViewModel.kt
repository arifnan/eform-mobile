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
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.database.UserDao
import com.example.eform.data.local.UserPreferences
import com.example.eform.data.model.UserEntity
import com.example.eform.data.model.api.UserApiModel
import com.example.eform.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

// Sealed class untuk state UI (sudah benar)
sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val user: UserApiModel) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

// Sealed class untuk hasil update (sudah benar)
sealed class UpdateProfileResult {
    object Idle : UpdateProfileResult()
    object Loading : UpdateProfileResult()
    data class Success(val updatedUser: UserApiModel, val message: String) : UpdateProfileResult()
    data class Error(val message: String) : UpdateProfileResult()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val authRepository = AuthRepository(RetrofitInstance.api, userPreferences)
    private val userDao: UserDao = AppDatabase.getDatabase(application).userDao()

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _updateResult = MutableStateFlow<UpdateProfileResult>(UpdateProfileResult.Idle)
    val updateResult: StateFlow<UpdateProfileResult> = _updateResult.asStateFlow()

    // StateFlow untuk field yang bisa diedit (sudah benar)
    val editableName = MutableStateFlow("")
    val editableAddress = MutableStateFlow("")
    val editableGrade = MutableStateFlow("") // Tambahkan untuk Grade jika bisa diedit
    val editableSubject = MutableStateFlow("") // Tambahkan untuk Subject jika bisa diedit
    val profileImageUri = MutableStateFlow<Uri?>(null)
    val currentProfilePhotoUrl = MutableStateFlow<String?>(null)


    init {
        loadUserProfile()
    }

    // Fungsi loadUserProfile (sudah benar)
    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val result = authRepository.getAuthenticatedUser()
            result.fold(
                onSuccess = { userApiModel ->
                    Log.d("PROFILE_VM_LOAD", "User API Model received: $userApiModel")
                    _uiState.value = ProfileUiState.Success(userApiModel)
                    editableName.value = userApiModel.name ?: ""
                    editableAddress.value = userApiModel.address ?: ""
                    editableGrade.value = userApiModel.grade ?: "" // Isi dari API
                    editableSubject.value = userApiModel.subject ?: "" // Isi dari API
                    currentProfilePhotoUrl.value = userApiModel.profilePhotoUrl
                    profileImageUri.value = null
                    saveOrUpdateUserInLocalDb(userApiModel, "loadUserProfile")
                },
                onFailure = { exception ->
                    Log.e("PROFILE_VM_LOAD", "Failed to load user profile: ${exception.message}", exception)
                    _uiState.value = ProfileUiState.Error(exception.message ?: "Gagal memuat profil pengguna")
                }
            )
        }
    }

    // Fungsi saveOrUpdateUserInLocalDb (sudah benar)
    private fun saveOrUpdateUserInLocalDb(userApiModel: UserApiModel, sourceTag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingUser = userDao.getUserById(userApiModel.id)
                val passwordForDb = existingUser?.password ?: ""
                val entityName = userApiModel.name ?: "N/A"
                val entityEmail = userApiModel.email ?: "N/A"
                val entityRole = userApiModel.role ?: "unknown"
                val userEntity = UserEntity(
                    id = userApiModel.id,
                    name = entityName,
                    email = entityEmail,
                    nip = userApiModel.nip ?: "",
                    password = passwordForDb,
                    role = entityRole,
                    address = userApiModel.address
                )
                if (existingUser != null) {
                    userDao.updateUser(userEntity)
                    Log.d("PROFILE_VM_DB", "User profile updated in local DB via $sourceTag: $userEntity")
                } else {
                    userDao.insertUser(userEntity)
                    Log.w("PROFILE_VM_DB", "User not found locally via $sourceTag, inserted new: $userEntity")
                }
            } catch (e: Exception) {
                Log.e("PROFILE_VM_DB", "Error saving/updating user profile in local DB via $sourceTag: ${e.message}", e)
            }
        }
    }


    // Fungsi-fungsi on...Changed (sudah benar)
    fun onNameChanged(newName: String) { editableName.value = newName }
    fun onAddressChanged(newAddress: String) { editableAddress.value = newAddress }
    fun onGradeChanged(newGrade: String) { editableGrade.value = newGrade } // Tambahkan untuk grade
    fun onSubjectChanged(newSubject: String) { editableSubject.value = newSubject } // Tambahkan untuk subject
    fun onProfileImageUriChanged(uri: Uri?) { profileImageUri.value = uri }

    // Fungsi getFileFromUri (sudah benar)
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

    // ==========================================================
    // == FUNGSI YANG DIPERBAIKI ADA DI SINI ==
    // ==========================================================
    fun saveProfileChanges(contextForFile: Context) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) {
            _updateResult.value = UpdateProfileResult.Error("Data pengguna saat ini tidak tersedia.")
            return
        }

        if (editableName.value.isBlank()) {
            _updateResult.value = UpdateProfileResult.Error("Nama tidak boleh kosong.")
            return
        }

        // Cek apakah ada perubahan pada data teks atau gambar
        val hasNameChanged = editableName.value != currentState.user.name
        val hasAddressChanged = editableAddress.value != (currentState.user.address ?: "")
        val hasGradeChanged = editableGrade.value != (currentState.user.grade ?: "")
        val hasSubjectChanged = editableSubject.value != (currentState.user.subject ?: "")
        val hasPhotoChanged = profileImageUri.value != null

        // === LOGIKA BARU DIMULAI DI SINI ===
        val isAnythingToUpdate = hasNameChanged || hasAddressChanged || hasGradeChanged || hasSubjectChanged || hasPhotoChanged

        if (!isAnythingToUpdate) {
            // Jika tidak ada yang berubah, langsung tampilkan pesan yang diinginkan dan selesai.
            _updateResult.value = UpdateProfileResult.Success(currentState.user, "Tidak ada data yang diubah.")
            return
        }
        // === LOGIKA BARU SELESAI ===

        _updateResult.value = UpdateProfileResult.Loading
        viewModelScope.launch {
            try {
                // Konversi String ke RequestBody HANYA jika nilainya berubah
                val nameRequestBody = if (hasNameChanged) editableName.value.toRequestBody("text/plain".toMediaTypeOrNull()) else null
                val addressRequestBody = if (hasAddressChanged) editableAddress.value.toRequestBody("text/plain".toMediaTypeOrNull()) else null
                val gradeRequestBody = if (hasGradeChanged) editableGrade.value.toRequestBody("text/plain".toMediaTypeOrNull()) else null
                val subjectRequestBody = if (hasSubjectChanged) editableSubject.value.toRequestBody("text/plain".toMediaTypeOrNull()) else null

                // Konversi File ke MultipartBody.Part HANYA jika ada gambar baru
                var photoFileToUpload: File? = null
                val photoPart: MultipartBody.Part? = profileImageUri.value?.let { uri ->
                    photoFileToUpload = getFileFromUri(contextForFile, uri, "profile_photo")
                    photoFileToUpload?.let { file ->
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("profilePhoto", file.name, requestFile)
                    }
                }

                // Panggil repository (sekarang kita yakin ada sesuatu untuk di-update)
                val result = authRepository.updateUserProfile(
                    name = nameRequestBody,
                    address = addressRequestBody,
                    profilePhoto = photoPart,
                    grade = gradeRequestBody,
                    subject = subjectRequestBody
                )

                result.fold(
                    onSuccess = { updatedUserApiModel ->
                        // Logika sukses yang sudah ada (benar)
                        _uiState.value = ProfileUiState.Success(updatedUserApiModel)
                        editableName.value = updatedUserApiModel.name ?: ""
                        editableAddress.value = updatedUserApiModel.address ?: ""
                        editableGrade.value = updatedUserApiModel.grade ?: ""
                        editableSubject.value = updatedUserApiModel.subject ?: ""
                        currentProfilePhotoUrl.value = updatedUserApiModel.profilePhotoUrl
                        profileImageUri.value = null
                        photoFileToUpload?.delete()
                        saveOrUpdateUserInLocalDb(updatedUserApiModel, "saveProfileChanges_ApiSuccess")
                        _updateResult.value = UpdateProfileResult.Success(updatedUserApiModel, "Profil berhasil diperbarui.")
                    },
                    onFailure = { exception ->
                        // Logika gagal yang sudah ada (benar)
                        photoFileToUpload?.delete()
                        _updateResult.value = UpdateProfileResult.Error(exception.message ?: "Gagal memperbarui profil.")
                    }
                )
            } catch (e: Exception) {
                _updateResult.value = UpdateProfileResult.Error(e.message ?: "Terjadi kesalahan.")
            }
        }
    }

    fun resetUpdateResult() {
        _updateResult.value = UpdateProfileResult.Idle
    }

    // Factory (sudah benar)
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

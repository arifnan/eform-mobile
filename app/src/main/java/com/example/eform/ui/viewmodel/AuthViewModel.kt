package com.example.eform.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.local.UserPreferences
import com.example.eform.data.model.LoginRequest
import com.example.eform.data.model.RegisterRequest
import com.example.eform.data.model.UserEntity
import com.example.eform.data.model.api.AuthResponse
import com.example.eform.data.model.api.UserApiModel
import com.example.eform.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class AuthResult {
    object Idle : AuthResult()
    object Loading : AuthResult()
    data class Success(val authResponse: AuthResponse) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val authRepository = AuthRepository(RetrofitInstance.api, userPreferences)
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val _loginResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val loginResult: StateFlow<AuthResult> = _loginResult.asStateFlow()

    private val _registerResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val registerResult: StateFlow<AuthResult> = _registerResult.asStateFlow()

    private val _logoutResult = MutableStateFlow<Result<Unit>?>(null)
    val logoutResult: StateFlow<Result<Unit>?> = _logoutResult.asStateFlow()

    // Fungsi untuk menyimpan/memperbarui UserApiModel ke UserEntity di database lokal
    private fun saveOrUpdateUserInLocalDb(userApiModel: UserApiModel) {
        viewModelScope.launch(Dispatchers.IO) { // Jalankan di IO thread
            try {
                val existingUser = userDao.getUserByEmail(userApiModel.email) // Cek apakah user sudah ada berdasarkan email

                val userEntity = UserEntity(
                    id = existingUser?.id ?: userApiModel.id, // Gunakan ID dari API jika baru, atau ID lokal jika update
                    // Pastikan UserApiModel.id konsisten atau Anda punya cara lain untuk PK
                    // Jika API tidak mengembalikan ID yang bisa jadi PK di lokal,
                    // Anda mungkin perlu membiarkan id = 0 untuk autoGenerate jika user baru.
                    // Namun, karena Anda menggunakan ID untuk relasi (favorite, notif),
                    // ID dari API harusnya yang jadi acuan.
                    name = userApiModel.name,
                    email = userApiModel.email,
                    nip = userApiModel.nip ?: "", // NIP bisa null dari API, simpan sebagai string kosong jika perlu
                    password = "", // Password tidak disimpan ulang dari API response. Dikelola saat registrasi awal.
                    role = userApiModel.role,
                    address = userApiModel.address
                )

                if (existingUser != null) {
                    userDao.updateUser(userEntity.copy(id = existingUser.id)) // Pastikan ID yang benar digunakan untuk update
                    Log.d("AUTH_VM_LOCAL_DB", "User updated in local DB: $userEntity")
                } else {
                    // Jika API tidak memberikan ID yang bisa jadi PK, dan Anda mengandalkan autoGenerate:
                    // userDao.insertUser(userEntity.copy(id = 0))
                    // Tapi karena Anda butuh ID untuk relasi, idealnya ID dari API (jika unik & stabil)
                    // atau setelah insert, Anda query lagi untuk dapatkan ID lokalnya jika diperlukan segera.
                    // Untuk kasus ini, kita asumsikan UserApiModel.id adalah PK yang valid.
                    userDao.insertUser(userEntity) // Jika ID dari API bisa jadi PK
                    Log.d("AUTH_VM_LOCAL_DB", "New user inserted into local DB: $userEntity")
                }
            } catch (e: Exception) {
                Log.e("AUTH_VM_LOCAL_DB", "Error saving/updating user in local DB: ${e.message}", e)
            }
        }
    }

    fun login(loginRequest: LoginRequest) {
        viewModelScope.launch {
            _loginResult.value = AuthResult.Loading
            val result = authRepository.login(loginRequest)
            result.fold(
                onSuccess = { authResponse ->
                    saveOrUpdateUserInLocalDb(authResponse.user) // <-- PANGGIL FUNGSI SIMPAN/UPDATE
                    _loginResult.value = AuthResult.Success(authResponse)
                },
                onFailure = { exception -> _loginResult.value = AuthResult.Error(exception.message ?: "Login gagal") }
            )
        }
    }

    fun register(registerRequest: RegisterRequest) {
        viewModelScope.launch {
            _registerResult.value = AuthResult.Loading
            val result = authRepository.register(registerRequest)
            result.fold(
                onSuccess = { authResponse ->
                    saveOrUpdateUserInLocalDb(authResponse.user) // <-- PANGGIL FUNGSI SIMPAN/UPDATE
                    _registerResult.value = AuthResult.Success(authResponse)
                },
                onFailure = { exception -> _registerResult.value = AuthResult.Error(exception.message ?: "Registrasi gagal") }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Hasil logout bisa diabaikan jika hanya ingin clear token
            authRepository.logout()
            _logoutResult.value = Result.success(Unit) // Tandai logout selesai
        }
    }

    fun resetLoginResult() { _loginResult.value = AuthResult.Idle }
    fun resetRegisterResult() { _registerResult.value = AuthResult.Idle }
    fun resetLogoutResult() { _logoutResult.value = null }

    suspend fun getToken(): String? = userPreferences.getToken.firstOrNull()

    class AuthViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
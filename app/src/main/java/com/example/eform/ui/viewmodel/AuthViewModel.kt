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

    private fun saveOrUpdateUserInLocalDb(userApiModel: UserApiModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingUser = userDao.getUserById(userApiModel.id) // Lebih baik cek by ID jika ID dari API stabil

                val userRoleSafe = userApiModel.role ?: "unknown" // Handle jika role dari API bisa null
                if (userApiModel.role == null) {
                    Log.w("AUTH_VM_DB", "UserApiModel.role is null for user: ${userApiModel.email}. Defaulting to '$userRoleSafe'.")
                }

                val userEntity = UserEntity(
                    id = userApiModel.id, // ID dari API harusnya jadi PK di lokal
                    name = userApiModel.name, // Jika name bisa null di API, tambahkan ?: "Nama Default"
                    email = userApiModel.email, // Jika email bisa null di API, tambahkan ?: "email@default.com"
                    nip = userApiModel.nip ?: "" , // nip sudah nullable di UserApiModel dan UserEntity
                    password = existingUser?.password ?: "", // Jaga password lama, atau kosongkan jika register baru
                    role = userRoleSafe,
                    address = userApiModel.address
                    // Anda bisa tambahkan subject dan grade di UserEntity jika mau disimpan
                    // subject = if (userRoleSafe == "teacher") userApiModel.subject else null,
                    // grade = if (userRoleSafe == "student") userApiModel.grade else null
                )

                if (existingUser != null) {
                    // Pastikan field password tidak di-overwrite jika tidak ada perubahan password
                    val finalEntity = if (userEntity.password.isBlank() && existingUser.password.isNotBlank()) {
                        userEntity.copy(password = existingUser.password)
                    } else {
                        userEntity
                    }
                    userDao.updateUser(finalEntity)
                    Log.d("AUTH_VM_DB", "User updated in local DB: $finalEntity")
                } else {
                    userDao.insertUser(userEntity)
                    Log.d("AUTH_VM_DB", "New user inserted into local DB: $userEntity")
                }
            } catch (e: Exception) {
                Log.e("AUTH_VM_DB", "Error saving/updating user in local DB: ${e.message}", e)
            }
        }
    }

    fun login(loginRequest: LoginRequest) {
        viewModelScope.launch {
            _loginResult.value = AuthResult.Loading
            val result = authRepository.login(loginRequest)
            result.fold(
                onSuccess = { authResponse ->
                    saveOrUpdateUserInLocalDb(authResponse.user)
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
                    saveOrUpdateUserInLocalDb(authResponse.user)
                    _registerResult.value = AuthResult.Success(authResponse)
                },
                onFailure = { exception -> _registerResult.value = AuthResult.Error(exception.message ?: "Registrasi gagal") }
            )
        }
    }

    fun logout() {
        // Jalankan seluruh proses logout di background thread (I/O)
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout()
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
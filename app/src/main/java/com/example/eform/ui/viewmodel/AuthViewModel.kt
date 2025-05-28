package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.local.UserPreferences
import com.example.eform.data.model.LoginRequest
import com.example.eform.data.model.RegisterRequest
import com.example.eform.data.model.api.AuthResponse
import com.example.eform.data.repository.AuthRepository
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

    private val _loginResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val loginResult: StateFlow<AuthResult> = _loginResult.asStateFlow()

    private val _registerResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val registerResult: StateFlow<AuthResult> = _registerResult.asStateFlow()

    private val _logoutResult = MutableStateFlow<Result<Unit>?>(null)
    val logoutResult: StateFlow<Result<Unit>?> = _logoutResult.asStateFlow()

    fun login(loginRequest: LoginRequest) {
        viewModelScope.launch {
            _loginResult.value = AuthResult.Loading
            val result = authRepository.login(loginRequest)
            result.fold(
                onSuccess = { authResponse -> _loginResult.value = AuthResult.Success(authResponse) },
                onFailure = { exception -> _loginResult.value = AuthResult.Error(exception.message ?: "Login gagal") }
            )
        }
    }

    fun register(registerRequest: RegisterRequest) {
        viewModelScope.launch {
            _registerResult.value = AuthResult.Loading
            // AuthRepository.register sekarang menghandle role untuk memanggil endpoint yang benar
            val result = authRepository.register(registerRequest)
            result.fold(
                onSuccess = { authResponse -> _registerResult.value = AuthResult.Success(authResponse) },
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
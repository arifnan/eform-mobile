package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.model.api.UserApiModel
import com.example.eform.data.repository.AuthRepository
import com.example.eform.data.repository.FormRepository
import com.example.eform.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StudentDashboardUiState {
    object Idle : StudentDashboardUiState()
    object Loading : StudentDashboardUiState()
    data class Success(
        val student: UserApiModel,
        val availableForms: List<FormApiModel> // Contoh jika siswa bisa lihat daftar form publik
    ) : StudentDashboardUiState()
    data class Error(val message: String) : StudentDashboardUiState()
}

sealed class FormCodeValidationResult {
    object Idle : FormCodeValidationResult()
    object Loading : FormCodeValidationResult()
    data class Valid(val form: FormApiModel) : FormCodeValidationResult()
    data class Invalid(val message: String) : FormCodeValidationResult()
    // data class Error(val message: String) : FormCodeValidationResult() // Ganti nama agar tidak ambigu
}


class DashboardStudentsViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val authRepository = AuthRepository(RetrofitInstance.api, userPreferences)
    private val formRepository = FormRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow<StudentDashboardUiState>(StudentDashboardUiState.Idle)
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    private val _formCodeValidationResult = MutableStateFlow<FormCodeValidationResult>(FormCodeValidationResult.Idle)
    val formCodeValidationResult: StateFlow<FormCodeValidationResult> = _formCodeValidationResult.asStateFlow()

    fun loadStudentDashboard(studentUserIdentifier: String) {
        viewModelScope.launch {
            _uiState.value = StudentDashboardUiState.Loading
            // Asumsi /api/user mengembalikan user yang login (siswa)
            val userResult = authRepository.getAuthenticatedUser()

            userResult.fold(
                onSuccess = { studentApiModel ->
                    if (studentApiModel.role.equals("student", ignoreCase = true)) {
                        // TODO: Ambil daftar formulir yang relevan untuk siswa ini dari API.
                        // Misal: formRepository.getPublicForms() atau getAssignedForms(studentApiModel.id)
                        // Untuk saat ini, kirim list kosong.
                        _uiState.value = StudentDashboardUiState.Success(studentApiModel, emptyList())
                    } else {
                        _uiState.value = StudentDashboardUiState.Error("Pengguna yang login bukan siswa.")
                    }
                },
                onFailure = { exception ->
                    _uiState.value = StudentDashboardUiState.Error(exception.message ?: "Gagal memuat data siswa.")
                }
            )
        }
    }

    fun validateFormCode(formCode: String) {
        viewModelScope.launch {
            _formCodeValidationResult.value = FormCodeValidationResult.Loading
            if (formCode.isBlank()) {
                _formCodeValidationResult.value = FormCodeValidationResult.Invalid("Kode formulir tidak boleh kosong.")
                return@launch
            }

            val result = formRepository.getFormByCode(formCode)
            result.fold(
                onSuccess = { formApiModel ->
                    _formCodeValidationResult.value = FormCodeValidationResult.Valid(formApiModel)
                },
                onFailure = { exception ->
                    _formCodeValidationResult.value = FormCodeValidationResult.Invalid(exception.message ?: "Kode formulir tidak valid atau tidak ditemukan.")
                }
            )
        }
    }

    fun resetFormCodeValidation() {
        _formCodeValidationResult.value = FormCodeValidationResult.Idle
    }

    class DashboardStudentsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardStudentsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardStudentsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
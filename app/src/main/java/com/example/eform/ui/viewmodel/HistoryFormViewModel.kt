package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.model.api.FormResponseApiModel
import com.example.eform.data.model.api.UserApiModel
import com.example.eform.data.repository.AuthRepository
import com.example.eform.data.repository.FormRepository
import com.example.eform.data.local.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HistoryUiState {
    object Idle : HistoryUiState()
    object Loading : HistoryUiState()
    data class SuccessForms(val forms: List<FormApiModel>) : HistoryUiState() // Untuk riwayat formulir yang dibuat guru
    data class SuccessResponses(val responses: List<FormResponseApiModel>) : HistoryUiState() // Untuk riwayat jawaban siswa
    data class Error(val message: String) : HistoryUiState()
    object Empty : HistoryUiState()
}

class HistoryFormViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val authRepository = AuthRepository(RetrofitInstance.api, userPreferences)
    private val formRepository = FormRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Idle)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // userIdentifier tidak lagi di-pass ke fungsi ini, karena kita akan ambil dari authRepository
    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading

            val userResult = authRepository.getAuthenticatedUser() // Dapatkan user yang sedang login
            userResult.fold(
                onSuccess = { user ->
                    if (user.role.equals("teacher", ignoreCase = true)) {
                        // Guru - Ambil riwayat formulir yang telah dibuat (menggunakan endpoint yang sama dengan dashboard guru)
                        val formsHistoryResult = formRepository.getTeacherFormsHistory() // Ini memanggil getTeacherForms()
                        formsHistoryResult.fold(
                            onSuccess = { forms ->
                                if (forms.isEmpty()) _uiState.value = HistoryUiState.Empty
                                else _uiState.value = HistoryUiState.SuccessForms(forms)
                            },
                            onFailure = { _uiState.value = HistoryUiState.Error(it.message ?: "Gagal memuat riwayat formulir guru") }
                        )
                    } else if (user.role.equals("student", ignoreCase = true)) {
                        // Siswa - Ambil riwayat formulir yang telah diisi
                        val responsesHistoryResult = formRepository.getStudentResponsesHistory()
                        responsesHistoryResult.fold(
                            onSuccess = { responses ->
                                if (responses.isEmpty()) _uiState.value = HistoryUiState.Empty
                                else _uiState.value = HistoryUiState.SuccessResponses(responses)
                            },
                            onFailure = { _uiState.value = HistoryUiState.Error(it.message ?: "Gagal memuat riwayat pengisian formulir") }
                        )
                    } else {
                        _uiState.value = HistoryUiState.Error("Peran pengguna tidak dikenal untuk memuat riwayat.")
                    }
                },
                onFailure = { exception ->
                    _uiState.value = HistoryUiState.Error(exception.message ?: "Gagal mendapatkan data pengguna untuk riwayat.")
                }
            )
        }
    }

    class HistoryFormViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryFormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HistoryFormViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
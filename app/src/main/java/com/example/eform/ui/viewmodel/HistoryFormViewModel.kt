package com.example.eform.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.model.api.FormResponseApiModel
// import com.example.eform.data.model.api.UserApiModel // Tidak digunakan secara langsung di sini
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
    data class SuccessForms(val forms: List<FormApiModel>) : HistoryUiState()
    data class SuccessResponses(val responses: List<FormResponseApiModel>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
    object Empty : HistoryUiState()
}

class HistoryFormViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val authRepository = AuthRepository(RetrofitInstance.api, userPreferences)
    private val formRepository = FormRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Idle)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            Log.d("HistoryVM", "loadHistory called")

            val userResult = authRepository.getAuthenticatedUser()
            userResult.fold(
                onSuccess = { user ->
                    Log.d("HistoryVM", "Authenticated user for history: ${user.email}, Role: ${user.role}")
                    if (user.role.equals("teacher", ignoreCase = true)) {
                        val formsHistoryResult = formRepository.getTeacherFormsHistory()
                        formsHistoryResult.fold(
                            onSuccess = { forms ->
                                Log.d("HistoryVM", "Teacher forms history success, count: ${forms.size}")
                                if (forms.isEmpty()) _uiState.value = HistoryUiState.Empty
                                else _uiState.value = HistoryUiState.SuccessForms(forms)
                            },
                            onFailure = { error ->
                                Log.e("HistoryVM", "Failed to load teacher forms history: ${error.message}")
                                _uiState.value = HistoryUiState.Error(error.message ?: "Gagal memuat riwayat formulir guru")
                            }
                        )
                    } else if (user.role.equals("student", ignoreCase = true)) {
                        val responsesHistoryResult = formRepository.getStudentResponsesHistory()
                        responsesHistoryResult.fold(
                            onSuccess = { responses ->
                                Log.d("HistoryVM", "Student responses history success, count: ${responses.size}")
                                if (responses.isEmpty()) _uiState.value = HistoryUiState.Empty
                                else _uiState.value = HistoryUiState.SuccessResponses(responses)
                            },
                            onFailure = { error ->
                                Log.e("HistoryVM", "Failed to load student responses history: ${error.message}")
                                _uiState.value = HistoryUiState.Error(error.message ?: "Gagal memuat riwayat pengisian formulir")
                            }
                        )
                    } else {
                        Log.w("HistoryVM", "Unknown user role for history: ${user.role}")
                        _uiState.value = HistoryUiState.Error("Peran pengguna tidak dikenal untuk memuat riwayat.")
                    }
                },
                onFailure = { exception ->
                    Log.e("HistoryVM", "Failed to get authenticated user for history: ${exception.message}")
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

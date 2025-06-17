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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Data class untuk UI, menggabungkan respons dan status favorit
data class StudentResponseDisplayItem(
    val response: FormResponseApiModel,
    var isFavorite: Boolean // Jadikan var agar bisa diubah
)

// Sealed class untuk UI State, sekarang membawa data yang tepat
sealed class StudentDashboardUiState {
    object Idle : StudentDashboardUiState()
    object Loading : StudentDashboardUiState()
    data class Success(
        val student: UserApiModel,
        val recentResponses: List<StudentResponseDisplayItem>
    ) : StudentDashboardUiState()
    data class Error(val message: String) : StudentDashboardUiState()
}

sealed class FormCodeValidationResult {
    object Idle : FormCodeValidationResult()
    object Loading : FormCodeValidationResult()
    data class Valid(val form: FormApiModel) : FormCodeValidationResult()
    data class Invalid(val message: String) : FormCodeValidationResult()
}


class DashboardStudentsViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(RetrofitInstance.api, UserPreferences(application))
    private val formRepository = FormRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow<StudentDashboardUiState>(StudentDashboardUiState.Idle)
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    private val _formCodeValidationResult = MutableStateFlow<FormCodeValidationResult>(FormCodeValidationResult.Idle)
    val formCodeValidationResult: StateFlow<FormCodeValidationResult> = _formCodeValidationResult.asStateFlow()

    fun loadStudentDashboardData() {
        viewModelScope.launch {
            // Langsung set ke Loading, UI akan menampilkan CircularProgressIndicator
            _uiState.value = StudentDashboardUiState.Loading

            // 1. Ambil data user yang sedang login dari server
            val userResult = authRepository.getAuthenticatedUser()

            val user = userResult.getOrNull()
            if (user == null || !user.role.equals("student", ignoreCase = true)) {
                _uiState.value = StudentDashboardUiState.Error("Gagal memuat data siswa atau peran tidak valid.")
                return@launch
            }

            // 2. Ambil riwayat dan favorit secara bersamaan untuk efisiensi
            val responsesHistoryDeferred = async(Dispatchers.IO) { formRepository.getStudentResponsesHistory() }
            val favoriteFormsDeferred = async(Dispatchers.IO) { formRepository.getFavoriteForms() }

            val responsesHistoryResult = responsesHistoryDeferred.await()
            val favoriteFormsResult = favoriteFormsDeferred.await()

            // Cek jika ada yang gagal, cukup satu yang gagal untuk menampilkan error
            if (responsesHistoryResult.isFailure || favoriteFormsResult.isFailure) {
                val errorMsg = responsesHistoryResult.exceptionOrNull()?.message
                    ?: favoriteFormsResult.exceptionOrNull()?.message
                    ?: "Terjadi kesalahan tidak diketahui."
                _uiState.value = StudentDashboardUiState.Error(errorMsg)
                return@launch
            }

            // Jika keduanya sukses (meskipun datanya kosong)
            val responses = responsesHistoryResult.getOrThrow()
            val favoriteForms = favoriteFormsResult.getOrThrow()
            val favoriteFormIds = favoriteForms.map { it.id }.toSet()

            // 3. Gabungkan data menjadi satu list untuk ditampilkan
            val displayList = responses.map { response ->
                StudentResponseDisplayItem(
                    response = response,
                    isFavorite = favoriteFormIds.contains(response.form?.id)
                )
            }

            // 4. Kirim state Success dengan semua data yang sudah siap
            _uiState.value = StudentDashboardUiState.Success(user, displayList)
        }
    }

    fun toggleFavoriteStatus(formId: Int, newStatus: Boolean) {
        viewModelScope.launch {
            val result = if (newStatus) formRepository.addFavorite(formId) else formRepository.removeFavorite(formId)

            if (result.isSuccess) {
                val currentState = _uiState.value
                if (currentState is StudentDashboardUiState.Success) {
                    val updatedList = currentState.recentResponses.map { item ->
                        if (item.response.form?.id == formId) {
                            item.copy(isFavorite = newStatus)
                        } else {
                            item
                        }
                    }
                    _uiState.value = currentState.copy(recentResponses = updatedList)
                }
            }
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
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
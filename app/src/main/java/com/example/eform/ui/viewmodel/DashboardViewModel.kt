package com.example.eform.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.model.api.UserApiModel // <-- Pastikan ini diimpor
import com.example.eform.data.repository.AuthRepository
import com.example.eform.data.repository.FormRepository
import com.example.eform.data.local.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FormDisplayItem(
    val formApiData: FormApiModel,
    var isUserFavorite: Boolean
)

sealed class DashboardFormsResult {
    object Loading : DashboardFormsResult()
    // ========== PERUBAHAN DI SINI ==========
    // Sekarang state Success juga membawa data 'user'
    data class Success(val user: UserApiModel, val forms: List<FormDisplayItem>) : DashboardFormsResult()
    data class Error(val message: String) : DashboardFormsResult()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)
    private val authRepository = AuthRepository(RetrofitInstance.api, UserPreferences(application)) // Tambahkan AuthRepository

    private val _formsResult = MutableStateFlow<DashboardFormsResult>(DashboardFormsResult.Loading)
    val formsResult: StateFlow<DashboardFormsResult> = _formsResult.asStateFlow()

    // State userName tidak lagi diperlukan karena data user sudah lengkap di 'Success'
    // private val _userName = MutableStateFlow("Guru")
    // val userName: StateFlow<String> = _userName.asStateFlow()

    fun loadTeacherDashboard(userIdentifier: String, sortOrder: String) {
        viewModelScope.launch {
            _formsResult.value = DashboardFormsResult.Loading

            // Ambil data user terlebih dahulu dari AuthRepository
            val userResult = authRepository.getAuthenticatedUser()
            val user = userResult.getOrNull()
            if (user == null || !user.role.equals("teacher", ignoreCase = true)) {
                _formsResult.value = DashboardFormsResult.Error("Gagal memuat data guru.")
                return@launch
            }

            val allFormsDeferred = async(Dispatchers.IO) { formRepository.getTeacherForms() }
            val favoriteFormsDeferred = async(Dispatchers.IO) { formRepository.getFavoriteForms() }

            val allFormsResult = allFormsDeferred.await()
            val favoriteFormsResult = favoriteFormsDeferred.await()

            if (allFormsResult.isFailure || favoriteFormsResult.isFailure) {
                val errorMessage = allFormsResult.exceptionOrNull()?.message
                    ?: favoriteFormsResult.exceptionOrNull()?.message
                    ?: "Gagal memuat data dashboard."
                _formsResult.value = DashboardFormsResult.Error(errorMessage)
                return@launch
            }

            val apiForms = allFormsResult.getOrNull() ?: emptyList()
            val favoriteForms = favoriteFormsResult.getOrNull() ?: emptyList()
            val favoriteFormIds = favoriteForms.map { it.id }.toSet()

            var displayList = apiForms.map { apiForm ->
                FormDisplayItem(
                    formApiData = apiForm,
                    isUserFavorite = favoriteFormIds.contains(apiForm.id)
                )
            }

            displayList = if (sortOrder == "asc") {
                displayList.sortedBy { it.formApiData.title.lowercase() }
            } else {
                displayList.sortedByDescending { it.formApiData.title.lowercase() }
            }

            // ========== PERUBAHAN DI SINI ==========
            // Kirimkan object 'user' lengkap ke dalam state Success
            _formsResult.value = DashboardFormsResult.Success(user, displayList)
        }
    }

    fun toggleFavoriteStatus(formId: Int, newStatus: Boolean) {
        viewModelScope.launch {
            val result = if (newStatus) {
                formRepository.addFavorite(formId)
            } else {
                formRepository.removeFavorite(formId)
            }

            result.fold(
                onSuccess = {
                    val currentUiState = _formsResult.value
                    if (currentUiState is DashboardFormsResult.Success) {
                        val updatedList = currentUiState.forms.map {
                            if (it.formApiData.id == formId) {
                                it.copy(isUserFavorite = newStatus)
                            } else {
                                it
                            }
                        }
                        // Kirim kembali state dengan user yang sama dan list yang sudah diupdate
                        _formsResult.value = currentUiState.copy(forms = updatedList)
                    }
                },
                onFailure = { error ->
                    Log.e("DashboardVM", "Toggle favorite failed: ${error.message}")
                }
            )
        }
    }

    class DashboardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
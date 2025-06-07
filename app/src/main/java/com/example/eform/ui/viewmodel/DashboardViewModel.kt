package com.example.eform.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.repository.FormRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async // <-- Import async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class dan Sealed class di sini tetap sama
data class FormDisplayItem(
    val formApiData: FormApiModel,
    var isUserFavorite: Boolean
)

sealed class DashboardFormsResult {
    object Loading : DashboardFormsResult()
    data class Success(val forms: List<FormDisplayItem>) : DashboardFormsResult()
    data class Error(val message: String) : DashboardFormsResult()
}


class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)
    private val userDao = AppDatabase.getDatabase(application).userDao()
    // Kita tidak lagi memerlukan DAO favorit dan form di ViewModel ini
    // private val formDao = AppDatabase.getDatabase(application).formDao()
    // private val userFavoriteFormDao = AppDatabase.getDatabase(application).userFavoriteFormDao()

    private val _formsResult = MutableStateFlow<DashboardFormsResult>(DashboardFormsResult.Loading)
    val formsResult: StateFlow<DashboardFormsResult> = _formsResult.asStateFlow()

    private val _userName = MutableStateFlow("Guru")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private var currentSortOrder: String = "asc"

    fun loadTeacherDashboard(userIdentifier: String, sortOrder: String) {
        // Logika untuk mencegah reload yang tidak perlu bisa dipertahankan
        this.currentSortOrder = sortOrder

        viewModelScope.launch {
            _formsResult.value = DashboardFormsResult.Loading

            // Ambil data user terlebih dahulu
            val user = withContext(Dispatchers.IO) { userDao.getUserByNip(userIdentifier) }
            if (user == null) {
                _formsResult.value = DashboardFormsResult.Error("Data guru tidak ditemukan.")
                return@launch
            }
            _userName.value = user.name

            // Jalankan dua panggilan API secara bersamaan untuk efisiensi
            val allFormsDeferred = async(Dispatchers.IO) { formRepository.getTeacherForms() }
            val favoriteFormsDeferred = async(Dispatchers.IO) { formRepository.getFavoriteForms() }

            // Tunggu kedua hasil panggilan API
            val allFormsResult = allFormsDeferred.await()
            val favoriteFormsResult = favoriteFormsDeferred.await()

            // Cek apakah ada error dari salah satu panggilan API
            if (allFormsResult.isFailure || favoriteFormsResult.isFailure) {
                val errorMessage = allFormsResult.exceptionOrNull()?.message
                    ?: favoriteFormsResult.exceptionOrNull()?.message
                    ?: "Gagal memuat data dashboard."
                _formsResult.value = DashboardFormsResult.Error(errorMessage)
                return@launch
            }

            val apiForms = allFormsResult.getOrNull() ?: emptyList()
            val favoriteForms = favoriteFormsResult.getOrNull() ?: emptyList()

            // Buat sebuah Set dari ID formulir yang difavoritkan untuk pencarian cepat
            val favoriteFormIds = favoriteForms.map { it.id }.toSet()

            // Gabungkan data: tandai formulir sebagai favorit jika ID-nya ada di dalam Set
            var displayList = apiForms.map { apiForm ->
                FormDisplayItem(
                    formApiData = apiForm,
                    isUserFavorite = favoriteFormIds.contains(apiForm.id) // <-- Logika penandaan favorit
                )
            }

            // Lakukan sorting
            displayList = if (sortOrder == "asc") {
                displayList.sortedBy { it.formApiData.title.lowercase() }
            } else {
                displayList.sortedByDescending { it.formApiData.title.lowercase() }
            }

            _formsResult.value = DashboardFormsResult.Success(displayList)
        }
    }

    // Fungsi toggleFavoriteStatus sekarang sudah benar karena hanya memanggil API
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
                        _formsResult.value = DashboardFormsResult.Success(updatedList)
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
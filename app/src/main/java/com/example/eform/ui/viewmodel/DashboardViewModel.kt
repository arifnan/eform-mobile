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
import com.example.eform.data.model.UserFavoriteFormEntity
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.repository.FormRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class untuk item yang ditampilkan di UI Dashboard Guru
data class FormDisplayItem(
    val formApiData: FormApiModel,
    var isUserFavorite: Boolean
)

// Sealed class untuk merepresentasikan state hasil pengambilan formulir
sealed class DashboardFormsResult {
    object Loading : DashboardFormsResult()
    data class Success(val forms: List<FormDisplayItem>) : DashboardFormsResult()
    data class Error(val message: String) : DashboardFormsResult()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // Inisialisasi repository dan DAO
    // RetrofitInstance.api diambil dari object yang sudah diinisialisasi
    private val formRepository = FormRepository(RetrofitInstance.api)
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val userFavoriteFormDao = AppDatabase.getDatabase(application).userFavoriteFormDao()

    // StateFlow untuk hasil pengambilan formulir
    private val _formsResult = MutableStateFlow<DashboardFormsResult>(DashboardFormsResult.Loading)
    val formsResult: StateFlow<DashboardFormsResult> = _formsResult

    // StateFlow untuk nama pengguna
    private val _userName = MutableStateFlow("Guru") // Nama default
    val userName: StateFlow<String> = _userName

    private var currentUserId: Int? = null
    private var currentUserIdentifier: String? = null
    private var currentSortOrder: String = "asc" // Default sort order
    private val formDao = AppDatabase.getDatabase(application).formDao()

    // Fungsi untuk memuat data dashboard guru

    fun loadTeacherDashboard(userIdentifier: String, sortOrder: String) {
        if (this.currentUserIdentifier == userIdentifier &&
            this.currentSortOrder == sortOrder &&
            _formsResult.value is DashboardFormsResult.Success) {
            return
        }
        this.currentUserIdentifier = userIdentifier
        this.currentSortOrder = sortOrder

        viewModelScope.launch {
            _formsResult.value = DashboardFormsResult.Loading

            // 1. Dapatkan info user
            val userEntity = withContext(Dispatchers.IO) { userDao.getUserByNip(userIdentifier) }
            if (userEntity == null) {
                _formsResult.value = DashboardFormsResult.Error("Data guru tidak ditemukan untuk NIP: $userIdentifier")
                return@launch
            }
            currentUserId = userEntity.id
            _userName.value = userEntity.name

            // 2. Ambil semua formulir dari API
            val formsApiResult = formRepository.getTeacherForms()

            formsApiResult.fold(
                onSuccess = { apiForms ->
                    // 3. SINKRONISASI KE DATABASE LOKAL (ROOM)
                    withContext(Dispatchers.IO) {
                        val formEntities = apiForms.map { apiForm ->
                            FormEntity(
                                id = apiForm.id,
                                title = apiForm.title,
                                description = apiForm.description ?: "",
                                createdAt = System.currentTimeMillis(), // Atau parse dari apiForm.createdAt jika perlu
                                formCode = apiForm.formCode
                            )
                        }
                        // Menggunakan insertForm yang akan me-replace jika sudah ada (OnConflictStrategy.REPLACE)
                        formDao.insertForm(formEntities)
                        Log.d("DashboardVM", "${formEntities.size} form entities saved to local DB.")
                    }

                    // 4. Ambil daftar ID favorit lokal setelah sinkronisasi
                    val favoriteFormIds = currentUserId?.let { userId ->
                        withContext(Dispatchers.IO) {
                            userFavoriteFormDao.getFavoriteFormIdsByUserId(userId).toSet()
                        }
                    } ?: emptySet()

                    // 5. Gabungkan data dan lakukan sorting
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
                    _formsResult.value = DashboardFormsResult.Success(displayList)
                },
                onFailure = { exception ->
                    _formsResult.value = DashboardFormsResult.Error(exception.message ?: "Gagal memuat formulir dari API")
                }
            )
        }
    }

    // Fungsi toggleFavoriteStatus sudah benar, karena sekarang data form sudah ada di lokal
    fun toggleFavoriteStatus(formId: Int, newStatus: Boolean) {
        val userId = currentUserId ?: return

        viewModelScope.launch(Dispatchers.IO) {
            if (newStatus) {
                userFavoriteFormDao.addFavorite(UserFavoriteFormEntity(userId, formId))
            } else {
                userFavoriteFormDao.removeFavorite(UserFavoriteFormEntity(userId, formId))
            }

            // Update UI
            val currentUiState = _formsResult.value
            if (currentUiState is DashboardFormsResult.Success) {
                val updatedList = currentUiState.forms.map {
                    if (it.formApiData.id == formId) {
                        it.copy(isUserFavorite = newStatus)
                    } else {
                        it
                    }
                }
                withContext(Dispatchers.Main) {
                    _formsResult.value = DashboardFormsResult.Success(updatedList)
                }
            }
        }
    }

    // ViewModelProvider.Factory untuk DashboardViewModel
    class DashboardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
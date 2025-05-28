package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance // Diperlukan untuk FormRepository
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.UserFavoriteFormEntity
import com.example.eform.data.model.api.FormApiModel // ViewModel akan menghasilkan ini
import com.example.eform.data.repository.FormRepository // Diperlukan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteFormsViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val userFavoriteFormDao = AppDatabase.getDatabase(application).userFavoriteFormDao()
    private val formRepository = FormRepository(RetrofitInstance.api) // Untuk mengambil detail FormApiModel

    private val _favoriteFormsApi = MutableStateFlow<List<FormApiModel>>(emptyList())
    val favoriteFormsApi: StateFlow<List<FormApiModel>> = _favoriteFormsApi.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentUserId: Int? = null

    fun loadFavoriteForms(userIdentifier: String) {
        viewModelScope.launch {
            _isLoading.value = true
            currentUserId = withContext(Dispatchers.IO) {
                userDao.getUserByNip(userIdentifier)?.id ?: userDao.getUserByEmail(userIdentifier)?.id
            }

            currentUserId?.let { userId ->
                val favoriteFormIds = withContext(Dispatchers.IO) {
                    userFavoriteFormDao.getFavoriteFormIdsByUserId(userId)
                }

                if (favoriteFormIds.isNotEmpty()) {
                    // Ambil detail setiap FormApiModel dari API secara paralel
                    val deferredFormApiModels = favoriteFormIds.map { formId ->
                        async(Dispatchers.IO) {
                            formRepository.getFormDetails(formId).getOrNull() // Ambil hasilnya atau null jika error
                        }
                    }
                    // Tunggu semua panggilan API selesai dan filter yang tidak null
                    val formsFromApi = deferredFormApiModels.awaitAll().filterNotNull()
                    _favoriteFormsApi.value = formsFromApi
                } else {
                    _favoriteFormsApi.value = emptyList()
                }
            } ?: run {
                _favoriteFormsApi.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun toggleFavoriteStatus(formId: Int, newStatus: Boolean) { // Ini akan menghapus dari favorit
        val userIdForToggle = currentUserId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (!newStatus) { // Hanya hapus jika newStatus adalah false
                userFavoriteFormDao.removeFavorite(UserFavoriteFormEntity(userIdForToggle, formId))
            }
            // Muat ulang daftar favorit setelah status diubah
            // Ini akan memicu pemanggilan API lagi untuk form yang tersisa
            currentUserIdentifier?.let { loadFavoriteForms(it) } // Perlu cara mendapatkan userIdentifier lagi
        }
    }
    // Simpan userIdentifier saat loadFavoriteForms dipanggil pertama kali
    private var currentUserIdentifier: String? = null
    fun loadFavoriteFormsAndSetIdentifier(userIdentifier: String) {
        this.currentUserIdentifier = userIdentifier
        loadFavoriteForms(userIdentifier)
    }


    class FavoriteFormsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FavoriteFormsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FavoriteFormsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
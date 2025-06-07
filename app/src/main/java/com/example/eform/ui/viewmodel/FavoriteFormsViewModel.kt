package com.example.eform.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.repository.FormRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FavoriteFormsViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)

    private val _favoriteFormsApi = MutableStateFlow<List<FormApiModel>>(emptyList())
    val favoriteFormsApi: StateFlow<List<FormApiModel>> = _favoriteFormsApi

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Diubah menjadi load dari API
    fun loadFavoriteForms() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = formRepository.getFavoriteForms()
            result.fold(
                onSuccess = { forms ->
                    _favoriteFormsApi.value = forms
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Gagal memuat formulir favorit."
                }
            )
            _isLoading.value = false
        }
    }

    // Fungsi untuk menghapus favorit dari halaman ini
    fun removeFavorite(formId: Int) {
        viewModelScope.launch {
            val result = formRepository.removeFavorite(formId)
            if (result.isSuccess) {
                // Hapus item dari list lokal untuk update UI instan
                val updatedList = _favoriteFormsApi.value.filter { it.id != formId }
                _favoriteFormsApi.value = updatedList
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Gagal menghapus favorit."
            }
        }
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
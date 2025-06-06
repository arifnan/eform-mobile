package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.model.api.FormResponseApiModel
import com.example.eform.data.repository.FormRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ResponseDetailUiState {
    object Loading : ResponseDetailUiState()
    data class Success(val response: FormResponseApiModel) : ResponseDetailUiState()
    data class Error(val message: String) : ResponseDetailUiState()
}

class FormResponseDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow<ResponseDetailUiState>(ResponseDetailUiState.Loading)
    val uiState: StateFlow<ResponseDetailUiState> = _uiState.asStateFlow()

    fun loadResponseDetail(responseId: Int) {
        viewModelScope.launch {
            _uiState.value = ResponseDetailUiState.Loading
            // Anda perlu fungsi di repository untuk mengambil detail response
            // Mari kita asumsikan namanya getResponseDetail(responseId)
             val result = formRepository.getResponseDetail(responseId)
             result.fold(
                 onSuccess = { responseData ->
                     _uiState.value = ResponseDetailUiState.Success(responseData)
                 },
                 onFailure = { error ->
                     _uiState.value = ResponseDetailUiState.Error(error.message ?: "Gagal memuat detail.")
                 }
             )
        }
    }

    // Factory untuk ViewModel
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FormResponseDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FormResponseDetailViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
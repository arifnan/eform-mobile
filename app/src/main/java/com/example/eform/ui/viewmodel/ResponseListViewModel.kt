package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.model.api.FormResponseApiModel
import com.example.eform.data.repository.FormRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ResponseListUiState {
    object Loading : ResponseListUiState()
    data class Success(val responses: List<FormResponseApiModel>) : ResponseListUiState()
    data class Error(val message: String) : ResponseListUiState()
    object Empty : ResponseListUiState()
}

class ResponseListViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow<ResponseListUiState>(ResponseListUiState.Loading)
    val uiState: StateFlow<ResponseListUiState> = _uiState

    fun loadResponses(formId: Int) {
        viewModelScope.launch {
            _uiState.value = ResponseListUiState.Loading
            val result = formRepository.getResponsesForForm(formId)
            result.fold(
                onSuccess = { responses ->
                    if (responses.isEmpty()) {
                        _uiState.value = ResponseListUiState.Empty
                    } else {
                        _uiState.value = ResponseListUiState.Success(responses)
                    }
                },
                onFailure = { error ->
                    _uiState.value = ResponseListUiState.Error(error.message ?: "Gagal memuat data.")
                }
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ResponseListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ResponseListViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
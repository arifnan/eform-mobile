package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.repository.FormRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// State untuk UI Preview
sealed class PreviewUiState {
    object Loading : PreviewUiState()
    data class Success(val form: FormApiModel) : PreviewUiState()
    data class Error(val message: String) : PreviewUiState()
}

class PreviewFormViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow<PreviewUiState>(PreviewUiState.Loading)
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    fun loadFormDetails(formId: Int) {
        if (formId == 0) {
            _uiState.value = PreviewUiState.Error("ID Formulir tidak valid.")
            return
        }

        viewModelScope.launch {
            _uiState.value = PreviewUiState.Loading
            // Menggunakan fungsi getFormDetails yang sudah ada di repository
            val result = formRepository.getFormDetails(formId)
            result.fold(
                onSuccess = { formApiModel ->
                    _uiState.value = PreviewUiState.Success(formApiModel)
                },
                onFailure = { error ->
                    _uiState.value = PreviewUiState.Error(error.message ?: "Gagal memuat detail formulir.")
                }
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PreviewFormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PreviewFormViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
// Path: com/example/eform/ui/viewmodel/DashboardStudentsViewModel.kt
package com.example.eform.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.database.AppDatabase // Pastikan AppDatabase diimpor jika userDao digunakan
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.model.api.UserApiModel
import com.example.eform.data.repository.AuthRepository
import com.example.eform.data.repository.FormRepository
import com.example.eform.data.local.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class StudentDashboardUiState {
    object Idle : StudentDashboardUiState()
    object Loading : StudentDashboardUiState()
    data class Success(
        val student: UserApiModel,
        val availableForms: List<FormApiModel> // Ini bisa jadi daftar form yang pernah diisi atau form publik
    ) : StudentDashboardUiState()
    data class Error(val message: String) : StudentDashboardUiState()
}

sealed class FormCodeValidationResult {
    object Idle : FormCodeValidationResult()
    object Loading : FormCodeValidationResult()
    data class Valid(val form: FormApiModel) : FormCodeValidationResult() // Mengembalikan FormApiModel
    data class Invalid(val message: String) : FormCodeValidationResult()
}


class DashboardStudentsViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val authRepository = AuthRepository(RetrofitInstance.api, userPreferences)
    private val formRepository = FormRepository(RetrofitInstance.api)
    private val userDao = AppDatabase.getDatabase(application).userDao() // Untuk mengambil nama siswa

    private val _uiState = MutableStateFlow<StudentDashboardUiState>(StudentDashboardUiState.Idle)
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    // StateFlow untuk hasil validasi kode
    private val _formCodeValidationResult = MutableStateFlow<FormCodeValidationResult>(FormCodeValidationResult.Idle)
    val formCodeValidationResult: StateFlow<FormCodeValidationResult> = _formCodeValidationResult.asStateFlow()

    private val _studentName = MutableStateFlow("Siswa")
    val studentName: StateFlow<String> = _studentName.asStateFlow()




    // Fungsi untuk memuat data awal dashboard siswa
    // userIdentifier bisa berupa email atau ID siswa, tergantung bagaimana Anda mengidentifikasinya
    fun loadStudentDashboardData(userIdentifier: String) {
        viewModelScope.launch {
            _uiState.value = StudentDashboardUiState.Loading

            // 1. Dapatkan detail siswa (termasuk nama) dari database lokal atau API
            // Di sini kita asumsikan userIdentifier adalah email untuk mengambil dari DB lokal
            // Jika menggunakan API untuk getAuthenticatedUser, itu lebih baik.
            val studentEntity = withContext(Dispatchers.IO) {
                userDao.getUserByEmail(userIdentifier) // Asumsi siswa login dengan email
            }

            if (studentEntity != null) {
                _studentName.value = studentEntity.name
                // Untuk UserApiModel, kita bisa buat instance dummy atau fetch dari API jika perlu data lengkap API
                val studentApiModel = UserApiModel(
                    id = studentEntity.id,
                    name = studentEntity.name,
                    email = studentEntity.email,
                    nip = studentEntity.nip, // Akan null atau "" untuk siswa
                    role = studentEntity.role,
                    emailVerifiedAt = null, // Tidak disimpan lokal biasanya
                    profilePhotoUrl = null, // Ambil dari API jika ada
                    address = studentEntity.address,
                    subject = null, // Tidak relevan untuk siswa
                    grade = null, // TODO: Ambil grade dari UserEntity jika sudah disimpan
                    createdAt = null, // Tidak disimpan lokal biasanya
                    updatedAt = null  // Tidak disimpan lokal biasanya
                )

                // 2. Ambil daftar formulir yang relevan untuk siswa (misalnya, riwayat atau form publik)
                // Untuk contoh, kita gunakan riwayat pengisian formulir siswa
                val responsesHistoryResult = formRepository.getStudentResponsesHistory()
                responsesHistoryResult.fold(
                    onSuccess = { responses ->
                        // Ekstrak FormApiModel dari responses jika ada
                        val formsFromHistory = responses.mapNotNull { it.form }
                        _uiState.value = StudentDashboardUiState.Success(studentApiModel, formsFromHistory)
                    },
                    onFailure = {
                        // Jika gagal ambil riwayat, tetap tampilkan info siswa dengan list form kosong
                        _uiState.value = StudentDashboardUiState.Success(studentApiModel, emptyList())
                        // Anda bisa juga menampilkan pesan error spesifik untuk riwayat
                    }
                )
            } else {
                _uiState.value = StudentDashboardUiState.Error("Gagal memuat data siswa.")
            }
        }
    }


    // Fungsi untuk memvalidasi kode formulir melalui API
    fun validateFormCode(formCode: String) {
        viewModelScope.launch {
            _formCodeValidationResult.value = FormCodeValidationResult.Loading
            if (formCode.isBlank()) {
                _formCodeValidationResult.value = FormCodeValidationResult.Invalid("Kode formulir tidak boleh kosong.")
                return@launch
            }

            val result = formRepository.getFormByCode(formCode) // Memanggil repository
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

    // Fungsi untuk me-reset state validasi setelah navigasi atau menampilkan pesan
    fun resetFormCodeValidation() {
        _formCodeValidationResult.value = FormCodeValidationResult.Idle
    }

    class DashboardStudentsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardStudentsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardStudentsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

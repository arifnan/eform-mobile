package com.example.eform.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.FormEntity // Digunakan di FormAnswerUiState
import com.example.eform.data.model.NotificationEntity
import com.example.eform.data.model.QuestionEntity // Digunakan di FormAnswerUiState
import com.example.eform.data.model.api.AnswerPayload
import com.example.eform.data.repository.FormRepository
import com.example.eform.utils.LocationHelper
// NotificationHelper tidak dipanggil dari ViewModel, tapi dari UI berdasarkan event
// import com.example.eform.utils.NotificationHelper
// import com.example.eform.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Sealed class untuk state UI FormAnswerScreen
sealed class FormAnswerUiState {
    object Idle : FormAnswerUiState()
    object LoadingQuestions : FormAnswerUiState()
    data class QuestionsLoaded(val form: FormEntity, val questions: List<QuestionEntity>) : FormAnswerUiState()
    object Submitting : FormAnswerUiState()
    // Tambahkan parameter untuk notifikasi sistem jika perlu
    data class SubmissionSuccess(val successMessage: String, val notificationTitle: String? = null, val notificationMessageForSytem: String? = null) : FormAnswerUiState()
    data class SubmissionError(val error: String) : FormAnswerUiState()
    data class LocationValidationResult(val isValid: Boolean, val message: String, val lat: Double?, val lng: Double?) : FormAnswerUiState()
}

class FormAnswerViewModel(application: Application) : AndroidViewModel(application) {

    // Inisialisasi repository, DAO, dan helper
    private val formRepository = FormRepository(RetrofitInstance.api)
    private val formDao = AppDatabase.getDatabase(application).formDao()
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val notificationDao = AppDatabase.getDatabase(application).notificationDao()
    private val locationHelper = LocationHelper(application) // Application context dari AndroidViewModel

    // StateFlow untuk UI state
    private val _uiState = MutableStateFlow<FormAnswerUiState>(FormAnswerUiState.Idle)
    val uiState: StateFlow<FormAnswerUiState> = _uiState

    private var currentStudentId: Int? = null
    // private lateinit var currentStudentIdentifier: String // Tidak perlu disimpan jika hanya untuk init
    private var currentFormEntity: FormEntity? = null


    // Fungsi untuk memuat detail formulir dan pertanyaan
    fun loadFormDetails(formId: Int, studentUserIdentifier: String) {
        // this.currentStudentIdentifier = studentUserIdentifier // Simpan jika akan digunakan lagi
        viewModelScope.launch {
            _uiState.value = FormAnswerUiState.LoadingQuestions
            // 1. Dapatkan studentId dari email siswa
            currentStudentId = withContext(Dispatchers.IO) { userDao.getUserByEmail(studentUserIdentifier)?.id }
            if (currentStudentId == null) {
                _uiState.value = FormAnswerUiState.SubmissionError("Gagal mengidentifikasi data siswa.")
                return@launch
            }

            // 2. Ambil FormEntity dan QuestionEntity dari database lokal (Room)
            // Ini berdasarkan asumsi FormAnswerScreen menerima FormEntity dari AppNavHost,
            // yang berarti FormEntity sudah ada di Room.
            // Jika FormAnswerScreen hanya menerima formId dan harus fetch dari API, logikanya akan berbeda.
            val formEntityFromDb = withContext(Dispatchers.IO) { formDao.getFormWithQuestions(formId) }
            if (formEntityFromDb == null) {
                _uiState.value = FormAnswerUiState.SubmissionError("Formulir dengan ID $formId tidak ditemukan di database lokal.")
                return@launch
            }
            currentFormEntity = formEntityFromDb

            val questionsFromDb = withContext(Dispatchers.IO) { formDao.getQuestionsForForm(formId) }
            _uiState.value = FormAnswerUiState.QuestionsLoaded(formEntityFromDb, questionsFromDb)
        }
    }

    // Fungsi untuk memvalidasi lokasi
    fun validateLocation(targetLat: Double, targetLng: Double, radius: Double) {
        locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
            override fun onLocationResult(lat: Double, lng: Double) {
                val isValid = locationHelper.isWithinRadius(lat, lng, targetLat, targetLng, radius)
                val message = if (isValid) "✅ Lokasi valid ($lat, $lng)"
                else "❌ Di luar area yang diizinkan ($lat, $lng). Pengisian formulir akan ditolak jika dikirim."
                _uiState.value = FormAnswerUiState.LocationValidationResult(isValid, message, lat, lng)
            }
            override fun onError(message: String) {
                _uiState.value = FormAnswerUiState.LocationValidationResult(false, "❌ Gagal mendapatkan lokasi: $message", null, null)
            }
        })
    }

    // Fungsi untuk mengirimkan jawaban formulir
    fun submitAnswers(
        formId: Int, // ID formulir dari API
        answersMap: Map<Int, String>, // questionId (dari API) ke answerText
        photoFile: File?, // File foto yang sudah disiapkan
        latitude: Double?,
        longitude: Double?,
        isLocationPreviouslyValidatedAndCorrect: Boolean // Status validasi lokasi dari UI
    ) {
        val studentIdForSubmission = currentStudentId
        val formTitleForNotification = currentFormEntity?.title ?: "Formulir"

        if (studentIdForSubmission == null) {
            _uiState.value = FormAnswerUiState.SubmissionError("Siswa tidak teridentifikasi. Tidak bisa mengirim jawaban.")
            return
        }
        if (!isLocationPreviouslyValidatedAndCorrect) {
            _uiState.value = FormAnswerUiState.SubmissionError("Validasi lokasi gagal atau belum dilakukan dengan benar.")
            return
        }
        if (photoFile == null) {
            _uiState.value = FormAnswerUiState.SubmissionError("Foto bukti wajib disertakan.")
            return
        }

        viewModelScope.launch {
            _uiState.value = FormAnswerUiState.Submitting
            val answerPayloads = answersMap.map { (qId, ansText) -> AnswerPayload(questionId = qId, answerText = ansText) }

            // Panggil repository untuk mengirim jawaban ke API
            val result = formRepository.submitFormResponse(
                formId = formId,
                answers = answerPayloads,
                photoFile = photoFile,
                latitude = latitude,
                longitude = longitude
            )

            result.fold(
                onSuccess = { /* formResponseApiModel -> (tidak terpakai langsung) */
                    // Buat notifikasi lokal untuk siswa
                    val notificationMessage = "Anda berhasil mengisi formulir: $formTitleForNotification!"
                    val newNotification = NotificationEntity(
                        userId = studentIdForSubmission,
                        title = "Formulir Terkirim!",
                        message = notificationMessage
                    )
                    // Simpan notifikasi ke DB lokal
                    withContext(Dispatchers.IO) { notificationDao.insertNotification(newNotification).toInt() }

                    // Batasi jumlah notifikasi lokal
                    val currentNotifCount = withContext(Dispatchers.IO) { notificationDao.getNotificationCountForUser(studentIdForSubmission) }
                    if (currentNotifCount > 8) {
                        val oldest = withContext(Dispatchers.IO) {notificationDao.getOldestNotificationForUser(studentIdForSubmission)}
                        oldest?.let { withContext(Dispatchers.IO) {notificationDao.deleteNotificationById(it.id)} }
                    }
                    // Kirim event sukses ke UI, UI akan menangani pembuatan notifikasi sistem
                    _uiState.value = FormAnswerUiState.SubmissionSuccess(
                        successMessage = "Jawaban berhasil dikirim!",
                        notificationTitle = "Formulir Berhasil Diisi!",
                        notificationMessageForSytem = notificationMessage
                    )
                },
                onFailure = { exception ->
                    _uiState.value = FormAnswerUiState.SubmissionError(exception.message ?: "Gagal mengirim jawaban ke server")
                }
            )
        }
    }

    // Fungsi untuk mereset UI state ke Idle
    fun resetUiState() {
        _uiState.value = FormAnswerUiState.Idle
    }

    // ViewModelProvider.Factory untuk FormAnswerViewModel
    class FormAnswerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FormAnswerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FormAnswerViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
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
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.utils.LocationHelper
import com.google.android.gms.maps.model.LatLng
// NotificationHelper tidak dipanggil dari ViewModel, tapi dari UI berdasarkan event
// import com.example.eform.utils.NotificationHelper
// import com.example.eform.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File



data class SchoolLocation(
    val name: String,
    val coordinate: LatLng,
    val radius: Double = 150.0
)

// Sealed class untuk state UI FormAnswerScreen
sealed class FormAnswerUiState {
    object Idle : FormAnswerUiState()
    object LoadingQuestions : FormAnswerUiState()
    data class QuestionsLoaded(val form: FormEntity, val questions: List<QuestionEntity>) : FormAnswerUiState()
    object Submitting : FormAnswerUiState()
    // Tambahkan parameter untuk notifikasi sistem jika perlu
    data class SubmissionSuccess(val successMessage: String, val notificationTitle: String? = null, val notificationMessageForSytem: String? = null) : FormAnswerUiState()
    data class Error(val message: String) : FormAnswerUiState()
    data class LocationValidationResult(val isValid: Boolean, val message: String, val lat: Double?, val lng: Double?) : FormAnswerUiState()
}

class FormAnswerViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val notificationDao = AppDatabase.getDatabase(application).notificationDao()
    private val locationHelper = LocationHelper(application)

    private val _uiState = MutableStateFlow<FormAnswerUiState>(FormAnswerUiState.Idle)
    val uiState: StateFlow<FormAnswerUiState> = _uiState

    private var currentStudentId: Int? = null
    private var currentFormEntity: FormEntity? = null

    // ===============================================
    // === PERUBAHAN UTAMA ADA DI SINI ===
    // ===============================================

    // Daftar lokasi sekolah yang diizinkan (hardcoded)
    private val allowedSchoolLocations = listOf(
        SchoolLocation(name = "SMA Negeri 7 Lhokseumawe", coordinate = LatLng(5.22308, 97.05332), radius = 300.0), // Koordinat dari file Anda
        SchoolLocation(name = "SMA Negeri 5 Lhokseumawe", coordinate = LatLng(5.173443, 97.123116), radius = 300.0), // Koordinat perkiraan
        SchoolLocation(name = "SMA Negeri 6 Lhokseumawe", coordinate = LatLng(5.11940, 97.17401), radius = 300.0),  // Koordinat perkiraan
        SchoolLocation(name = "Lokasi Testing Rumah", coordinate = LatLng(3.579741, 98.621874), radius = 300.0),
        SchoolLocation(name = "Lokasi Testing Kost", coordinate = LatLng(5.211149, 97.08079), radius = 300.0)
    )

    // Fungsi untuk memvalidasi lokasi (TIDAK PERLU PARAMETER LAGI)
    fun validateLocation() {
        locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
            override fun onLocationResult(lat: Double, lng: Double) {
                // Cek apakah lokasi saat ini berada di dalam radius salah satu sekolah
                val validSchool = allowedSchoolLocations.find { school ->
                    locationHelper.isWithinRadius(
                        currentLat = lat,
                        currentLng = lng,
                        targetLat = school.coordinate.latitude,
                        targetLng = school.coordinate.longitude,
                        radiusInMeters = school.radius
                    )
                }

                val isValid = validSchool != null
                val message = if (isValid) {
                    "✅ Lokasi valid di area ${validSchool?.name}"
                } else {
                    "❌ Anda berada di luar area sekolah yang diizinkan."
                }
                _uiState.value = FormAnswerUiState.LocationValidationResult(isValid, message, lat, lng)
            }
            override fun onError(message: String) {
                _uiState.value = FormAnswerUiState.LocationValidationResult(false, "❌ Gagal mendapatkan lokasi: $message", null, null)
            }
        })
    }

    // Fungsi untuk memuat detail formulir dan pertanyaan
    fun loadFormDetails(formId: Int, studentUserIdentifier: String) {
        viewModelScope.launch {
            _uiState.value = FormAnswerUiState.LoadingQuestions

            // 1. Dapatkan studentId dari email siswa (jika diperlukan untuk validasi lain)
            currentStudentId = withContext(Dispatchers.IO) { userDao.getUserByEmail(studentUserIdentifier)?.id }
            if (currentStudentId == null) {
                _uiState.value = FormAnswerUiState.Error("Gagal mengidentifikasi data siswa.")
                return@launch
            }

            // ===============================================
            // === PERBAIKAN UTAMA ADA DI SINI ===
            // ===============================================
            // 2. Ambil FormApiModel dari API menggunakan FormRepository
            val result = formRepository.getFormDetails(formId)
            result.fold(
                onSuccess = { formApiModel ->
                    // Konversi FormApiModel ke FormEntity dan QuestionEntity untuk ditampilkan di UI
                    // Ini memungkinkan UI Anda tetap menggunakan model Entity yang sudah ada
                    val formEntity = FormEntity(
                        id = formApiModel.id,
                        title = formApiModel.title,
                        description = formApiModel.description ?: "",
                        formCode = formApiModel.formCode
                    )
                    currentFormEntity = formEntity // Simpan untuk referensi

                    val questionEntities = formApiModel.questions?.map { qApi ->
                        QuestionEntity(
                            id = qApi.id,
                            formId = qApi.formId,
                            questionText = qApi.questionText,
                            questionType = QuestionType.fromString(qApi.questionType) ?: QuestionType.Text,
                            options = qApi.options ?: emptyList(),
                            required = qApi.required,
                            answer = "" // Jawaban awal kosong
                        )
                    } ?: emptyList()

                    _uiState.value = FormAnswerUiState.QuestionsLoaded(formEntity, questionEntities)
                },
                onFailure = { error ->
                    _uiState.value = FormAnswerUiState.Error(error.message ?: "Gagal memuat formulir dari server.")
                }
            )
            // ===============================================
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

    // ==============================================================================
    /*
     LOGIKA ALTERNATIF (SESUAI PERMINTAAN ANDA)
     Jika ingin siswa bisa mengisi dari mana saja, beri komentar pada fungsi `validateLocation()` di atas,
     dan hapus tanda komentar dari fungsi `validateLocationAnywhere()` di bawah ini.
     Lalu ganti namanya menjadi `validateLocation()`.
    */
    /*
    fun validateLocationAnywhere() {
        locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
            override fun onLocationResult(lat: Double, lng: Double) {
                // Saat lokasi berhasil didapat, langsung anggap valid.
                val isValid = true
                val message = "✅ Lokasi berhasil diambil ($lat, $lng)"
                // Kirim state sukses beserta data lat/lng untuk disimpan
                _uiState.value = FormAnswerUiState.LocationValidationResult(isValid, message, lat, lng)
            }
            override fun onError(message: String) {
                // Jika gagal, kirim state tidak valid.
                _uiState.value = FormAnswerUiState.LocationValidationResult(false, "❌ Gagal mendapatkan lokasi: $message", null, null)
            }
        })
    }
    */
    // ==============================================================================


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
            _uiState.value = FormAnswerUiState.Error("Siswa tidak teridentifikasi. Tidak bisa mengirim jawaban.")
            return
        }
        if (!isLocationPreviouslyValidatedAndCorrect) {
            _uiState.value = FormAnswerUiState.Error("Validasi lokasi gagal atau belum dilakukan dengan benar.")
            return
        }
        if (photoFile == null) {
            _uiState.value = FormAnswerUiState.Error("Foto bukti wajib disertakan.")
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
                    _uiState.value = FormAnswerUiState.Error(exception.message ?: "Gagal mengirim jawaban ke server")
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
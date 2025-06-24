package com.example.eform.ui.viewmodel

import android.app.Application
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.ApiService
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.local.UserPreferences
import com.example.eform.data.model.DraftAnswerEntity
import com.example.eform.data.model.FormDraftEntity
import com.example.eform.data.model.FormEntity // Digunakan di FormAnswerUiState
import com.example.eform.data.model.NotificationEntity
import com.example.eform.data.model.QuestionEntity // Digunakan di FormAnswerUiState
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.model.api.FormResponseApiModel
import com.example.eform.data.model.api.QuestionApiModel
import com.example.eform.data.repository.AuthRepository
import com.example.eform.data.repository.FormRepository
import com.example.eform.data.repository.DraftRepository
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.utils.LocationHelper
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import androidx.compose.runtime.mutableStateMapOf // Import ini
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// Import AnswerPayload secara eksplisit
import com.example.eform.data.api.ApiService.AnswerPayload
// DITAMBAHKAN: Import untuk model lokasi dari API
import com.example.eform.data.model.api.LocationApiModel

// DITAMBAHKAN: Data class SchoolLocation tidak lagi diperlukan
// data class SchoolLocation(
//     val name: String,
//     val coordinate: LatLng,
//     val radius: Double = 150.0
// )

// Sealed class untuk state UI FormAnswerScreen
sealed class FormAnswerUiState {
    object Idle : FormAnswerUiState()
    object Loading : FormAnswerUiState()
    data class Success(val form: FormApiModel) : FormAnswerUiState()
    data class Error(val message: String) : FormAnswerUiState()
}

sealed class SubmitFormResult {
    object Idle : SubmitFormResult()
    object Loading : SubmitFormResult()
    data class Success(val formResponse: FormResponseApiModel, val notificationTitle: String? = null, val notificationMessageForSytem: String? = null) : SubmitFormResult()
    data class Error(val message: String) : SubmitFormResult()
}

enum class LocationVerificationStatus {
    IDLE,
    FETCHING_API, // Mengambil data lokasi dari API
    VERIFYING,    // Memverifikasi lokasi pengguna
    VALID,        // Lokasi pengguna valid
    INVALID,      // Lokasi pengguna tidak valid (di luar radius)
    API_ERROR,    // Gagal mengambil data dari API
    GPS_ERROR     // Gagal mendapatkan lokasi GPS pengguna
}

data class LocationStatusResult(
    val status: LocationVerificationStatus,
    val message: String
)

class FormAnswerViewModel(application: Application, private val formId: Int, private val userIdentifier: String) : AndroidViewModel(application) {

    private val apiService: ApiService = RetrofitInstance.api // DITAMBAHKAN: Inisialisasi ApiService
    private val formRepository: FormRepository = FormRepository(apiService)
    private val authRepository: AuthRepository = AuthRepository(apiService, UserPreferences(getApplication()))
    private val draftRepository: DraftRepository
    private val userDao = AppDatabase.getDatabase(getApplication()).userDao()
    private val notificationDao = AppDatabase.getDatabase(getApplication()).notificationDao()

    private val locationHelper = LocationHelper(getApplication())
    private val gson = Gson()

    // UI States
    private val _uiState = MutableStateFlow<FormAnswerUiState>(FormAnswerUiState.Loading)
    val uiState: StateFlow<FormAnswerUiState> = _uiState.asStateFlow()

    private val _submitResult = MutableStateFlow<SubmitFormResult>(SubmitFormResult.Idle)
    val submitResult: StateFlow<SubmitFormResult> = _submitResult.asStateFlow()

    // Data yang dikelola ViewModel untuk jawaban dan input lainnya
    val answers = MutableStateFlow<MutableMap<Int, String>>(mutableStateMapOf())
    val photoUri = MutableStateFlow<Uri?>(null)
    val currentPhotoPathFromDraft = MutableStateFlow<String?>(null)
    val location = MutableStateFlow<Location?>(null)
    private val _locationStatus = MutableLiveData<LocationStatusResult>(
        LocationStatusResult(LocationVerificationStatus.IDLE, "Tekan tombol untuk validasi")
    )
    val locationStatus: LiveData<LocationStatusResult> = _locationStatus

    private var currentStudentId: Int? = null

    // DITAMBAHKAN: State untuk menyimpan lokasi dari API
    private val _apiLocations = MutableStateFlow<List<LocationApiModel>>(emptyList())

    // DIHAPUS: Daftar lokasi sekolah yang hardcoded
    // val allowedSchoolLocations = listOf( ... )

    init {
        val database = AppDatabase.getDatabase(getApplication())
        draftRepository = DraftRepository(database.draftDao())
        loadFormAndDraftDetails()
        loadCurrentUserId()
        // DITAMBAHKAN: Panggil fungsi untuk mengambil lokasi dari API saat ViewModel dibuat
        fetchApiLocations()
    }

    // DITAMBAHKAN: Fungsi baru untuk mengambil data lokasi dari API
    private fun fetchApiLocations() {
        viewModelScope.launch {
            _locationStatus.postValue(LocationStatusResult(LocationVerificationStatus.FETCHING_API, "Mengambil data lokasi..."))
            try {
                val response = apiService.getLocations()
                if (response.isSuccessful && response.body() != null) {
                    _apiLocations.value = response.body()!!
                    _locationStatus.postValue(LocationStatusResult(LocationVerificationStatus.IDLE, "Data lokasi siap. Silakan validasi."))
                } else {
                    _locationStatus.postValue(LocationStatusResult(LocationVerificationStatus.API_ERROR, "Gagal mengambil data lokasi."))
                }
            } catch (e: Exception) {
                _locationStatus.postValue(LocationStatusResult(LocationVerificationStatus.API_ERROR, "Gagal koneksi ke server lokasi."))
            }
        }
    }


    private fun loadCurrentUserId() {
        viewModelScope.launch {
            currentStudentId = withContext(Dispatchers.IO) { userDao.getUserByEmail(userIdentifier)?.id }
            if (currentStudentId == null) {
                Log.e("FormAnswerVM", "User ID not found for userIdentifier: $userIdentifier")
                _uiState.value = FormAnswerUiState.Error("Gagal mengidentifikasi pengguna.")
            }
        }
    }

    private fun loadFormAndDraftDetails() {
        viewModelScope.launch {
            _uiState.value = FormAnswerUiState.Loading

            val formResult = formRepository.getFormDetails(formId)
            formResult.fold(
                onSuccess = { formApiModel ->
                    _uiState.value = FormAnswerUiState.Success(formApiModel)
                    val initialAnswers = mutableStateMapOf<Int, String>()
                    formApiModel.questions?.forEach { question ->
                        initialAnswers[question.id] = ""
                    }
                    answers.value = initialAnswers
                    loadDraft()
                },
                onFailure = { throwable ->
                    _uiState.value = FormAnswerUiState.Error(throwable.message ?: "Gagal memuat formulir dari server.")
                }
            )
        }
    }

    private fun loadDraft() {
        viewModelScope.launch {
            val draft = draftRepository.getDraft(formId, userIdentifier)
            if (draft != null) {
                Log.d("FormAnswerVM", "Draft ditemukan untuk form ${formId} oleh ${userIdentifier}.")
                val restoredAnswers = mutableStateMapOf<Int, String>()
                restoredAnswers.putAll(answers.value)

                draft.answers.forEach { draftAnswer ->
                    val questionId = draftAnswer.questionId
                    when (QuestionType.fromString(draftAnswer.questionType)) {
                        QuestionType.Text, QuestionType.MultipleChoice, QuestionType.true_false -> {
                            restoredAnswers[questionId] = draftAnswer.answerText ?: ""
                        }
                        QuestionType.Checkbox -> {
                            if (!draftAnswer.selectedOptions.isNullOrBlank()) {
                                try {
                                    val type = object : TypeToken<List<String>>() {}.type
                                    val selectedList: List<String> = gson.fromJson(draftAnswer.selectedOptions, type)
                                    restoredAnswers[questionId] = gson.toJson(selectedList)
                                } catch (e: Exception) {
                                    Log.e("FormAnswerVM", "Error parsing checkbox draft: ${draftAnswer.selectedOptions}, ${e.message}")
                                    restoredAnswers[questionId] = draftAnswer.selectedOptions ?: ""
                                }
                            } else {
                                restoredAnswers[questionId] = ""
                            }
                        }
                        QuestionType.LinearScale -> {
                            restoredAnswers[questionId] = draftAnswer.linearScaleValue?.toString() ?: ""
                        }
                        else -> {
                            restoredAnswers[questionId] = ""
                        }
                    }
                }
                answers.value = restoredAnswers

                draft.formDraft.photoPath?.let {
                    currentPhotoPathFromDraft.value = it
                    photoUri.value = Uri.parse(it)
                }

                Log.d("FormAnswerVM", "Draft loaded. Answers: ${answers.value.toMap()}")
            } else {
                Log.d("FormAnswerVM", "No draft found for form ${formId} by ${userIdentifier}.")
            }
        }
    }

    fun onAnswerChanged(questionId: Int, newAnswer: String) {
        answers.value = answers.value.apply { this[questionId] = newAnswer }
        Log.d("FormAnswerVM", "Answer for Q${questionId} updated to: $newAnswer")
    }

    fun onPhotoUriChanged(uri: Uri?) {
        photoUri.value = uri
        currentPhotoPathFromDraft.value = uri?.toString()
        Log.d("FormAnswerVM", "Photo URI updated to: $uri")
    }

    fun onLocationChanged(loc: Location?) {
        location.value = loc
        Log.d("FormAnswerVM", "Location updated to: ${loc?.latitude}, ${loc?.longitude}")
    }

    // DIMODIFIKASI: Fungsi ini sekarang memvalidasi lokasi menggunakan data dari API
    fun validateLocation() {
        if (_apiLocations.value.isEmpty()) {
            _locationStatus.postValue(LocationStatusResult(LocationVerificationStatus.API_ERROR, "Data lokasi belum siap, mencoba lagi..."))
            fetchApiLocations()
            return
        }

        _locationStatus.postValue(LocationStatusResult(LocationVerificationStatus.VERIFYING, "Memverifikasi lokasi Anda..."))

        locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
            override fun onLocationResult(lat: Double, lng: Double) {
                val validApiLocation = _apiLocations.value.find { apiLoc ->
                    locationHelper.isWithinRadius(lat, lng, apiLoc.latitude, apiLoc.longitude, apiLoc.radius)
                }

                location.value = Location("").apply {
                    this.latitude = lat
                    this.longitude = lng
                }

                if (validApiLocation != null) {
                    // SUKSES: Kirim status VALID dengan pesan yang memuat nama lokasi
                    _locationStatus.postValue(
                        LocationStatusResult(LocationVerificationStatus.VALID, "✅ Lokasi Valid: ${validApiLocation.name}")
                    )
                } else {
                    // GAGAL: Kirim status INVALID
                    _locationStatus.postValue(
                        LocationStatusResult(LocationVerificationStatus.INVALID, "❌ Anda berada di luar area yang diizinkan.")
                    )
                }
            }

            override fun onError(message: String) {
                location.value = null
                _locationStatus.postValue(
                    LocationStatusResult(LocationVerificationStatus.GPS_ERROR, "❌ Gagal mendapatkan lokasi: $message")
                )
            }
        })
    }


    fun saveDraft(currentAnswersMap: Map<Int, String>, currentPhotoUri: Uri?, currentFormTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val formApiModel = (_uiState.value as? FormAnswerUiState.Success)?.form
            if (formApiModel == null) {
                Log.e("FormAnswerVM", "Failed to save draft: Form details not loaded.")
                return@launch
            }

            val answersForDraft = mutableListOf<DraftAnswerEntity>()
            formApiModel.questions?.forEach { question ->
                val answerValue = currentAnswersMap[question.id]
                answersForDraft.add(
                    when (QuestionType.fromString(question.questionType)) {
                        QuestionType.Text, QuestionType.MultipleChoice, QuestionType.true_false ->
                            DraftAnswerEntity(
                                questionId = question.id,
                                questionType = question.questionType,
                                answerText = answerValue,
                                selectedOptions = null,
                                linearScaleValue = null,
                                formDraftId = 0
                            )
                        QuestionType.Checkbox -> {
                            val selectedOptionsList = if (answerValue?.startsWith("[") == true && answerValue.endsWith("]")) {
                                answerValue
                            } else if (!answerValue.isNullOrBlank()) {
                                gson.toJson(answerValue.split(",").map { it.trim() }.filter { it.isNotBlank() })
                            } else {
                                null
                            }
                            DraftAnswerEntity(
                                questionId = question.id,
                                questionType = question.questionType,
                                answerText = null,
                                selectedOptions = selectedOptionsList,
                                linearScaleValue = null,
                                formDraftId = 0
                            )
                        }
                        QuestionType.LinearScale ->
                            DraftAnswerEntity(
                                questionId = question.id,
                                questionType = question.questionType,
                                answerText = null,
                                selectedOptions = null,
                                linearScaleValue = answerValue?.toIntOrNull(),
                                formDraftId = 0
                            )
                        else -> DraftAnswerEntity(
                            questionId = question.id,
                            questionType = question.questionType,
                            answerText = null, selectedOptions = null, linearScaleValue = null, formDraftId = 0
                        )
                    }
                )
            }

            val photoPath = currentPhotoUri?.toString()

            val formDraft = FormDraftEntity(
                formId = formId,
                userIdentifier = userIdentifier,
                formTitle = currentFormTitle,
                lastSaved = Date(),
                photoPath = photoPath
            )

            draftRepository.saveFormDraft(
                formDraft = formDraft,
                answers = answersForDraft.toList()
            )
            Log.d("FormAnswerVM", "Draft saved for form $formId for user $userIdentifier.")
        }
    }

    fun deleteDraft() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("FormAnswerVM", "Deleting draft for form $formId by $userIdentifier")
            draftRepository.deleteDraft(formId, userIdentifier)
            Log.d("FormAnswerVM", "Draft deleted for form $formId.")
            photoUri.value = null
            currentPhotoPathFromDraft.value = null
        }
    }

    fun submitAnswers(
        photoFile: File?,
        currentLocation: Location?
    ) {
        viewModelScope.launch {
            _submitResult.value = SubmitFormResult.Loading
            val formApiModel = (_uiState.value as? FormAnswerUiState.Success)?.form
            if (formApiModel == null) {
                _submitResult.value = SubmitFormResult.Error("Formulir belum dimuat dengan benar.")
                return@launch
            }

            // DIMODIFIKASI: Validasi lokasi sekarang menggunakan data dari API
            val isLocationValid = currentLocation != null && _apiLocations.value.any { apiLoc ->
                locationHelper.isWithinRadius(
                    currentLat = currentLocation.latitude,
                    currentLng = currentLocation.longitude,
                    targetLat = apiLoc.latitude,
                    targetLng = apiLoc.longitude,
                    radiusInMeters = apiLoc.radius
                )
            }

            val isAllAnswered = formApiModel.questions?.all { question ->
                val answer = answers.value[question.id]
                if (question.required) {
                    when (QuestionType.fromString(question.questionType)) {
                        QuestionType.Text, QuestionType.MultipleChoice, QuestionType.true_false -> !answer.isNullOrBlank()
                        QuestionType.Checkbox -> {
                            !answer.isNullOrBlank() && answer != "[]" && answer != gson.toJson(emptyList<String>())
                        }
                        QuestionType.LinearScale -> answer?.toIntOrNull() != null
                        else -> false
                    }
                } else {
                    true
                }
            } ?: false

            if (formApiModel.locationRequired == true && currentLocation == null) {
                _submitResult.value = SubmitFormResult.Error("Lokasi wajib diisi.")
                return@launch
            }
            if (formApiModel.locationRequired == true && !isLocationValid) {
                _submitResult.value = SubmitFormResult.Error("Lokasi tidak valid atau di luar area yang diizinkan.")
                return@launch
            }
            if (formApiModel.photoRequired == true && photoFile == null && photoUri.value == null && currentPhotoPathFromDraft.value == null) {
                _submitResult.value = SubmitFormResult.Error("Foto bukti wajib disertakan.")
                return@launch
            }
            if (!isAllAnswered) {
                _submitResult.value = SubmitFormResult.Error("Harap lengkapi semua pertanyaan wajib.")
                return@launch
            }

            val questionAnswers = mutableListOf<AnswerPayload>()
            answers.value.forEach { (questionId, answerText) ->
                val question = formApiModel.questions?.find { it.id == questionId }
                if (question != null) {
                    when (QuestionType.fromString(question.questionType)) {
                        QuestionType.Text, QuestionType.MultipleChoice, QuestionType.true_false -> {
                            questionAnswers.add(
                                AnswerPayload(
                                    questionId = question.id,
                                    answerText = answerText,
                                    selectedOptions = null,
                                    linearScaleValue = null
                                )
                            )
                        }
                        QuestionType.Checkbox -> {
                            val jsonSelectedOptions = answerText
                            questionAnswers.add(
                                AnswerPayload(
                                    questionId = question.id,
                                    answerText = null,
                                    selectedOptions = jsonSelectedOptions,
                                    linearScaleValue = null
                                )
                            )
                        }
                        QuestionType.LinearScale -> {
                            questionAnswers.add(
                                AnswerPayload(
                                    questionId = question.id,
                                    answerText = null,
                                    selectedOptions = null,
                                    linearScaleValue = answerText.toIntOrNull()
                                )
                            )
                        }
                        else -> { /* Handle unknown types if necessary or ignore */ }
                    }
                }
            }

            val formIdRequestBody = formId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitude = currentLocation?.latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val longitude = currentLocation?.longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val answersJson = gson.toJson(questionAnswers).toRequestBody("application/json".toMediaTypeOrNull())

            var photoPart: MultipartBody.Part? = null
            val finalPhotoFile = photoFile ?: photoUri.value?.let { uri ->
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                        inputStream?.use { input ->
                            val tempFile = File(getApplication<Application>().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                            tempFile
                        }
                    } catch (e: Exception) {
                        Log.e("FormAnswerVM", "Error converting Uri to File for submission: ${e.message}")
                        null
                    }
                }
            }

            finalPhotoFile?.let {
                val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
                photoPart = MultipartBody.Part.createFormData("photo", it.name, requestFile)
            }

            formRepository.submitFormResponse(
                formIdRequestBody,
                latitude,
                longitude,
                answersJson,
                photoPart
            ).fold(
                onSuccess = { formResponseApiModel ->
                    val formTitleForNotification = formApiModel.title
                    val notificationMessage = "Anda berhasil mengisi formulir: $formTitleForNotification!"
                    val newNotification = NotificationEntity(
                        userId = currentStudentId ?: 0,
                        title = "Formulir Terkirim!",
                        message = notificationMessage
                    )
                    withContext(Dispatchers.IO) { notificationDao.insertNotification(newNotification) }

                    val currentNotifCount = withContext(Dispatchers.IO) { notificationDao.getNotificationCountForUser(currentStudentId ?: 0) }
                    if (currentNotifCount > 8) {
                        val oldest = withContext(Dispatchers.IO) {notificationDao.getOldestNotificationForUser(currentStudentId ?: 0)}
                        oldest?.let { withContext(Dispatchers.IO) {notificationDao.deleteNotificationById(it.id)} }
                    }

                    _submitResult.value = SubmitFormResult.Success(
                        formResponse = formResponseApiModel,
                        notificationTitle = "Formulir Berhasil Diisi!",
                        notificationMessageForSytem = notificationMessage
                    )
                },
                onFailure = { throwable ->
                    _submitResult.value = SubmitFormResult.Error(throwable.message ?: "Gagal mengirim formulir")
                }
            )
        }
    }

    fun resetSubmitResult() {
        _submitResult.value = SubmitFormResult.Idle
    }

    fun resetUiState() {
        _uiState.value = FormAnswerUiState.Idle
    }

    class FormAnswerViewModelFactory(
        private val application: Application,
        private val formId: Int,
        private val userIdentifier: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FormAnswerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FormAnswerViewModel(application, formId, userIdentifier) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
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
import com.example.eform.data.model.NotificationEntity
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.model.api.FormResponseApiModel
import com.example.eform.data.repository.FormRepository
import com.example.eform.data.repository.DraftRepository
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.utils.LocationHelper
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.eform.data.api.ApiService.AnswerPayload

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

// Enum untuk status lokasi
enum class LocationVerificationStatus {
    NOT_VERIFIED,
    VERIFYING,
    VALID,
    INVALID,
    GPS_UNAVAILABLE,
    API_ERROR
}

// Wrapper untuk status dan pesan
data class LocationStatusResult(
    val status: LocationVerificationStatus,
    val message: String
)

class FormAnswerViewModel(application: Application, private val formId: Int, private val userIdentifier: String) : AndroidViewModel(application) {

    private val apiService: ApiService = RetrofitInstance.api
    private val formRepository: FormRepository = FormRepository(apiService)
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

    // Data yang dikelola ViewModel
    val answers = MutableStateFlow<MutableMap<Int, String>>(mutableStateMapOf())
    val photoUri = MutableStateFlow<Uri?>(null)
    val currentPhotoPathFromDraft = MutableStateFlow<String?>(null)
    val location = MutableStateFlow<Location?>(null)

    private val _locationStatus = MutableLiveData(
        LocationStatusResult(LocationVerificationStatus.NOT_VERIFIED, "Anda belum melakukan verifikasi lokasi")
    )
    val locationStatus: LiveData<LocationStatusResult> = _locationStatus

    private var currentStudentId: Int? = null

    init {
        val database = AppDatabase.getDatabase(getApplication())
        draftRepository = DraftRepository(database.draftDao())
        loadFormAndDraftDetails()
        loadCurrentUserId()
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
                Log.d("FormAnswerVM", "Draft ditemukan.")
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
    }

    fun onPhotoUriChanged(uri: Uri?) {
        photoUri.value = uri
        currentPhotoPathFromDraft.value = uri?.toString()
    }

    fun onLocationChanged(loc: Location?) {
        location.value = loc
    }

    fun validateLocation() {
        _locationStatus.postValue(LocationStatusResult(LocationVerificationStatus.VERIFYING, "Memverifikasi lokasi Anda..."))

        locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
            override fun onLocationResult(lat: Double, lng: Double) {
                location.value = Location("user_location").apply {
                    this.latitude = lat
                    this.longitude = lng
                }

                viewModelScope.launch {
                    try {
                        // PERBAIKAN: Memanggil fungsi `verifyLocation` sesuai nama di ApiService.kt
                        val response = apiService.verifyLocation(lat, lng)

                        if (response.isSuccessful && response.body() != null) {
                            val validationResult = response.body()!!
                            if (validationResult.status == "valid") {
                                _locationStatus.postValue(
                                    LocationStatusResult(
                                        LocationVerificationStatus.VALID,
                                        "Lokasi Valid: ${validationResult.locationName}"
                                    )
                                )
                            } else {
                                _locationStatus.postValue(
                                    LocationStatusResult(
                                        LocationVerificationStatus.INVALID,
                                        validationResult.message
                                    )
                                )
                            }
                        } else {
                            // PERBAIKAN: Logika parsing error JSON yang lebih aman
                            val errorBody = response.errorBody()?.string()
                            if (errorBody != null) {
                                val errorMessage = try {
                                    // Menggunakan data class dari ApiService untuk menghindari ambiguitas
                                    gson.fromJson(errorBody, ApiService.LocationVerificationResponse::class.java).message
                                } catch (e: Exception) {
                                    "Anda berada di luar area yang diperbolehkan"
                                }
                                _locationStatus.postValue(
                                    LocationStatusResult(LocationVerificationStatus.INVALID, errorMessage)
                                )
                            } else {
                                _locationStatus.postValue(
                                    LocationStatusResult(LocationVerificationStatus.API_ERROR, "Gagal memvalidasi lokasi.")
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FormAnswerVM", "API call for location validation failed", e)
                        _locationStatus.postValue(
                            LocationStatusResult(LocationVerificationStatus.API_ERROR, "Gagal terhubung ke server validasi.")
                        )
                    }
                }
            }

            override fun onError(message: String) {
                location.value = null
                _locationStatus.postValue(
                    LocationStatusResult(LocationVerificationStatus.GPS_UNAVAILABLE, "Anda belum menghidupkan lokasi/GPS.")
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

            val isLocationValid = locationStatus.value?.status == LocationVerificationStatus.VALID

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

            if (formApiModel.locationRequired == true && !isLocationValid) {
                _submitResult.value = SubmitFormResult.Error("Lokasi tidak valid atau belum diverifikasi.")
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
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

// Import AnswerPayload secara eksplisit
import com.example.eform.data.api.ApiService.AnswerPayload

data class SchoolLocation(
    val name: String,
    val coordinate: LatLng,
    val radius: Double = 150.0
)

// Sealed class untuk state UI FormAnswerScreen
sealed class FormAnswerUiState {
    object Idle : FormAnswerUiState()
    object Loading : FormAnswerUiState() // Menggantikan LoadingQuestions
    data class Success(val form: FormApiModel) : FormAnswerUiState() // Menggantikan QuestionsLoaded, langsung FormApiModel
    data class Error(val message: String) : FormAnswerUiState()
}

sealed class SubmitFormResult {
    object Idle : SubmitFormResult()
    object Loading : SubmitFormResult()
    data class Success(val formResponse: FormResponseApiModel, val notificationTitle: String? = null, val notificationMessageForSytem: String? = null) : SubmitFormResult()
    data class Error(val message: String) : SubmitFormResult()
}


class FormAnswerViewModel(application: Application, private val formId: Int, private val userIdentifier: String) : AndroidViewModel(application) {

    private val formRepository: FormRepository = FormRepository(RetrofitInstance.api)
    private val authRepository: AuthRepository = AuthRepository(RetrofitInstance.api, UserPreferences(getApplication())) // Gunakan getApplication()
    private val draftRepository: DraftRepository
    private val userDao = AppDatabase.getDatabase(getApplication()).userDao() // Gunakan getApplication()
    private val notificationDao = AppDatabase.getDatabase(getApplication()).notificationDao() // Gunakan getApplication()

    private val locationHelper = LocationHelper(getApplication()) // Gunakan getApplication()
    private val gson = Gson()

    // UI States
    private val _uiState = MutableStateFlow<FormAnswerUiState>(FormAnswerUiState.Loading)
    val uiState: StateFlow<FormAnswerUiState> = _uiState.asStateFlow()

    private val _submitResult = MutableStateFlow<SubmitFormResult>(SubmitFormResult.Idle)
    val submitResult: StateFlow<SubmitFormResult> = _submitResult.asStateFlow()

    // Data yang dikelola ViewModel untuk jawaban dan input lainnya
    val answers = MutableStateFlow<MutableMap<Int, String>>(mutableStateMapOf()) // Menggunakan MutableStateFlow untuk Map
    val photoUri = MutableStateFlow<Uri?>(null) // URI foto yang diambil (baru atau dari draft)
    val currentPhotoPathFromDraft = MutableStateFlow<String?>(null) // Path foto dari draft (untuk tampilan)
    val location = MutableStateFlow<Location?>(null)

    private var currentStudentId: Int? = null


    // Daftar lokasi sekolah yang diizinkan (hardcoded)
    val allowedSchoolLocations = listOf(
        SchoolLocation(name = "SMA Negeri 7 Lhokseumawe", coordinate = LatLng(5.22308, 97.05332), radius = 300.0), // Koordinat dari file Anda
        SchoolLocation(name = "SMA Negeri 5 Lhokseumawe", coordinate = LatLng(5.173443, 97.123116), radius = 300.0), // Koordinat perkiraan
        SchoolLocation(name = "SMA Negeri 6 Lhokseumawe", coordinate = LatLng(5.11940, 97.17401), radius = 300.0),  // Koordinat perkiraan
        SchoolLocation(name = "Lokasi Testing Rumah", coordinate = LatLng(3.579741, 98.621874), radius = 300.0),
        SchoolLocation(name = "Lokasi Testing Kost", coordinate = LatLng(5.211149, 97.08079), radius = 300.0)
    )

    init {
        val database = AppDatabase.getDatabase(getApplication()) // Gunakan getApplication()
        draftRepository = DraftRepository(database.draftDao())
        loadFormAndDraftDetails()
        loadCurrentUserId()
    }

    private fun loadCurrentUserId() {
        viewModelScope.launch {
            currentStudentId = withContext(Dispatchers.IO) { userDao.getUserByEmail(userIdentifier)?.id }
            if (currentStudentId == null) {
                // Log or handle error: User ID not found in local DB
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
                    // Inisialisasi map jawaban dengan semua pertanyaan kosong
                    val initialAnswers = mutableStateMapOf<Int, String>()
                    formApiModel.questions?.forEach { question ->
                        initialAnswers[question.id] = "" // Set initial empty answer
                    }
                    answers.value = initialAnswers

                    // Kemudian coba muat draft
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
                val restoredAnswers = mutableStateMapOf<Int, String>() // Inisialisasi mutableStateMapOf baru
                restoredAnswers.putAll(answers.value) // Salin jawaban saat ini (biasanya kosong atau default)

                draft.answers.forEach { draftAnswer ->
                    val questionId = draftAnswer.questionId
                    when (QuestionType.fromString(draftAnswer.questionType)) { // Menggunakan fromString
                        QuestionType.Text, QuestionType.MultipleChoice, QuestionType.true_false -> {
                            restoredAnswers[questionId] = draftAnswer.answerText ?: ""
                        }
                        QuestionType.Checkbox -> {
                            // Cek apakah selectedOptions bukan null atau string kosong
                            if (!draftAnswer.selectedOptions.isNullOrBlank()) {
                                try {
                                    // Asumsi draftAnswer.selectedOptions adalah JSON array string
                                    val type = object : TypeToken<List<String>>() {}.type
                                    val selectedList: List<String> = gson.fromJson(draftAnswer.selectedOptions, type)
                                    restoredAnswers[questionId] = gson.toJson(selectedList) // Simpan sebagai JSON string
                                } catch (e: Exception) {
                                    Log.e("FormAnswerVM", "Error parsing checkbox draft: ${draftAnswer.selectedOptions}, ${e.message}")
                                    // Fallback if parsing fails (e.g., old format)
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
                            // Fallback for unknown/unhandled types
                            restoredAnswers[questionId] = ""
                        }
                    }
                }
                answers.value = restoredAnswers // Update StateFlow dengan Map yang dipulihkan

                // Pulihkan foto dari draft
                draft.formDraft.photoPath?.let {
                    currentPhotoPathFromDraft.value = it // Simpan path string asli untuk tampilan
                    photoUri.value = Uri.parse(it) // Coba parse ke Uri
                }

                // Pulihkan lokasi dari draft (jika disimpan di FormDraftEntity)
                // Saat ini FormDraftEntity belum menyimpan lat/lon, hanya photoPath.
                // Jika ingin lokasi juga disimpan, FormDraftEntity perlu diupdate.
                // For now, location will be taken fresh or explicitly from UI input.

                Log.d("FormAnswerVM", "Draft loaded. Answers: ${answers.value.toMap()}")
                // Toast shown in UI Composable via LaunchedEffect(uiState) or directly.
            } else {
                Log.d("FormAnswerVM", "No draft found for form ${formId} by ${userIdentifier}.")
            }
        }
    }

    // Fungsi untuk memperbarui jawaban dari UI
    fun onAnswerChanged(questionId: Int, newAnswer: String) {
        // Langsung modifikasi map dan reassign value untuk memicu update pada StateFlow
        answers.value = answers.value.apply { this[questionId] = newAnswer }
        Log.d("FormAnswerVM", "Answer for Q${questionId} updated to: $newAnswer")
    }

    // Fungsi untuk memperbarui URI foto dari UI
    fun onPhotoUriChanged(uri: Uri?) {
        photoUri.value = uri
        currentPhotoPathFromDraft.value = uri?.toString() // Update path juga
        Log.d("FormAnswerVM", "Photo URI updated to: $uri")
    }

    // Fungsi untuk memperbarui lokasi dari UI
    fun onLocationChanged(loc: Location?) {
        location.value = loc
        Log.d("FormAnswerVM", "Location updated to: ${loc?.latitude}, ${loc?.longitude}")
    }

    // Fungsi untuk memvalidasi lokasi saat ini (menggunakan hardcoded locations)
    fun validateLocation() {
        locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
            override fun onLocationResult(lat: Double, lng: Double) {
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
                // Update location state with new LatLng, but ensure UI observes `location` StateFlow for lat/lng
                location.value = Location("").apply {
                    this.latitude = lat
                    this.longitude = lng
                }
                // Jika Anda ingin menampilkan pesan validasi di UI, _uiState perlu diupdate juga
                // _uiState.value = FormAnswerUiState.LocationValidationResult(isValid, message, lat, lng)
                Log.d("FormAnswerVM", "Location validation result: $message")
                // Toast pesan akan ditampilkan di UI berdasarkan pengamatan perubahan state
            }
            override fun onError(message: String) {
                location.value = null // Clear location on error
                Log.e("FormAnswerVM", "Failed to get location: $message")
                // Toast pesan akan ditampilkan di UI
            }
        })
    }

    // Fungsi untuk menyimpan draft formulir
    fun saveDraft(currentAnswersMap: Map<Int, String>, currentPhotoUri: Uri?, currentFormTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val formApiModel = (_uiState.value as? FormAnswerUiState.Success)?.form
            if (formApiModel == null) {
                Log.e("FormAnswerVM", "Failed to save draft: Form details not loaded.")
                return@launch
            }

            // Map answers to DraftAnswerEntity format
            val answersForDraft = mutableListOf<DraftAnswerEntity>()
            formApiModel.questions?.forEach { question ->
                val answerValue = currentAnswersMap[question.id]
                answersForDraft.add(
                    when (QuestionType.fromString(question.questionType)) { // Menggunakan fromString
                        QuestionType.Text, QuestionType.MultipleChoice, QuestionType.true_false ->
                            DraftAnswerEntity(
                                questionId = question.id,
                                questionType = question.questionType, // String representation
                                answerText = answerValue,
                                selectedOptions = null,
                                linearScaleValue = null,
                                formDraftId = 0 // Akan diisi oleh DAO
                            )
                        QuestionType.Checkbox -> {
                            val selectedOptionsList = if (answerValue?.startsWith("[") == true && answerValue.endsWith("]")) {
                                // Already a JSON string, use as is
                                answerValue
                            } else if (!answerValue.isNullOrBlank()) {
                                // Convert comma-separated string to JSON array string
                                gson.toJson(answerValue.split(",").map { it.trim() }.filter { it.isNotBlank() })
                            } else {
                                null
                            }
                            DraftAnswerEntity(
                                questionId = question.id,
                                questionType = question.questionType, // String representation
                                answerText = null,
                                selectedOptions = selectedOptionsList,
                                linearScaleValue = null,
                                formDraftId = 0 // Akan diisi oleh DAO
                            )
                        }
                        QuestionType.LinearScale ->
                            DraftAnswerEntity(
                                questionId = question.id,
                                questionType = question.questionType, // String representation
                                answerText = null,
                                selectedOptions = null,
                                linearScaleValue = answerValue?.toIntOrNull(),
                                formDraftId = 0 // Akan diisi oleh DAO
                            )
                        else -> DraftAnswerEntity(
                            questionId = question.id,
                            questionType = question.questionType,
                            answerText = null, selectedOptions = null, linearScaleValue = null, formDraftId = 0
                        ) // Fallback
                    }
                )
            }

            val photoPath = currentPhotoUri?.toString() // Simpan Uri sebagai string

            val formDraft = FormDraftEntity(
                formId = formId,
                userIdentifier = userIdentifier,
                formTitle = currentFormTitle,
                lastSaved = Date(),
                photoPath = photoPath
            )

            draftRepository.saveFormDraft(
                formDraft = formDraft,
                answers = answersForDraft.toList() // Kirim list of DraftAnswerEntity
            )
            Log.d("FormAnswerVM", "Draft saved for form $formId for user $userIdentifier.")
        }
    }

    // Fungsi untuk menghapus draft formulir
    fun deleteDraft() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("FormAnswerVM", "Deleting draft for form $formId by $userIdentifier")
            draftRepository.deleteDraft(formId, userIdentifier)
            Log.d("FormAnswerVM", "Draft deleted for form $formId.")
            // Setelah draft dihapus, reset photoUri dan currentPhotoPathFromDraft di ViewModel
            photoUri.value = null
            currentPhotoPathFromDraft.value = null
        }
    }

    // Fungsi untuk mengirimkan jawaban formulir
    fun submitAnswers( // Tetap menggunakan nama submitAnswers sesuai permintaan Anda
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

            val isLocationValid = currentLocation != null && allowedSchoolLocations.any { school ->
                locationHelper.isWithinRadius(
                    currentLat = currentLocation.latitude,
                    currentLng = currentLocation.longitude,
                    targetLat = school.coordinate.latitude,
                    targetLng = school.coordinate.longitude,
                    radiusInMeters = school.radius
                )
            }


            // Validasi di sisi ViewModel sebelum kirim
            val isAllAnswered = formApiModel.questions?.all { question ->
                val answer = answers.value[question.id]
                if (question.required) {
                    when (QuestionType.fromString(question.questionType)) { // Menggunakan fromString
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


            // Fix locationRequired and photoRequired unresolved references
            // Mengakses properti ini secara aman dengan ?. (safe call operator)
            // Ini akan memastikan kompilasi jika properti mungkin null, TAPI jika "Unresolved reference"
            // masih terjadi, itu berarti properti itu TIDAK ADA sama sekali di FormApiModel.kt Anda.
            // Anda HARUS menambahkannya di FormApiModel.kt
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

            val questionAnswers = mutableListOf<AnswerPayload>() // Gunakan AnswerPayload yang sudah diimpor
            answers.value.forEach { (questionId, answerText) ->
                val question = formApiModel.questions?.find { it.id == questionId }
                if (question != null) {
                    when (QuestionType.fromString(question.questionType)) { // Menggunakan fromString
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
                            // answerText dari answers.value diharapkan sudah berupa JSON string
                            // dari onAnswerChanged di QuestionInput
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
            // Use the photoFile passed in, or attempt to get it from photoUri if it exists and is a content URI
            val finalPhotoFile = photoFile ?: photoUri.value?.let { uri ->
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = getApplication<Application>().contentResolver.openInputStream(uri) // Gunakan getApplication()
                        inputStream?.use { input ->
                            val tempFile = File(getApplication<Application>().cacheDir, "upload_${System.currentTimeMillis()}.jpg") // Gunakan getApplication()
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
                        userId = currentStudentId ?: 0, // Fallback to 0 if ID is null
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
package com.example.eform.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.ApiService
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.NotificationEntity
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.repository.FormRepository
import com.example.eform.ui.form.components.QuestionInputData
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class CreateFormResultUi {
    object Idle : CreateFormResultUi()
    object Loading : CreateFormResultUi()
    data class Success(
        val createdForm: FormApiModel,
        val generatedLink: String,
        val notificationMessage: String? = null, // Pesan notifikasi internal aplikasi
        val requiresSystemNotification: Boolean = false // Flag untuk memicu notifikasi sistem
    ) : CreateFormResultUi()
    data class Error(val message: String) : CreateFormResultUi()
}

class CreateFormViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val notificationDao = AppDatabase.getDatabase(application).notificationDao()

    private val _createFormResult = MutableStateFlow<CreateFormResultUi>(CreateFormResultUi.Idle)
    val createFormResult: StateFlow<CreateFormResultUi> = _createFormResult.asStateFlow()

    private val _teacherInitialized = MutableStateFlow(false)
    val teacherInitialized: StateFlow<Boolean> = _teacherInitialized.asStateFlow()

    private var currentTeacherId: Int? = null
    private var userIdentifierForInit: String? = null // Untuk melacak NIP yang digunakan untuk inisialisasi

    fun initializeTeacher(userNip: String) {
        // Hanya re-inisialisasi jika NIP berbeda atau belum pernah diinisialisasi dengan NIP ini
        if (currentTeacherId == null || userIdentifierForInit != userNip || !_teacherInitialized.value) {
            userIdentifierForInit = userNip // Simpan NIP yang sedang diproses
            viewModelScope.launch {
                Log.d("CreateFormVM", "Attempting to initialize teacher with NIP: $userNip")
                _teacherInitialized.value = false // Set false saat mulai proses inisialisasi
                currentTeacherId = null // Reset teacherId sebelum mencoba mengambil yang baru

                val teacherEntity = withContext(Dispatchers.IO) {
                    userDao.getUserByNip(userNip)
                }

                if (teacherEntity != null) {
                    currentTeacherId = teacherEntity.id
                    _teacherInitialized.value = true
                    Log.d("CreateFormVM", "Teacher ID initialized: $currentTeacherId for NIP: $userNip")
                } else {
                    _teacherInitialized.value = false // Tetap false jika guru tidak ditemukan
                    Log.e("CreateFormVM", "Failed to initialize teacher ID for NIP: $userNip. User not found in local DB.")
                }
            }
        } else {
            Log.d("CreateFormVM", "Teacher already initialized with NIP: $userNip and ID: $currentTeacherId")
            if(!_teacherInitialized.value && currentTeacherId != null) _teacherInitialized.value = true // Pastikan state konsisten
        }
    }


    fun createForm(
        title: String,
        description: String?,
        questionsData: List<QuestionInputData>
    ) {
        if (!_teacherInitialized.value || currentTeacherId == null) { // Pengecekan sudah ada
            _createFormResult.value = CreateFormResultUi.Error("Data guru belum siap atau tidak valid. Harap tunggu atau coba lagi.")
            Log.w("CreateFormVM", "CreateForm called but teacher not initialized. TeacherID: $currentTeacherId, InitializedState: ${_teacherInitialized.value}")
            return
        }
        val teacherIdForForm = currentTeacherId!! // Aman karena sudah dicek

        viewModelScope.launch {
            _createFormResult.value = CreateFormResultUi.Loading
            if (title.isBlank()) {
                _createFormResult.value = CreateFormResultUi.Error("Judul formulir tidak boleh kosong"); return@launch
            }
            if (questionsData.isEmpty()) {
                _createFormResult.value = CreateFormResultUi.Error("Minimal harus ada satu pertanyaan."); return@launch
            }
            if (questionsData.any { it.questionText.isBlank() }) {
                _createFormResult.value = CreateFormResultUi.Error("Teks pertanyaan tidak boleh kosong."); return@launch
            }
            questionsData.forEach { qd ->
                if ((qd.questionType == QuestionType.MultipleChoice || qd.questionType == QuestionType.Checkbox) && qd.options.all { it.isBlank() }) {
                    _createFormResult.value = CreateFormResultUi.Error("Pertanyaan pilihan ganda/checkbox harus memiliki minimal satu opsi yang terisi."); return@launch
                }
            }


            val questionPayloads = questionsData.map { qd ->
                ApiService.CreateQuestionPayloadApi(
                    questionText = qd.questionText,
                    questionType = qd.questionType.name,
                    options = when (qd.questionType) {
                        QuestionType.MultipleChoice, QuestionType.Checkbox -> qd.options.filter { it.isNotBlank() }.ifEmpty { null }
                        QuestionType.LinearScale -> listOf(qd.minScale.toString(), qd.maxScale.toString(), qd.minLabel, qd.maxLabel)
                        else -> null
                    },
                    required = qd.required
                )
            }
            val createFormRequest = ApiService.CreateFormRequestApi(
                title = title,
                description = description?.takeIf { it.isNotBlank() },
                questions = questionPayloads,
                teacherId = teacherIdForForm // <-- SERTAKAN teacherId DI SINI
            )

            Log.d("CreateFormVM", "Sending create form request: $createFormRequest")
            val result = formRepository.createForm(createFormRequest)

            result.fold(
                onSuccess = { createdFormApiModel ->
                    Log.d("CreateFormVM", "Form created successfully via API: ${createdFormApiModel.id}, Code: ${createdFormApiModel.formCode}")
                    val formCode = createdFormApiModel.formCode
                    val formLink = "${Constants.APP_DEEP_LINK_SCHEME}://${Constants.APP_DEEP_LINK_HOST}/${formCode}"

                    var notificationMessageForUi: String? = null
                    var requiresSystemNotificationTrigger = false

                    // Cek pencapaian setelah berhasil membuat form
                    val teacherFormsResult = formRepository.getTeacherFormsHistory() // Ini mengambil semua form guru
                    if(teacherFormsResult.isSuccess){
                        val formCount = teacherFormsResult.getOrNull()?.size ?: 0
                        Log.d("CreateFormVM", "Total forms for teacher $teacherIdForForm after creation: $formCount")
                        if (formCount > 0 && formCount % 10 == 0) { // Notifikasi setiap kelipatan 10
                            notificationMessageForUi = "Selamat! Anda telah membuat total $formCount formulir!"
                            val newNotification = NotificationEntity(
                                userId = teacherIdForForm, // ID guru yang membuat form
                                title = "Pencapaian Pembuatan Formulir!",
                                message = notificationMessageForUi
                            )
                            withContext(Dispatchers.IO) {
                                notificationDao.insertNotification(newNotification)
                                // Batasi jumlah notifikasi lokal jika perlu
                                val currentNotifCount = notificationDao.getNotificationCountForUser(teacherIdForForm)
                                if (currentNotifCount > 8) { // Batas misal 8 notifikasi
                                    val oldest = notificationDao.getOldestNotificationForUser(teacherIdForForm)
                                    oldest?.let { notificationDao.deleteNotificationById(it.id) }
                                }
                            }
                            requiresSystemNotificationTrigger = true
                            Log.d("CreateFormVM", "Achievement notification triggered for $formCount forms.")
                        }
                    } else {
                        Log.w("CreateFormVM", "Failed to get teacher forms history for achievement check.")
                    }

                    _createFormResult.value = CreateFormResultUi.Success(
                        createdFormApiModel,
                        formLink,
                        notificationMessageForUi,
                        requiresSystemNotificationTrigger
                    )
                },
                onFailure = { exception ->
                    Log.e("CreateFormVM", "Failed to create form via API: ${exception.message}", exception)
                    _createFormResult.value = CreateFormResultUi.Error(exception.message ?: "Gagal membuat formulir")
                }
            )
        }
    }

    fun resetResult() {
        _createFormResult.value = CreateFormResultUi.Idle
    }

    class CreateFormViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CreateFormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CreateFormViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

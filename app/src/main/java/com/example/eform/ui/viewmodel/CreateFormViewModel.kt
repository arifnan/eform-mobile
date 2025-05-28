package com.example.eform.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eform.data.api.ApiService // Untuk CreateFormRequestApi dan CreateQuestionPayloadApi
import com.example.eform.data.api.RetrofitInstance
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.NotificationEntity
import com.example.eform.data.model.api.FormApiModel // Impor FormApiModel
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
    // Success sekarang membawa FormApiModel, bukan hanya code & link
    data class Success(val createdForm: FormApiModel, val generatedLink: String, val notificationMessage: String? = null, val requiresSystemNotification: Boolean = false) : CreateFormResultUi()
    data class Error(val message: String) : CreateFormResultUi()
}

class CreateFormViewModel(application: Application) : AndroidViewModel(application) {

    private val formRepository = FormRepository(RetrofitInstance.api)
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val notificationDao = AppDatabase.getDatabase(application).notificationDao()

    private val _createFormResult = MutableStateFlow<CreateFormResultUi>(CreateFormResultUi.Idle)
    val createFormResult: StateFlow<CreateFormResultUi> = _createFormResult.asStateFlow()

    private var currentTeacherId: Int? = null
    private var currentUserIdentifierForNotif: String? = null
    private val _teacherInitialized = MutableStateFlow(false)

    fun isTeacherInitialized(): Boolean = _teacherInitialized.value && currentTeacherId != null

    fun initializeTeacher(userIdentifier: String) {
        this.currentUserIdentifierForNotif = userIdentifier
        viewModelScope.launch {
            _teacherInitialized.value = false
            currentTeacherId = withContext(Dispatchers.IO) { userDao.getUserByNip(userIdentifier)?.id }
            _teacherInitialized.value = true
            if (currentTeacherId == null) Log.e("CreateFormVM", "Gagal menginisialisasi teacher ID untuk NIP: $userIdentifier.")
            else Log.d("CreateFormVM", "Teacher ID initialized: $currentTeacherId for NIP: $userIdentifier")
        }
    }

    fun createForm(
        title: String,
        description: String?,
        questionsData: List<QuestionInputData>
    ) {
        val teacherIdForForm = currentTeacherId
        if (teacherIdForForm == null) {
            _createFormResult.value = CreateFormResultUi.Error("Identifikasi guru gagal. Harap login kembali.")
            return
        }

        viewModelScope.launch {
            _createFormResult.value = CreateFormResultUi.Loading
            if (title.isBlank()) {
                _createFormResult.value = CreateFormResultUi.Error("Judul formulir tidak boleh kosong"); return@launch
            }
            if (questionsData.any { it.questionText.isBlank() && (it.questionType != QuestionType.Text || it.options.all { opt -> opt.isBlank() }) }) {
                _createFormResult.value = CreateFormResultUi.Error("Pastikan semua teks pertanyaan dan opsi (jika ada) telah diisi"); return@launch
            }

            val questionPayloads = questionsData.map { qd ->
                ApiService.CreateQuestionPayloadApi(
                    questionText = qd.questionText,
                    questionType = qd.questionType.name,
                    options = when (qd.questionType) {
                        QuestionType.MultipleChoice, QuestionType.Checkbox -> if (qd.options.any { it.isNotBlank() }) qd.options.filter { it.isNotBlank() } else null
                        QuestionType.LinearScale -> listOf(qd.minScale.toString(), qd.maxScale.toString(), qd.minLabel, qd.maxLabel)
                        else -> null
                    },
                    required = qd.required
                )
            }
            val createFormRequest = ApiService.CreateFormRequestApi(title = title, description = description, questions = questionPayloads)
            val result = formRepository.createForm(createFormRequest) // Mengembalikan Result<FormApiModel>

            result.fold(
                onSuccess = { createdFormApiModel ->
                    val formCode = createdFormApiModel.formCode
                    val formLink = "${Constants.APP_DEEP_LINK_SCHEME}://${Constants.APP_DEEP_LINK_HOST}/$formCode"
                    var notificationMessageForUi: String? = null
                    var requiresSystemNotification = false

                    val teacherFormsResult = formRepository.getTeacherFormsHistory() // Panggil fungsi riwayat guru
                    if(teacherFormsResult.isSuccess){
                        val formCount = teacherFormsResult.getOrNull()?.size ?: 0
                        if (formCount > 0 && formCount % 10 == 0) {
                            notificationMessageForUi = "Anda telah membuat total ${formCount} formulir!"
                            val newNotification = NotificationEntity(userId = teacherIdForForm, title = "Pencapaian Pembuatan Formulir!", message = notificationMessageForUi)
                            withContext(Dispatchers.IO) { notificationDao.insertNotification(newNotification) } // ID tidak perlu disimpan jika tidak dipakai
                            val currentNotifCount = withContext(Dispatchers.IO) { notificationDao.getNotificationCountForUser(teacherIdForForm) }
                            if (currentNotifCount > 8) {
                                val oldest = withContext(Dispatchers.IO) {notificationDao.getOldestNotificationForUser(teacherIdForForm)}
                                oldest?.let { withContext(Dispatchers.IO) {notificationDao.deleteNotificationById(it.id)} }
                            }
                            requiresSystemNotification = true
                        }
                    }
                    _createFormResult.value = CreateFormResultUi.Success(createdFormApiModel, formLink, notificationMessageForUi, requiresSystemNotification)
                },
                onFailure = { exception ->
                    _createFormResult.value = CreateFormResultUi.Error(exception.message ?: "Gagal membuat formulir")
                }
            )
        }
    }

    fun resetResult() { _createFormResult.value = CreateFormResultUi.Idle }

    class CreateFormViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CreateFormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CreateFormViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
package com.example.eform.data.model.api

import com.google.gson.annotations.SerializedName

data class FormResponseApiModel(
    val id: Int,
    @SerializedName("form_id")
    val formId: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("photo_url")
    val photoUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("is_location_valid")
    val isLocationValid: Boolean,
    @SerializedName("submitted_at")
    val submittedAt: String,
    val answers: List<AnswerApiModel>?,
    val student: UserApiModel?,
    val form: FormApiModel? // <-- TAMBAHKAN FIELD INI
)

// AnswerApiModel tetap sama
data class AnswerApiModel(
    val id: Int,
    @SerializedName("question_id")
    val questionId: Int,
    @SerializedName("answer_text")
    val answerText: String?,
    val question: QuestionApiModel?
)

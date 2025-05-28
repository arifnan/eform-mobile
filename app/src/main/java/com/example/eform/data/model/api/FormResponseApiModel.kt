package com.example.eform.data.model.api

import com.google.gson.annotations.SerializedName

data class FormResponseApiModel(
    val id: Int,
    @SerializedName("form_id")
    val formId: Int,
    @SerializedName("student_id")
    val studentId: Int,
    @SerializedName("photo_url") // Sesuai dengan ResponseResource Anda (getPhotoUrlAttribute)
    val photoUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("is_location_valid")
    val isLocationValid: Boolean, // Di Laravel Anda integer, Gson bisa handle
    @SerializedName("submitted_at")
    val submittedAt: String,
    val answers: List<AnswerApiModel>?, // ResponseResource Anda menyertakan answers with 'whenLoaded'
    val student: UserApiModel? // ResponseResource Anda menyertakan student with 'whenLoaded'
)

// AnswerApiModel untuk di dalam FormResponseApiModel
data class AnswerApiModel(
    val id: Int,
    @SerializedName("question_id")
    val questionId: Int,
    @SerializedName("answer_text")
    val answerText: String?,
    val question: QuestionApiModel? // ResponseResource.answers menyertakan question
)
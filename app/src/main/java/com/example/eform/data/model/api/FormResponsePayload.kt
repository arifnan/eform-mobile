package com.example.eform.data.model.api

import com.google.gson.annotations.SerializedName

data class FormResponsePayload(
    @SerializedName("form_id")
    val formId: Int,
    val answers: List<AnswerPayload>, // List dari { "question_id": ..., "answer_text": ... }
    val latitude: Double?,
    val longitude: Double?
    // photo akan dikirim sebagai Multipart, jadi tidak di sini
)

data class AnswerPayload(
    @SerializedName("question_id")
    val questionId: Int,
    @SerializedName("answer_text")
    val answerText: String?
)
package com.example.eform.data.model.api

import com.google.gson.annotations.SerializedName

data class QuestionApiModel(
    val id: Int,
    @SerializedName("form_id")
    val formId: Int,
    @SerializedName("question_text")
    val questionText: String,
    @SerializedName("question_type")
    val questionType: String, // "Text", "MultipleChoice", "Checkbox", "LinearScale"
    val options: List<String>?, // Di Laravel, ini adalah array JSON, Gson akan handle deserialisasi ke List<String>
    val required: Boolean, // Di Laravel adalah integer 0/1, Gson biasanya bisa handle konversi ke Boolean
    // Tambahkan field LinearScale jika API Anda mengembalikannya secara terpisah dari 'options'
    // Misal, jika 'options' untuk LinearScale hanya berisi label, dan min/max value ada field sendiri
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("min_value")
    val minValue: Int? = null, // Untuk LinearScale
    @SerializedName("max_value")
    val maxValue: Int? = null, // Untuk LinearScale
    @SerializedName("min_label")
    val minLabel: String? = null, // Untuk LinearScale
    @SerializedName("max_label")
    val maxLabel: String? = null // Untuk LinearScale
)
package com.example.eform.ui.form.components

import androidx.room.TypeConverter
import java.util.UUID

// Data class untuk pertanyaan
data class QuestionInputData(
    val id: String = UUID.randomUUID().toString(), // Tambahkan ini
    var questionText: String = "",
    var questionType: QuestionType = QuestionType.Text,
    var options: MutableList<String> = mutableListOf(),
    var required: Boolean = false,
    var minScale: Int = 1,
    var maxScale: Int = 5,
    var minLabel: String = "",
    var maxLabel: String = ""

)

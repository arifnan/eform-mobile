// File: app/src/main/java/com/example/eform/data/model/DraftAnswerEntity.kt
package com.example.eform.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "draft_answers")
data class DraftAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formDraftId: Long, // Foreign key ke FormDraftEntity
    val questionId: Int, // ID pertanyaan dari API
    val questionType: String, // Tipe pertanyaan (Text, MultipleChoice, Checkbox, LinearScale)
    val answerText: String?, // Jawaban teks atau pilihan tunggal
    val selectedOptions: String?, // Jawaban untuk Checkbox (disimpan sebagai JSON string atau comma-separated)
    val linearScaleValue: Int? // Jawaban untuk LinearScale
)
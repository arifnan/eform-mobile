package com.example.eform.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = FormResponseEntity::class,
            parentColumns = ["id"],
            childColumns = ["formResponseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionEntity::class, // Asumsi QuestionEntity memiliki 'id' sebagai PK
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val formResponseId: Int,
    val questionId: Int,
    val answerText: String? // Jawaban bisa berupa teks, atau representasi string dari pilihan
)
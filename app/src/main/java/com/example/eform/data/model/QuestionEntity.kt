package com.example.eform.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.eform.ui.form.components.QuestionType

@Entity(
    tableName = "questions",
    foreignKeys = [ForeignKey(
        entity = FormEntity::class,
        parentColumns = ["id"],
        childColumns = ["formId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("formId")]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val formId: Int, // foreign key
    val questionText: String,
    val questionType: QuestionType,
    val options: List<String>,
    val answer: String,
    val required: Boolean = false
)


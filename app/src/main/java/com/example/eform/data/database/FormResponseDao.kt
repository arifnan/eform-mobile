package com.example.eform.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.eform.data.model.AnswerEntity
import com.example.eform.data.model.FormResponseEntity

// Data class untuk menggabungkan response dengan jawabannya
data class FormResponseWithAnswers(
    @androidx.room.Embedded val response: FormResponseEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "formResponseId"
    )
    val answers: List<AnswerEntity>
)

@Dao
interface FormResponseDao {
    @Insert
    suspend fun insertFormResponse(response: FormResponseEntity): Long // Mengembalikan ID response yang diinsert

    @Insert
    suspend fun insertAnswers(answers: List<AnswerEntity>)

    @Transaction
    @Query("SELECT * FROM form_responses WHERE formId = :formId AND studentId = :studentId")
    suspend fun getResponsesForFormByStudent(formId: Int, studentId: Int): List<FormResponseWithAnswers>

    @Transaction
    @Query("SELECT * FROM form_responses WHERE formId = :formId") // Untuk dilihat guru
    suspend fun getAllResponsesForForm(formId: Int): List<FormResponseWithAnswers>

    @Transaction
    @Query("SELECT * FROM form_responses WHERE id = :responseId")
    suspend fun getResponseWithAnswersById(responseId: Int): FormResponseWithAnswers?
}
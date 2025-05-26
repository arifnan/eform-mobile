package com.example.eform.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.eform.data.model.QuestionEntity

@Dao
interface QuestionDao {
    @Insert
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("SELECT * FROM questions WHERE formId = :formId")
    suspend fun getQuestionsForForm(formId: Int): List<QuestionEntity>

}
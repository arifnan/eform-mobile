package com.example.eform.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.QuestionEntity

@Dao
interface FormDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForm(form: FormEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("SELECT * FROM forms ORDER BY createdAt DESC")
    suspend fun getAllForms(): List<FormEntity> // Ini tetap mengambil semua form

    @Query("SELECT * FROM questions WHERE formId = :formId")
    suspend fun getQuestionsForForm(formId: Int): List<QuestionEntity>

    @Transaction
    @Query("SELECT * FROM forms WHERE id = :formId")
    suspend fun getFormWithQuestions(formId: Int): FormEntity?

    @Query("SELECT * FROM forms WHERE formCode = :formCode LIMIT 1")
    suspend fun getFormByCode(formCode: String): FormEntity?

    // HAPUS FUNGSI LAMA INI:
    // @Query("UPDATE forms SET isFavorite = :isFavorite WHERE id = :formId")
    // suspend fun updateFavoriteStatus(formId: Int, isFavorite: Boolean)

    // @Query("SELECT * FROM forms WHERE isFavorite = 1 ORDER BY createdAt DESC")
    // suspend fun getFavoriteForms(): List<FormEntity> // Akan digantikan dengan query baru yang melibatkan userId

    // Fungsi baru untuk mengambil formulir favorit berdasarkan userId (menggunakan subquery)
    @Query("""
        SELECT * FROM forms 
        WHERE id IN (SELECT formId FROM user_favorite_forms WHERE userId = :userId)
        ORDER BY createdAt DESC
    """)
    suspend fun getFavoriteFormsByUserId(userId: Int): List<FormEntity>


    @Update
    suspend fun updateForm(form: FormEntity)
}
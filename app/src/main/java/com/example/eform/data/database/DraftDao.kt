package com.example.eform.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.eform.data.model.DraftAnswerEntity
import com.example.eform.data.model.FormDraftEntity
import com.example.eform.data.model.FormDraftWithAnswers

@Dao
interface DraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormDraft(formDraft: FormDraftEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraftAnswer(draftAnswer: DraftAnswerEntity)

    @Transaction
    @Query("SELECT * FROM form_drafts WHERE formId = :formId AND userIdentifier = :userIdentifier LIMIT 1")
    suspend fun getFormDraftWithAnswers(formId: Int, userIdentifier: String): FormDraftWithAnswers?

    @Query("DELETE FROM form_drafts WHERE formId = :formId AND userIdentifier = :userIdentifier")
    suspend fun deleteFormDraft(formId: Int, userIdentifier: String)

    @Query("DELETE FROM draft_answers WHERE formDraftId = :formDraftId")
    suspend fun deleteDraftAnswersByFormDraftId(formDraftId: Long)

    @Transaction
    suspend fun saveDraft(formDraft: FormDraftEntity, answers: List<DraftAnswerEntity>) {
        // Hapus draft lama jika ada
        val existingDraft = getFormDraftWithAnswers(formDraft.formId, formDraft.userIdentifier)
        existingDraft?.let {
            deleteDraftAnswersByFormDraftId(it.formDraft.id)
            deleteFormDraft(it.formDraft.formId, it.formDraft.userIdentifier)
        }
        val formDraftId = insertFormDraft(formDraft)
        answers.forEach {
            insertDraftAnswer(it.copy(formDraftId = formDraftId))
        }
    }

    @Query("SELECT * FROM form_drafts WHERE userIdentifier = :userIdentifier ORDER BY lastSaved DESC")
    suspend fun getAllDraftsForUser(userIdentifier: String): List<FormDraftEntity>
}
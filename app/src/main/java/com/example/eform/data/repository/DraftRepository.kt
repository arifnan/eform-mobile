package com.example.eform.data.repository

import com.example.eform.data.database.DraftDao
import com.example.eform.data.model.DraftAnswerEntity
import com.example.eform.data.model.FormDraftEntity
import com.example.eform.data.model.FormDraftWithAnswers
import java.util.Date

class DraftRepository(private val draftDao: DraftDao) {

    suspend fun saveFormDraft(formDraft: FormDraftEntity, answers: List<DraftAnswerEntity>) {
        draftDao.saveDraft(formDraft, answers)
    }

    suspend fun getDraft(formId: Int, userIdentifier: String): FormDraftWithAnswers? {
        return draftDao.getFormDraftWithAnswers(formId, userIdentifier)
    }

    suspend fun deleteDraft(formId: Int, userIdentifier: String) {
        val existingDraft = draftDao.getFormDraftWithAnswers(formId, userIdentifier)
        existingDraft?.let {
            draftDao.deleteDraftAnswersByFormDraftId(it.formDraft.id)
            draftDao.deleteFormDraft(it.formDraft.formId, it.formDraft.userIdentifier)
        }
    }

    suspend fun getAllDraftsForUser(userIdentifier: String): List<FormDraftEntity> {
        return draftDao.getAllDraftsForUser(userIdentifier)
    }
}
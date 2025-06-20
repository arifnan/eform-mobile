package com.example.eform.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class FormDraftWithAnswers(
    @Embedded val formDraft: FormDraftEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "formDraftId"
    )
    val answers: List<DraftAnswerEntity>
)
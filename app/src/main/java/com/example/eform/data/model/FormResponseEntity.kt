package com.example.eform.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "form_responses",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FormEntity::class,
            parentColumns = ["id"],
            childColumns = ["formId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FormResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val formId: Int,
    val studentId: Int, // ID dari UserEntity siswa
    val submissionTimestamp: Long = System.currentTimeMillis(),
    val photoUri: String?, // Path atau URI ke foto bukti
    val latitude: Double?,
    val longitude: Double?,
    val isLocationValid: Boolean // Apakah lokasi saat submit valid
)
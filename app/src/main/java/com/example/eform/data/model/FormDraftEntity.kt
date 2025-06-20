package com.example.eform.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "form_drafts")
data class FormDraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val formId: Int, // ID formulir dari API
    val userIdentifier: String, // Email atau NIP pengguna yang mengisi draft
    val formTitle: String, // Judul formulir (untuk tampilan mudah)
    val lastSaved: Date, // Timestamp kapan draft terakhir disimpan
    val photoPath: String? // Path lokal untuk foto yang diambil, jika ada
)
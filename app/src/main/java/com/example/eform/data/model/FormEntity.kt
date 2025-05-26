package com.example.eform.data.model
//untuk local
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forms")
data class FormEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val formCode: String, // Tambahkan properti ini
//    val isFavorite: Boolean = false
)

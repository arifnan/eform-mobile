package com.example.eform.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val nip: String,
    val password: String, // Sebaiknya tidak diedit langsung dari profil oleh pengguna
    val role: String,
    val address: String? = null // Tambahkan kolom alamat, bisa null
)

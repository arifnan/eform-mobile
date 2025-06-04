package com.example.eform.data.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val nip: String?, // Tambahkan NIP
    val password: String,
    val password_confirmation: String,
    val role: String, // Tambahkan role
    val gender: Boolean?,
    val grade: String?,// <<< TAMBAHKAN INI
    val subject: String?  // <<< TAMBAHKAN INI (Mungkin hanya untuk guru?)
 )
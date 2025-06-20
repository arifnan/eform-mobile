package com.example.eform.data.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val nip: String?, // Add NIP
    val password: String,
    val password_confirmation: String,
    val role: String,
    val gender: Boolean? = null, // Made nullable as it might not be required for all roles or registration types
    val grade: String? = null,   // Made nullable, only for students
    val subject: String? = null  // Made nullable, only for teachers
)
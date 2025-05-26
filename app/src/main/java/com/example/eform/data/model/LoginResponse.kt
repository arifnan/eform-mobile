package com.example.eform.data.model

data class LoginResponse(
    val status: Boolean,
    val message: String,
    val token: String?, // jika kamu mengembalikan token dari Laravel
    val user: User?     // opsional
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)
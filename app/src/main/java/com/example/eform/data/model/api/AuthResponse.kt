package com.example.eform.data.model.api

// Untuk respons login dan register yang mengembalikan token dan user
data class AuthResponse(
    val token: String,
    val user: UserApiModel
)
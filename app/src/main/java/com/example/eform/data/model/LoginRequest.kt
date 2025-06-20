package com.example.eform.data.model

data class LoginRequest(
    val email: String,
    val password: String,
    val role: String?
)

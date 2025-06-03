package com.example.eform.data.model.api

data class LoginRequestTeacher(
    val identifier: String, // Akan berisi NIP atau Email
    val password: String
)
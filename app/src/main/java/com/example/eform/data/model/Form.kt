package com.example.eform.data.model

data class Form(
    val id: Int,
    val title: String,
    val description: String?,
    val teacher_id: Int,
    val createdAt: String,  // atau bisa pakai LocalDateTime
    val teacher: Teacher
)
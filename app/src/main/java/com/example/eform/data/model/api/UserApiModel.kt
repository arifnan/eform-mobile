package com.example.eform.data.model.api

import com.google.gson.annotations.SerializedName

data class UserApiModel(
    val id: Int,
    val name: String?,
    val email: String?,
    val nip: String?,
    val role: String?,
    @SerializedName("email_verified_at")
    val emailVerifiedAt: String?,
    @SerializedName("profile_photo_url") // Sesuai dengan UserResource Anda (jika getProfilePhotoUrlAttribute ada)
    val profilePhotoUrl: String?,
    val address: String?, // Pastikan UserResource Anda juga menyertakan ini
    val subject: String?,
    val grade: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)
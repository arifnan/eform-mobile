package com.example.eform.data.model.api

import com.google.gson.annotations.SerializedName

data class FormApiModel(
    val id: Int,
    val title: String,
    val description: String?,
    @SerializedName("form_code")
    val formCode: String,
    @SerializedName("teacher_id")
    val teacherId: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val questions: List<QuestionApiModel>?, // FormResource Anda menyertakan questions with 'whenLoaded'
    val teacher: UserApiModel?, // FormResource Anda menyertakan teacher with 'whenLoaded'
    @SerializedName("location_required") val locationRequired: Boolean? = false,
    @SerializedName("photo_required") val photoRequired: Boolean? = false,
    @SerializedName("location_latitude") val latitude: String? = null,
    @SerializedName("location_longitude") val longitude: String? = null,
    @SerializedName("location_radius") val locationRadius: String? = null
)

// Untuk respons FormController@store
data class FormCreationResponseApi(
    val message: String,
    val form: FormApiModel
)
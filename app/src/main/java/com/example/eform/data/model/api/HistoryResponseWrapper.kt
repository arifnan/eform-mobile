package com.example.eform.data.model.api

import com.google.gson.annotations.SerializedName

// Wrapper ini bisa menerima respons object {"message": "...", "data": [...]}
data class HistoryResponseWrapper(
    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: List<FormResponseApiModel>
)
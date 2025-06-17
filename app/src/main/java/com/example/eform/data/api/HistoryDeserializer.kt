package com.example.eform.data.api

import com.example.eform.data.model.api.FormResponseApiModel
import com.example.eform.data.model.api.HistoryResponseWrapper
import com.google.gson.*
import java.lang.reflect.Type

// Kelas ini akan menangani logika deserialisasi yang kompleks
class HistoryDeserializer : JsonDeserializer<HistoryResponseWrapper> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): HistoryResponseWrapper {
        // Cek apakah JSON yang datang adalah sebuah Array
        if (json.isJsonArray) {
            // Jika ya, langsung parse sebagai List<FormResponseApiModel>
            val responseList: List<FormResponseApiModel> = context.deserialize(json, object : com.google.gson.reflect.TypeToken<List<FormResponseApiModel>>() {}.type)
            // Bungkus list tersebut ke dalam wrapper kita
            return HistoryResponseWrapper(message = "Data found", data = responseList)
        }
        // Jika bukan array, berarti itu adalah object {"message": ..., "data": [...]}
        // Biarkan Gson mem-parsingnya seperti biasa
        return Gson().fromJson(json, HistoryResponseWrapper::class.java)
    }
}
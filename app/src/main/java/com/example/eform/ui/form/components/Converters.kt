package com.example.eform.ui.form.components

import androidx.room.TypeConverter
import com.example.eform.data.model.QuestionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    // Konversi QuestionType ke String
    @TypeConverter
    fun fromQuestionList(questionList: List<QuestionEntity>): String {
        val gson = Gson()
        return gson.toJson(questionList)
    }


    // Konversi String ke QuestionType
    @TypeConverter
    fun toQuestionList(questionString: String): List<QuestionEntity> {
        val gson = Gson()
        val listType = object : TypeToken<List<QuestionEntity>>() {}.type
        return gson.fromJson(questionString, listType)
    }

    // Mengonversi List<String> menjadi String (JSON format)
    @TypeConverter
    fun fromOptions(options: List<String>): String {
        val gson = Gson()
        return gson.toJson(options)
    }

    // Mengonversi String (JSON format) kembali menjadi List<String>
    @TypeConverter
    fun toOptions(optionsString: String): List<String> {
        val gson = Gson()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(optionsString, listType)
    }

}

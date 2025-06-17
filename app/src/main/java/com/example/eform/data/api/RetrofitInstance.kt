package com.example.eform.data.api

import android.content.Context
import com.example.eform.data.local.UserPreferences // Pastikan path ini benar
import com.example.eform.data.model.api.HistoryResponseWrapper
import com.example.eform.utils.Constants
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private var applicationContext: Context? = null
    private lateinit var userPreferences: UserPreferences // Akan diinisialisasi di initialize

    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
            userPreferences = UserPreferences(applicationContext!!)
        }
    }

    private val authInterceptor = Interceptor { chain ->
        // Pastikan UserPreferences sudah diinisialisasi
        if (!::userPreferences.isInitialized) {
            throw IllegalStateException("UserPreferences not initialized. Call RetrofitInstance.initialize(context) first.")
        }

        val token = runBlocking { userPreferences.getToken.firstOrNull() }
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json") // Selalu kirim header Accept

        token?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }
    private val gson = GsonBuilder()
        .registerTypeAdapter(HistoryResponseWrapper::class.java, HistoryDeserializer())
        .create()

    val api: ApiService by lazy {
        if (applicationContext == null) {
            throw IllegalStateException("RetrofitInstance must be initialized with Context before accessing api.")
        }
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL) // BASE_URL = "https://e-form.ilta-services.tech/api/"
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

}
package com.example.eform.data.repository

import com.example.eform.data.api.ApiService
import com.example.eform.data.model.api.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

class FormRepository(private val apiService: ApiService) {

    suspend fun getTeacherForms(): Result<List<FormApiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTeacherForms()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(IOException("Get Forms API Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Get Forms HTTP Error: ${e.code()} - ${e.message()}", e))
            } catch (e: IOException) {
                Result.failure(IOException("Network error getting forms: ${e.message}", e))
            } catch (e: Exception) {
                Result.failure(IOException("Unknown error getting forms: ${e.message}", e))
            }
        }
    }

    suspend fun createForm(createFormRequest: ApiService.CreateFormRequestApi): Result<FormApiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createForm(createFormRequest)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg =
                        response.errorBody()?.string() ?: response.message() ?: "Create Form failed"
                    Result.failure(IOException("Create Form API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: HttpException) {
                Result.failure(
                    IOException(
                        "Create Form HTTP Error: ${e.code()} - ${e.message()}",
                        e
                    )
                )
            } catch (e: IOException) {
                Result.failure(IOException("Network error creating form: ${e.message}", e))
            } catch (e: Exception) {
                Result.failure(IOException("Unknown error creating form: ${e.message}", e))
            }
        }
    }

    suspend fun getFormDetails(formId: Int): Result<FormApiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFormDetails(formId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(IOException("Get Form Details API Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: HttpException) {
                Result.failure(
                    IOException(
                        "Get Form Details HTTP Error: ${e.code()} - ${e.message()}",
                        e
                    )
                )
            } catch (e: IOException) {
                Result.failure(IOException("Network error getting form details: ${e.message}", e))
            } catch (e: Exception) {
                Result.failure(IOException("Unknown error getting form details: ${e.message}", e))
            }
        }
    }

    suspend fun submitFormResponse(
        formId: Int,
        answers: List<AnswerPayload>,
        photoFile: File?,
        latitude: Double?,
        longitude: Double?
    ): Result<FormResponseApiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val formIdRb = formId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val answersJson = Gson().toJson(answers)
                val answersRb = answersJson.toRequestBody("application/json".toMediaTypeOrNull())
                val latitudeRb = latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val longitudeRb = longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                var photoPart: MultipartBody.Part? = null
                photoFile?.let {
                    val photoFileRb = it.asRequestBody("image/*".toMediaTypeOrNull())
                    photoPart = MultipartBody.Part.createFormData("photo", it.name, photoFileRb)
                }

                val response = apiService.submitFormResponse(formIdRb, latitudeRb, longitudeRb, answersRb, photoPart)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message() ?: "Submit Response failed"
                    Result.failure(IOException("Submit Response API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Submit Response HTTP Error: ${e.code()} - ${e.message()}", e))
            } catch (e: IOException) {
                Result.failure(IOException("Network error submitting response: ${e.message}", e))
            } catch (e: Exception) {
                Result.failure(IOException("Unknown error submitting response: ${e.message}", e))
            }
        }
    }

    suspend fun getResponsesForForm(formId: Int): Result<List<FormResponseApiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getResponsesForForm(formId)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal mengambil daftar respons"
                    Result.failure(IOException("API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: Exception) {
                Result.failure(IOException("Network error: ${e.message}", e))
            }
        }
    }

    suspend fun getFormByCode(formCode: String): Result<FormApiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFormByCode(formCode)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message() ?: "Formulir dengan kode '$formCode' tidak ditemukan"
                    Result.failure(IOException("Get Form By Code API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Get Form By Code HTTP Error: ${e.code()} - ${e.message()}", e))
            } catch (e: IOException) {
                Result.failure(IOException("Network error getting form by code: ${e.message}", e))
            } catch (e: Exception) {
                Result.failure(IOException("Unknown error getting form by code: ${e.message}", e))
            }
        }
    }

    suspend fun getTeacherFormsHistory(): Result<List<FormApiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTeacherFormsHistory()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(IOException("Get Teacher History API Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Get Teacher History HTTP Error: ${e.code()} - ${e.message()}", e))
            } catch (e: IOException) {
                Result.failure(IOException("Network error getting teacher history: ${e.message}", e))
            } catch (e: Exception) {
                Result.failure(IOException("Unknown error getting teacher history: ${e.message}", e))
            }
        }
    }

    suspend fun getStudentResponsesHistory(): Result<List<FormResponseApiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStudentSubmittedResponsesHistory()
                if (response.isSuccessful) {
                    // Ambil 'data' dari dalam wrapper. Jika body null, kembalikan list kosong.
                    val historyList = response.body()?.data ?: emptyList()
                    Result.success(historyList)
                } else {
                    Result.failure(IOException("Get Student History API Error: ${response.code()} - ${response.message()}"))
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Get Student History HTTP Error: ${e.code()} - ${e.message()}", e))
            } catch (e: IOException) {
                Result.failure(IOException("Network error getting student history: ${e.message}", e))
            } catch (e: Exception) {
                // Tangkap error parsing Gson di sini jika masih terjadi
                Result.failure(IOException("Unknown error getting student history: ${e.message}", e))
            }
        }
    }


    suspend fun getResponseDetail(responseId: Int): Result<FormResponseApiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getResponseDetail(responseId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal mengambil detail respons"
                    Result.failure(IOException("API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: Exception) {
                Result.failure(IOException("Network error or exception: ${e.message}", e))
            }
        }
    }

    suspend fun getFavoriteForms(): Result<List<FormApiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFavoriteForms()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(IOException("API Error: ${response.code()} - Gagal memuat favorit"))
                }
            } catch (e: Exception) {
                Result.failure(IOException("Network error: ${e.message}", e))
            }
        }
    }

    suspend fun addFavorite(formId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.addFavorite(formId)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("API Error: ${response.code()} - Gagal menambah favorit"))
                }
            } catch (e: Exception) {
                Result.failure(IOException("Network error: ${e.message}", e))
            }
        }
    }

    suspend fun removeFavorite(formId: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.removeFavorite(formId)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("API Error: ${response.code()} - Gagal menghapus favorit"))
                }
            } catch (e: Exception) {
                Result.failure(IOException("Network error: ${e.message}", e))
            }
        }
    }
}
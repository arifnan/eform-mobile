package com.example.eform.data.repository

import com.example.eform.data.api.ApiService
import com.example.eform.data.local.UserPreferences
import com.example.eform.data.model.LoginRequest
import com.example.eform.data.model.RegisterRequest
import com.example.eform.data.model.api.AuthResponse
import com.example.eform.data.model.api.ErrorResponse
import com.example.eform.data.model.api.UserApiModel
import com.google.gson.Gson // <-- IMPORT Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException

class AuthRepository(
    private val apiService: ApiService,
    private val userPreferences: UserPreferences
) {

    // Fungsi helper untuk parsing error body
    private fun parseErrorResponse(errorBodyString: String?): String {
        if (errorBodyString == null) return "Terjadi kesalahan yang tidak diketahui."
        return try {
            val gson = Gson()
            val errorResponse = gson.fromJson(errorBodyString, ErrorResponse::class.java)
            errorResponse.getFirstErrorMessage()
        } catch (e: Exception) {
            // Jika parsing gagal, coba kembalikan string error body mentah (sebagai fallback)
            // atau pesan default yang lebih baik
            errorBodyString // atau "Gagal memproses pesan error dari server."
        }
    }

    suspend fun login(loginRequest: LoginRequest): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.loginUser(loginRequest)
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.token.let { token ->
                        userPreferences.saveToken(token)
                    }
                    Result.success(response.body()!!)
                } else {
                    val errorMsgJson = response.errorBody()?.string()
                    val readableErrorMsg = parseErrorResponse(errorMsgJson) // <-- Gunakan parser
                    Result.failure(IOException(readableErrorMsg)) // <-- Hanya pesan yang sudah diparsing
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Kesalahan jaringan (HTTP ${e.code()}): ${e.message()}", e))
            }
            catch (e: IOException) {
                Result.failure(IOException("Tidak dapat terhubung ke server. Periksa koneksi internet Anda.", e))
            }
            catch (e: Exception) {
                Result.failure(IOException("Terjadi kesalahan: ${e.message}", e))
            }
        }
    }

    suspend fun register(registerRequest: RegisterRequest): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<AuthResponse> = if (registerRequest.role.equals("teacher", ignoreCase = true)) {
                    apiService.registerTeacher(registerRequest)
                } else if (registerRequest.role.equals("student", ignoreCase = true)) {
                    apiService.registerStudent(registerRequest)
                } else {
                    return@withContext Result.failure(IllegalArgumentException("Peran pengguna tidak valid untuk registrasi: ${registerRequest.role}"))
                }

                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.token.let { token ->
                        userPreferences.saveToken(token)
                    }
                    Result.success(response.body()!!)
                } else {
                    val errorMsgJson = response.errorBody()?.string()
                    val readableErrorMsg = parseErrorResponse(errorMsgJson) // <-- Gunakan parser
                    Result.failure(IOException(readableErrorMsg)) // <-- Hanya pesan yang sudah diparsing
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Kesalahan jaringan (HTTP ${e.code()}): ${e.message()}", e))
            }
            catch (e: IOException) {
                Result.failure(IOException("Tidak dapat terhubung ke server. Periksa koneksi internet Anda.", e))
            }
            catch (e: Exception) {
                Result.failure(IOException("Terjadi kesalahan: ${e.message}", e))
            }
        }
    }

    // ... (method logoutUser, getAuthenticatedUser, getAuthToken, updateUserProfile tetap sama)
    suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.logoutUser()
            } catch (e: Exception) {
                println("API logout error (ignored): ${e.message}")
            } finally {
                userPreferences.clearToken()
            }
            Result.success(Unit)
        }
    }

    suspend fun getAuthenticatedUser(): Result<UserApiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAuthenticatedUser()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsgJson = response.errorBody()?.string()
                    val readableErrorMsg = parseErrorResponse(errorMsgJson)
                    if (response.code() == 401) userPreferences.clearToken()
                    Result.failure(IOException(readableErrorMsg))
                }
            } catch (e: HttpException) { Result.failure(IOException("Kesalahan jaringan (HTTP ${e.code()}): ${e.message()}", e)) }
            catch (e: IOException) { Result.failure(IOException("Tidak dapat terhubung ke server. Periksa koneksi internet Anda.", e)) }
            catch (e: Exception) { Result.failure(IOException("Terjadi kesalahan: ${e.message}", e)) }
        }
    }

    suspend fun getAuthToken(): String? {
        return userPreferences.getToken.firstOrNull()
    }

    suspend fun updateUserProfile(
        name: String?,
        address: String?,
        profilePhotoFile: File?,
        grade: String? = null,
        subject: String? = null
    ): Result<UserApiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val nameRb = name?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                val addressRb = address?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                val gradeRb = grade?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                val subjectRb = subject?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())


                var photoPart: MultipartBody.Part? = null
                profilePhotoFile?.let {
                    val photoFileRb = it.asRequestBody("image/*".toMediaTypeOrNull())
                    photoPart = MultipartBody.Part.createFormData("profile_photo", it.name, photoFileRb)
                }

                val response = apiService.updateUserProfile(
                    name = nameRb,
                    address = addressRb,
                    profilePhoto = photoPart,
                    grade = gradeRb,
                    subject = subjectRb
                )

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsgJson = response.errorBody()?.string()
                    val readableErrorMsg = parseErrorResponse(errorMsgJson)
                    Result.failure(IOException(readableErrorMsg))
                }
            } catch (e: HttpException) { Result.failure(IOException("Kesalahan jaringan (HTTP ${e.code()}): ${e.message()}", e)) }
            catch (e: IOException) { Result.failure(IOException("Tidak dapat terhubung ke server. Periksa koneksi internet Anda.", e)) }
            catch (e: Exception) { Result.failure(IOException("Terjadi kesalahan: ${e.message}", e)) }
        }
    }
}
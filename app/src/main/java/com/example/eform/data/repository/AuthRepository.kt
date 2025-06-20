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

    // Helper function for parsing error body
    private fun parseErrorResponse(errorBodyString: String?): String {
        if (errorBodyString == null) return "An unknown error occurred."
        return try {
            val gson = Gson()
            val errorResponse = gson.fromJson(errorBodyString, ErrorResponse::class.java)
            errorResponse.getFirstErrorMessage()
        } catch (e: Exception) {
            // If parsing fails, try to return the raw error body string (as fallback)
            // or a better default message
            errorBodyString // or "Failed to process error message from server."
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
                    val readableErrorMsg = parseErrorResponse(errorMsgJson) // <-- Use parser
                    Result.failure(IOException(readableErrorMsg)) // <-- Only the parsed message
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Network error (HTTP ${e.code()}): ${e.message()}", e))
            }
            catch (e: IOException) {
                Result.failure(IOException("Cannot connect to server. Check your internet connection.", e))
            }
            catch (e: Exception) {
                Result.failure(IOException("An error occurred: ${e.message}", e))
            }
        }
    }

    suspend fun register(registerRequest: RegisterRequest): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Removed role-specific register calls. Assuming a single register endpoint now.
                val response: Response<AuthResponse> = apiService.registerUser(registerRequest)

                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.token.let { token ->
                        userPreferences.saveToken(token)
                    }
                    Result.success(response.body()!!)
                } else {
                    val errorMsgJson = response.errorBody()?.string()
                    val readableErrorMsg = parseErrorResponse(errorMsgJson) // <-- Use parser
                    Result.failure(IOException(readableErrorMsg)) // <-- Only the parsed message
                }
            } catch (e: HttpException) {
                Result.failure(IOException("Network error (HTTP ${e.code()}): ${e.message()}", e))
            }
            catch (e: IOException) {
                Result.failure(IOException("Cannot connect to server. Check your internet connection.", e))
            }
            catch (e: Exception) {
                Result.failure(IOException("An error occurred: ${e.message}", e))
            }
        }
    }

    // ... (method logoutUser, getAuthenticatedUser, getAuthToken, updateUserProfile remain the same)
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
            } catch (e: HttpException) { Result.failure(IOException("Network error (HTTP ${e.code()}): ${e.message()}", e)) }
            catch (e: IOException) { Result.failure(IOException("Cannot connect to server. Check your internet connection.", e)) }
            catch (e: Exception) { Result.failure(IOException("An error occurred: ${e.message}", e)) }
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
            } catch (e: HttpException) { Result.failure(IOException("Network error (HTTP ${e.code()}): ${e.message()}", e)) }
            catch (e: IOException) { Result.failure(IOException("Cannot connect to server. Check your internet connection.", e)) }
            catch (e: Exception) { Result.failure(IOException("An error occurred: ${e.message}", e)) }
        }
    }
}
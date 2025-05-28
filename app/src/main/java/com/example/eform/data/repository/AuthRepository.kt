package com.example.eform.data.repository

import com.example.eform.data.api.ApiService
import com.example.eform.data.local.UserPreferences
import com.example.eform.data.model.LoginRequest
import com.example.eform.data.model.RegisterRequest
import com.example.eform.data.model.api.AuthResponse
import com.example.eform.data.model.api.UserApiModel
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

    suspend fun login(loginRequest: LoginRequest): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.loginUser(loginRequest) // Panggil endpoint login yang benar
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.token.let { token ->
                        userPreferences.saveToken(token)
                    }
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message() ?: "Login gagal"
                    Result.failure(IOException("Login API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: HttpException) { Result.failure(IOException("Login HTTP Error: ${e.code()} - ${e.message()}", e)) }
            catch (e: IOException) { Result.failure(IOException("Network error during login: ${e.message}", e)) }
            catch (e: Exception) { Result.failure(IOException("Unknown error during login: ${e.message}", e)) }
        }
    }

    suspend fun register(registerRequest: RegisterRequest): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response: Response<AuthResponse> = if (registerRequest.role.equals("teacher", ignoreCase = true)) {
                    apiService.registerTeacher(registerRequest) // Panggil endpoint register guru
                } else if (registerRequest.role.equals("student", ignoreCase = true)) {
                    apiService.registerStudent(registerRequest) // Panggil endpoint register siswa
                } else {
                    return@withContext Result.failure(IllegalArgumentException("Peran pengguna tidak valid untuk registrasi: ${registerRequest.role}"))
                }

                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.token.let { token ->
                        userPreferences.saveToken(token)
                    }
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message() ?: "Registrasi gagal"
                    Result.failure(IOException("Register API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: HttpException) { Result.failure(IOException("Register HTTP Error: ${e.code()} - ${e.message()}", e)) }
            catch (e: IOException) { Result.failure(IOException("Network error during registration: ${e.message}", e)) }
            catch (e: Exception) { Result.failure(IOException("Unknown error during registration: ${e.message}", e)) }
        }
    }

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
                    val errorMsg = response.errorBody()?.string() ?: response.message() ?: "Gagal mendapatkan data pengguna"
                    if (response.code() == 401) userPreferences.clearToken()
                    Result.failure(IOException("Get User API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: HttpException) { Result.failure(IOException("Get User HTTP Error: ${e.code()} - ${e.message()}", e)) }
            catch (e: IOException) { Result.failure(IOException("Network error getting user: ${e.message}", e)) }
            catch (e: Exception) { Result.failure(IOException("Unknown error getting user: ${e.message}", e)) }
        }
    }

    suspend fun getAuthToken(): String? {
        return userPreferences.getToken.firstOrNull()
    }

    suspend fun updateUserProfile(
        name: String?,
        address: String?,
        profilePhotoFile: File?
    ): Result<UserApiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val nameRb = name?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                val addressRb = address?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                // Jika menggunakan PUT di ApiService, _method tidak diperlukan di sini
                // val methodRb = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

                var photoPart: MultipartBody.Part? = null
                profilePhotoFile?.let {
                    val photoFileRb = it.asRequestBody("image/*".toMediaTypeOrNull())
                    photoPart = MultipartBody.Part.createFormData("profile_photo", it.name, photoFileRb)
                }

                val response = apiService.updateUserProfile(
                    name = nameRb,
                    address = addressRb,
                    profilePhoto = photoPart
                    // method = methodRb // Hanya jika ApiService menggunakan @POST untuk meniru PUT
                )

                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = response.errorBody()?.string() ?: response.message() ?: "Gagal memperbarui profil"
                    Result.failure(IOException("Update Profile API Error: ${response.code()} - $errorMsg"))
                }
            } catch (e: HttpException) { Result.failure(IOException("Update Profile HTTP Error: ${e.code()} - ${e.message()}", e)) }
            catch (e: IOException) { Result.failure(IOException("Network error updating profile: ${e.message}", e)) }
            catch (e: Exception) { Result.failure(IOException("Unknown error updating profile: ${e.message}", e)) }
        }
    }
}
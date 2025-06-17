package com.example.eform.data.api

import com.example.eform.data.model.api.*
import com.example.eform.data.model.LoginRequest
import com.example.eform.data.model.RegisterRequest
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ================= AUTHENTICATION =================
    // Rute dari file api.php Anda
    @POST("register/teacher")
    suspend fun registerTeacher(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    @POST("register/student")
    suspend fun registerStudent(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    // ASUMSI: Anda akan menambahkan endpoint ini di Laravel routes/api.php
    // Misal: Route::post('/login', [App\Http\Controllers\Api\AuthController::class, 'login']);
    @POST("login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<AuthResponse>

    // ASUMSI: Anda akan menambahkan endpoint ini di Laravel routes/api.php (dalam auth:sanctum)
    // Misal: Route::post('/logout', [App\Http\Controllers\Api\AuthController::class, 'logout']);
    @POST("logout")
    suspend fun logoutUser(): Response<Unit>

    // ASUMSI: Anda akan menambahkan endpoint ini di Laravel routes/api.php (dalam auth:sanctum)
    // Misal: Route::get('/user', function (Request $request) { return new UserResource($request->user()); });
    // atau Route::get('/user', [App\Http\Controllers\Api\AuthController::class, 'user']);
    @GET("user")
    suspend fun getAuthenticatedUser(): Response<UserApiModel>

    // Rute dari UserController.php (untuk update profil)
    // Di api.php Anda: Route::put('/profile', [UserController::class, 'updateProfile']);
    @Multipart
    @POST("profile") // <-- UBAH DARI @PUT MENJADI @POST
    suspend fun updateUserProfile(
        @Part("name") name: RequestBody?,
        @Part("address") address: RequestBody?,
        @Part profilePhoto: MultipartBody.Part?,
        @Part("grade") grade: RequestBody?,
        @Part("subject") subject: RequestBody?,
    ): Response<UserApiModel>


    // ================= FORMS (Guru) =================
    // Rute dari FormController.php
    @GET("forms") // GET /forms (untuk guru yang login) -> FormController@apiIndex
    suspend fun getTeacherForms(): Response<List<FormApiModel>>

    // Payload untuk membuat pertanyaan
    data class CreateQuestionPayloadApi(
        @SerializedName("question_text")
        val questionText: String,
        @SerializedName("question_type")
        val questionType: String, // "Text", "MultipleChoice", "Checkbox", "LinearScale"
        val options: List<String>?,
        val required: Boolean
    )
    // Payload untuk membuat form
    data class CreateFormRequestApi(
        val title: String,
        val description: String?,
        val questions: List<CreateQuestionPayloadApi>,
        @SerializedName("teacher_id")
        val teacherId: Int
    )



    @POST("forms") // POST /forms -> FormController@apiStore
    suspend fun createForm(@Body createFormRequest: CreateFormRequestApi): Response<FormApiModel> // FormController@apiStore mengembalikan FormResource

    // Untuk FormController@show (GET /forms/{form})
    @GET("forms/{id}") // Menggunakan {id} sebagai placeholder path, Laravel akan handle binding ke {form}
    suspend fun getFormDetails(@Path("id") formId: Int): Response<FormApiModel>

    @GET("forms/code/{form_code}") // GET /forms/code/{form_code} -> FormController@apiGetByFormCode (Publik atau terautentikasi)
    suspend fun getFormByCode(@Path("form_code") formCode: String): Response<FormApiModel>

    // Request body untuk update mirip dengan create
    @PUT("forms/{id}") // PUT /forms/{form} -> FormController@apiUpdate (menggunakan {id} sebagai path)
    suspend fun updateForm(@Path("id") formId: Int, @Body updateFormRequest: CreateFormRequestApi): Response<FormApiModel>

    @DELETE("forms/{id}") // DELETE /forms/{form} -> FormController@apiDestroy (menggunakan {id} sebagai path)
    suspend fun deleteForm(@Path("id") formId: Int): Response<Unit>


    // ================= FORM RESPONSES =================
    // Rute dari ResponseController.php
    @Multipart
    @POST("responses") // POST /responses (untuk siswa yang login)
    suspend fun submitFormResponse(
        @Part("form_id") formId: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part("answers") answersJson: RequestBody, // JSON String dari List<AnswerPayload>
        @Part photo: MultipartBody.Part?
    ): Response<FormResponseApiModel>




    // Rute dari ResponseController@indexByForm: GET /forms/{form}/responses
    @GET("forms/{form_id}/responses")
    suspend fun getResponsesForForm(@Path("form_id") formId: Int): Response<List<FormResponseApiModel>>

    @GET("responses/{id}")
    suspend fun getResponseDetail(@Path("id") responseId: Int): Response<FormResponseApiModel>

    // ================= HISTORY =================
    // Rute dari TeacherController.php
    @GET("teacher/forms/history") // GET /teacher/forms/history -> apiGetFormHistory
    suspend fun getTeacherFormsHistory(): Response<List<FormApiModel>>

    @GET("student/responses/history")
    suspend fun getStudentSubmittedResponsesHistory(): Response<HistoryResponseWrapper>


    // --- ENDPOINT UNTUK FAVORIT ---
    // ================= FAVORITES =================
    @GET("favorites")
    suspend fun getFavoriteForms(): Response<List<FormApiModel>>

    @POST("forms/{id}/favorite")
    suspend fun addFavorite(@Path("id") formId: Int): Response<Unit>

    // ========== PERBAIKAN URL DI SINI ==========
    @DELETE("forms/{id}/favorite")
    suspend fun removeFavorite(@Path("id") formId: Int): Response<Unit>
}
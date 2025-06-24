package com.example.eform.data.api

import com.example.eform.data.model.api.*
import com.example.eform.data.model.LoginRequest
import com.example.eform.data.model.RegisterRequest
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ================= AUTHENTICATION =================
    // Removed role-specific register endpoints as per request
    @POST("register") // Unified registration endpoint
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<AuthResponse>

    // ASSUMPTION: You will add this endpoint in Laravel routes/api.php
    // Example: Route::post('/login', [App\Http\Controllers\Api\AuthController::class, 'login']);
    @POST("login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<AuthResponse>

    // ASSUMPTION: You will add this endpoint in Laravel routes/api.php (within auth:sanctum)
    // Example: Route::post('/logout', [App\Http\Controllers\Api\AuthController::class, 'logout']);
    @POST("logout")
    suspend fun logoutUser(): Response<Unit>

    // ASSUMPTION: You will add this endpoint in Laravel routes/api.php (within auth:sanctum)
    // Example: Route::get('/user', function (Request $request) { return new UserResource($request->user()); });
    // or Route::get('/user', [App\Http\Controllers\Api\AuthController::class, 'user']);
    @GET("user")
    suspend fun getAuthenticatedUser(): Response<UserApiModel>

    // Route from UserController.php (for profile update)
    // In your api.php: Route::post('/profile', [UserController::class, 'updateProfile']);
    @Multipart
    @POST("profile") // <-- CHANGED FROM @PUT TO @POST for Laravel
    suspend fun updateUserProfile(
        @Part("name") name: RequestBody?,
        @Part("address") address: RequestBody?,
        @Part profilePhoto: MultipartBody.Part?,
        @Part("grade") grade: RequestBody?,
        @Part("subject") subject: RequestBody?,
    ): Response<UserApiModel>


    // ================= FORMS (Teacher) =================
    // Route from FormController.php
    @GET("forms") // GET /forms (for logged-in teacher) -> FormController@apiIndex
    suspend fun getTeacherForms(): Response<List<FormApiModel>>

    // Payload for creating questions
    data class CreateQuestionPayloadApi(
        @SerializedName("question_text")
        val questionText: String,
        @SerializedName("question_type")
        val questionType: String, // "Text", "MultipleChoice", "Checkbox", "LinearScale"
        val options: List<String>?,
        val required: Boolean
    )
    // Payload for creating form
    data class CreateFormRequestApi(
        val title: String,
        val description: String?,
        val questions: List<CreateQuestionPayloadApi>,
        @SerializedName("teacher_id")
        val teacherId: Int
    )



    @POST("forms") // POST /forms -> FormController@apiStore
    suspend fun createForm(@Body createFormRequest: CreateFormRequestApi): Response<FormApiModel> // FormController@apiStore returns FormResource

    // For FormController@show (GET /forms/{form})
    @GET("forms/{id}") // Using {id} as path placeholder, Laravel will handle binding to {form}
    suspend fun getFormDetails(@Path("id") formId: Int): Response<FormApiModel>

    @GET("forms/code/{form_code}") // GET /forms/code/{form_code} -> FormController@apiGetByFormCode (Public or authenticated)
    suspend fun getFormByCode(@Path("form_code") formCode: String): Response<FormApiModel>

    // Request body for update similar to create
    @PUT("forms/{id}") // PUT /forms/{form} -> FormController@apiUpdate (using {id} as path)
    suspend fun updateForm(@Path("id") formId: Int, @Body updateFormRequest: CreateFormRequestApi): Response<FormApiModel>

    @DELETE("forms/{id}") // DELETE /forms/{form} -> FormController@apiDestroy (using {id} as path)
    suspend fun deleteForm(@Path("id") formId: Int): Response<Unit>


    // ================= FORM RESPONSES =================
    // Route from ResponseController.php
    @Multipart
    @POST("responses") // POST /responses (for logged-in student)
    suspend fun submitFormResponse(
        @Part("form_id") formId: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part("answers") answersJson: RequestBody, // JSON String of List<AnswerPayload>
        @Part photo: MultipartBody.Part?
    ): Response<FormResponseApiModel>




    // Route from ResponseController@indexByForm: GET /forms/{form}/responses
    @GET("forms/{form_id}/responses")
    suspend fun getResponsesForForm(@Path("form_id") formId: Int): Response<List<FormResponseApiModel>>

    @GET("responses/{id}")
    suspend fun getResponseDetail(@Path("id") responseId: Int): Response<FormResponseApiModel>

    // ================= HISTORY =================
    // Route from TeacherController.php
    @GET("teacher/forms/history") // GET /teacher/forms/history -> apiGetFormHistory
    suspend fun getTeacherFormsHistory(): Response<List<FormApiModel>>

    @GET("student/responses/history")
    suspend fun getStudentSubmittedResponsesHistory(): Response<HistoryResponseWrapper>


    // --- ENDPOINT FOR FAVORITES ---
    // ================= FAVORITES =================
    @GET("favorites")
    suspend fun getFavoriteForms(): Response<List<FormApiModel>>

    @POST("forms/{id}/favorite")
    suspend fun addFavorite(@Path("id") formId: Int): Response<Unit>

    // ========== FIX URL HERE ==========
    @DELETE("forms/{id}/favorite")
    suspend fun removeFavorite(@Path("id") formId: Int): Response<Unit>

    data class AnswerPayload(
        @SerializedName("question_id") val questionId: Int,
        @SerializedName("answer_text") val answerText: String?,
        @SerializedName("selected_options") val selectedOptions: String?,
        @SerializedName("linear_scale_value") val linearScaleValue: Int?
    )

    @GET("locations")
    suspend fun getLocations(): Response<List<LocationApiModel>>
}
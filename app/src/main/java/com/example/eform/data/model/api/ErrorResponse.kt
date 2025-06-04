package com.example.eform.data.model.api


data class ErrorResponse(
    val message: String?,
    val errors: Map<String, List<String>>?
) {
    // Fungsi helper untuk mendapatkan pesan error pertama yang lebih user-friendly
    fun getFirstErrorMessage(): String {
        if (errors != null && errors.isNotEmpty()) {
            for (fieldErrors in errors.values) {
                if (fieldErrors.isNotEmpty()) {
                    return fieldErrors[0] // Ambil pesan error pertama dari field pertama yang error
                }
            }
        }
        return message ?: "Terjadi kesalahan yang tidak diketahui." // Fallback ke message utama atau pesan default
    }
}
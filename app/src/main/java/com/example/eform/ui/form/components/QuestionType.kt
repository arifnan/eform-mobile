package com.example.eform.ui.form.components

enum class QuestionType(val label: String) {
    Text("Teks"),
    MultipleChoice("Pilihan Ganda"),
    Checkbox("Kotak Centang"), // Atau "Checkbox" jika lebih disukai
    LinearScale("Skala Linier"),
    true_false("Benar/Salah"),   // Ditambahkan
    file_upload("Unggah File");  // Ditambahkan

    companion object {
        // Helper untuk konversi dari String ke Enum, berguna saat parsing data dari API
        // atau saat menyimpan preferensi tipe pertanyaan.
        fun fromString(value: String?): QuestionType? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }

        // Helper untuk mendapatkan label dari nama enum (jika diperlukan di tempat lain)
        fun getLabelFor(value: String?): String? {
            return fromString(value)?.label
        }
    }
}
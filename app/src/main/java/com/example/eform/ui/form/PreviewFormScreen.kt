package com.example.eform.ui.form

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.QuestionEntity
import com.example.eform.ui.form.components.QuestionType

@Composable
fun PreviewFormScreen(formId: Int) {
    val context = LocalContext.current
    val questions = remember { mutableStateListOf<QuestionEntity>() }
    val formDao = AppDatabase.getDatabase(context).formDao()
    // Load questions dari database berdasarkan formId
    LaunchedEffect(formId) {
        questions.clear()
        questions.addAll(formDao.getQuestionsForForm(formId))
    }
    // Ambil detail formulir berdasarkan formId
    val formData = remember { mutableStateOf<FormEntity?>(null) }
    LaunchedEffect(formId) {
        formData.value = formDao.getFormWithQuestions(formId)
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        formData.value?.let { form ->
            // Tampilkan Judul dan Deskripsi
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = form.title, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = form.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Tampilkan pertanyaan formulir
            questions.forEach { question ->
                QuestionPreview(questionData = question)
            }
        } ?: run {
            Text("Loading...")
        }
    }
}

@Composable
fun QuestionPreview(questionData: QuestionEntity) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = questionData.questionText, style = MaterialTheme.typography.bodyMedium)

        when (questionData.questionType) {
            QuestionType.Text -> {
                Text("Jawaban: ${questionData.answer}") // Tampilkan jawaban teks
            }
            QuestionType.MultipleChoice -> {
                // Tampilkan pilihan ganda
                questionData.options.forEach { option ->
                    Text("Option: $option")
                }
            }
            QuestionType.Checkbox -> {
                // Tampilkan checkbox
                questionData.options.forEach { option ->
                    Text("Option: $option")
                }
            }
            QuestionType.LinearScale -> {
                // Tampilkan slider
                Text("Skala: ${questionData.answer}")
            }
        }
    }
}

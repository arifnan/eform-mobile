package com.example.eform.ui.form

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.QuestionEntity
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.components.QuestionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewFormScreen(
    navController: NavController, // <- TAMBAHKAN NavController sebagai parameter
    formId: Int
) {
    val context = LocalContext.current
    val formDao = AppDatabase.getDatabase(context).formDao()

    var formData by remember { mutableStateOf<FormEntity?>(null) }
    var questions by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(formId) {
        isLoading = true
        formData = formDao.getFormWithQuestions(formId)
        questions = formData?.let { formDao.getQuestionsForForm(it.id) } ?: emptyList()
        isLoading = false
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = formData?.title ?: "Pratinjau Formulir",
                navController = navController
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (formData == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Formulir tidak ditemukan.")
            }
        } else {
            val form = formData!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    // Card Judul & Deskripsi
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = form.title, style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = form.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Tombol untuk melihat respons
                    Button(
                        onClick = {
                            navController.navigate(
                                Screen.ResponseList.route
                                    .replace("{formId}", "$formId")
                                    .replace("{formTitle}", form.title)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lihat Jawaban Siswa")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Pertanyaan:", style = MaterialTheme.typography.titleMedium)
                }

                // Daftar Pertanyaan
                items(questions, key = { it.id }) { question ->
                    QuestionPreview(questionData = question)
                }
            }
        }
    }
}

@Composable
fun QuestionPreview(questionData: QuestionEntity) {
    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = questionData.questionText, style = MaterialTheme.typography.bodyMedium)

        when (questionData.questionType) {
            QuestionType.Text -> {
                // Untuk preview, kita mungkin tidak menampilkan jawaban, hanya tipe field
                OutlinedTextField(
                    value = questionData.answer.ifEmpty { "(Responden akan mengisi teks)" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Jawaban Teks")},
                    modifier = Modifier.fillMaxWidth()
                )
            }
            QuestionType.MultipleChoice -> {
                Text("Pilihan Ganda:", style = MaterialTheme.typography.labelMedium)
                questionData.options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = false, onClick = {}, enabled = false)
                        Text(option, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            QuestionType.Checkbox -> {
                Text("Pilihan Kotak Centang:", style = MaterialTheme.typography.labelMedium)
                questionData.options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = false, onCheckedChange = {}, enabled = false)
                        Text(option, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            QuestionType.LinearScale -> {
                val minVal = questionData.options.getOrNull(0) ?: "0"
                val maxVal = questionData.options.getOrNull(1) ?: "5"
                val minLabel = questionData.options.getOrNull(2)
                val maxLabel = questionData.options.getOrNull(3)
                Text("Skala Linier: $minVal (${minLabel ?: ""}) sampai $maxVal (${maxLabel ?: ""})", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = questionData.answer.toFloatOrNull() ?: minVal.toFloatOrNull() ?: 0f,
                    onValueChange = {},
                    valueRange = (minVal.toFloatOrNull() ?: 0f)..(maxVal.toFloatOrNull() ?: 5f),
                    enabled = false
                )
            }
            // Menambahkan case untuk TrueFalse dan FileUpload
            QuestionType.TrueFalse -> {
                Text("Pilihan: Benar / Salah", style = MaterialTheme.typography.labelMedium)
                // Bisa tampilkan radio button disabled untuk visualisasi
                Row {
                    RadioButton(selected = false, onClick = {}, enabled = false)
                    Text("Benar", modifier = Modifier.padding(start = 8.dp, end = 16.dp))
                    RadioButton(selected = false, onClick = {}, enabled = false)
                    Text("Salah", modifier = Modifier.padding(start = 8.dp))
                }
            }
            QuestionType.FileUpload -> {
                Text("Tipe Jawaban: Unggah File", style = MaterialTheme.typography.labelMedium)
                Button(onClick = {}, enabled = false) { Text("Pilih File (Preview)")}
            }
        }
        // Jika ada jawaban tersimpan (misal untuk preview hasil isian), bisa ditambahkan di sini
        if (questionData.answer.isNotBlank() && questionData.questionType !in listOf(QuestionType.MultipleChoice, QuestionType.Checkbox, QuestionType.LinearScale, QuestionType.FileUpload, QuestionType.TrueFalse)) {
            Text("Jawaban Tersimpan: ${questionData.answer}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        } else if (questionData.answer.isNotBlank() && questionData.questionType in listOf(QuestionType.MultipleChoice, QuestionType.Checkbox, QuestionType.TrueFalse)) {
            Text("Pilihan Tersimpan: ${questionData.answer}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        } else if (questionData.answer.isNotBlank() && questionData.questionType == QuestionType.FileUpload){
            Text("File Tersimpan: ${Uri.parse(questionData.answer).lastPathSegment ?: questionData.answer}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(10.dp)) // Jarak antar pertanyaan di preview
        Divider() // Pemisah antar pertanyaan di preview
    }
}


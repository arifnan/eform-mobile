package com.example.eform.ui.form

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eform.data.model.api.QuestionApiModel
import com.example.eform.navigation.Screen // Pastikan Screen diimpor
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.ui.viewmodel.PreviewFormViewModel
import com.example.eform.ui.viewmodel.PreviewUiState
import com.example.eform.utils.Constants // Import Constants untuk deep link

// Signature fungsi diubah untuk menerima NavController
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewFormScreen(
    navController: NavController,
    formId: Int,
    viewModel: PreviewFormViewModel = viewModel(
        factory = PreviewFormViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // Memuat data saat composable pertama kali ditampilkan
    LaunchedEffect(formId) {
        viewModel.loadFormDetails(formId)
    }

    Scaffold(
        topBar = {
            val title = (uiState as? PreviewUiState.Success)?.form?.title ?: "Pratinjau Formulir"
            StandardTopAppBar(title = title, navController = navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is PreviewUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is PreviewUiState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is PreviewUiState.Success -> {
                    val form = state.form
                    val context = LocalContext.current
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val accessLink = "${Constants.APP_DEEP_LINK_SCHEME}://${Constants.APP_DEEP_LINK_HOST}/${form.formCode}"

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Memberi jarak antar item
                    ) {
                        // Card untuk Detail Formulir (Judul & Deskripsi)
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = form.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    if (!form.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = form.description, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        // ==========================================================
                        // === BAGIAN YANG DITAMBAHKAN ===
                        // ==========================================================
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Baris untuk Kode Formulir
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Kode Formulir:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(form.formCode, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                        }
                                        IconButton(onClick = {
                                            val clip = ClipData.newPlainText("Form Code", form.formCode)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, "Kode Formulir disalin!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.ContentCopy, "Salin Kode")
                                        }
                                    }

                                    Divider()

                                    // Baris untuk Link Akses
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Link Akses:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(accessLink, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                        }
                                        IconButton(onClick = {
                                            val clip = ClipData.newPlainText("Access Link", accessLink)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, "Link Akses disalin!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.ContentCopy, "Salin Link")
                                        }
                                    }
                                }
                            }
                        }
                        // ==========================================================
                        // === AKHIR BAGIAN YANG DITAMBAHKAN ===
                        // ==========================================================

                        // Tombol Lihat Jawaban Siswa
                        item {
                            Button(
                                onClick = {
                                    // Anda perlu memastikan rute `Screen.ResponseList` sudah ada di file Screen.kt Anda
                                    // dan sudah di-handle di AppNavHost.kt
                                    // navController.navigate(
                                    //     Screen.ResponseList.route
                                    //         .replace("{formId}", "$formId")
                                    //         .replace("{formTitle}", form.title)
                                    // )
                                    Toast.makeText(context, "Navigasi ke Daftar Jawaban", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Lihat Jawaban Siswa")
                            }
                        }

                        // Header untuk daftar pertanyaan
                        item {
                            Text(
                                "Daftar Pertanyaan",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(form.questions ?: emptyList(), key = { it.id }) { question ->
                            QuestionPreviewApi(questionData = question)
                        }
                    }
                }
            }
        }
    }
}

// Composable QuestionPreviewApi tetap sama, tidak perlu diubah
@Composable
fun QuestionPreviewApi(questionData: QuestionApiModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = questionData.questionText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (questionData.required) {
                    Text(" *", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            val questionType = try {
                // Konversi string dari API ke enum QuestionType
                // Pastikan nama enum di Kotlin sama persis (case-sensitive) dengan string dari API
                QuestionType.valueOf(questionData.questionType)
            } catch (e: IllegalArgumentException) {
                null // Tipe tidak dikenal
            }

            // Tampilkan preview jawaban berdasarkan tipe
            when (questionType) {
                QuestionType.Text -> {
                    OutlinedTextField(
                        value = "(Responden akan mengisi teks di sini)",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                QuestionType.MultipleChoice, QuestionType.Checkbox -> {
                    val options = questionData.options ?: emptyList()
                    if (options.isEmpty()){
                        Text("Tidak ada opsi untuk pertanyaan ini.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        options.forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                if (questionType == QuestionType.MultipleChoice) RadioButton(selected = false, onClick = {}, enabled = false)
                                else Checkbox(checked = false, onCheckedChange = {}, enabled = false)
                                Text(option, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
                QuestionType.LinearScale -> {
                    // Di Laravel 'options' untuk LinearScale berisi [minVal, maxVal, minLabel, maxLabel]
                    val minVal = questionData.options?.getOrNull(0) ?: "0"
                    val maxVal = questionData.options?.getOrNull(1) ?: "5"
                    val minLabel = questionData.options?.getOrNull(2)
                    val maxLabel = questionData.options?.getOrNull(3)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (minLabel.isNullOrBlank()) minVal else minLabel, style = MaterialTheme.typography.bodySmall)
                            Text(if (maxLabel.isNullOrBlank()) maxVal else maxLabel, style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            value = minVal.toFloatOrNull() ?: 0f,
                            onValueChange = {},
                            valueRange = (minVal.toFloatOrNull() ?: 0f)..(maxVal.toFloatOrNull() ?: 5f),
                            enabled = false
                        )
                    }
                }
                else -> { // Termasuk jika questionType null
                    Text("Tipe pertanyaan tidak dapat ditampilkan: ${questionData.questionType}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
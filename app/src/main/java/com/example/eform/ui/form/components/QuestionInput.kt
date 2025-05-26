package com.example.eform.ui.form.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class) // Untuk TextFieldDefaults.colors
@Composable
fun QuestionInput(
    questionData: QuestionInputData,
    onDelete: () -> Unit,
    questionNumber: Int // Tambahkan nomor pertanyaan
) {
    var questionText by remember { mutableStateOf(questionData.questionText) }
    var questionType by remember { mutableStateOf(questionData.questionType) }
    var options by remember { mutableStateOf(questionData.options.toMutableList()) }
    var required by remember { mutableStateOf(questionData.required) }

    var minScale by remember { mutableStateOf(questionData.minScale) }
    var maxScale by remember { mutableStateOf(questionData.maxScale) }
    var minLabel by remember { mutableStateOf(questionData.minLabel) }
    var maxLabel by remember { mutableStateOf(questionData.maxLabel) }

    // Sync kembali ke model ketika state internal berubah
    LaunchedEffect(questionText, questionType, options, required, minScale, maxScale, minLabel, maxLabel) {
        questionData.questionText = questionText
        questionData.questionType = questionType
        questionData.options = options.filter { it.isNotBlank() }.toMutableList() // Simpan opsi yang tidak kosong saja
        questionData.required = required
        questionData.minScale = minScale
        questionData.maxScale = maxScale
        questionData.minLabel = minLabel
        questionData.maxLabel = maxLabel
    }

    Card( // Bungkus semua elemen pertanyaan dalam Card
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Beri jarak antar Card pertanyaan
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // Warna background card yang lembut
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp) // Padding di dalam Card
        ) {
            Text(
                text = "Pertanyaan ${questionNumber + 1}", // Tampilkan nomor pertanyaan
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Tulis Pertanyaan Anda") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    maxLines = 3
                )

                DropdownMenuQuestionType(
                    selectedType = questionType,
                    onTypeSelected = { newType ->
                        questionType = newType
                        // Reset options jika tipe berubah dari atau ke tipe yang tidak menggunakan options
                        if (newType != QuestionType.MultipleChoice && newType != QuestionType.Checkbox) {
                            options = mutableListOf()
                            questionData.options = mutableListOf()
                        } else if (options.isEmpty()) { // Jika berubah ke tipe dengan opsi dan opsi kosong, tambahkan satu default
                            options = mutableListOf("")
                            questionData.options = mutableListOf("")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Opsi jawaban berdasarkan tipe pertanyaan
            when (questionType) {
                QuestionType.MultipleChoice, QuestionType.Checkbox -> {
                    Column {
                        options.forEachIndexed { index, option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp) // Kurangi padding vertikal
                            ) {
                                if (questionType == QuestionType.MultipleChoice) {
                                    RadioButton(selected = false, onClick = null, enabled = false) // Hanya visual saat pembuatan
                                } else {
                                    Checkbox(checked = false, onCheckedChange = null, enabled = false) // Hanya visual saat pembuatan
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = option,
                                    onValueChange = {
                                        val updatedOptions = options.toMutableList()
                                        updatedOptions[index] = it
                                        options = updatedOptions
                                    },
                                    placeholder = { Text("Opsi ${index + 1}") },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors( // Gunakan TextFieldDefaults.colors
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                                )
                                if (options.size > 1) { // Tombol hapus opsi hanya jika lebih dari 1 opsi
                                    IconButton(onClick = {
                                        val updatedOptions = options.toMutableList()
                                        updatedOptions.removeAt(index)
                                        options = updatedOptions
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Opsi", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                        TextButton(onClick = {
                            options = options.toMutableList().apply { add("") }
                        }) {
                            Text("➕ Tambahkan Opsi")
                        }
                    }
                }

                QuestionType.LinearScale -> {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Skala dari ")
                            DropdownSelector(
                                value = minScale,
                                range = 0..1, // Skala Linier umumnya mulai dari 0 atau 1
                                onValueChange = { minScale = it }
                            )
                            Text(" sampai ")
                            DropdownSelector(
                                value = maxScale,
                                range = 2..10, // Maksimal skala umum
                                onValueChange = { maxScale = it }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minLabel,
                                onValueChange = { minLabel = it },
                                label = { Text("$minScale Label (opsional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = maxLabel,
                                onValueChange = { maxLabel = it },
                                label = { Text("$maxScale Label (opsional)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
                QuestionType.Text -> {
                    // Tidak ada input tambahan untuk tipe Text di sini, hanya pertanyaan
                    // Mungkin placeholder untuk jawaban singkat atau panjang
                    Text(
                        "Responden akan menjawab dalam format teks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pengaturan pertanyaan (wajib diisi, hapus)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End, // Pindahkan ke kanan
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Wajib diisi", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = required,
                        onCheckedChange = { required = it }
                    )
                }
                Spacer(modifier = Modifier.weight(1f)) // Dorong tombol hapus ke ujung
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus Pertanyaan", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// DropdownMenuQuestionType dan DropdownSelector tetap sama
@Composable
fun DropdownMenuQuestionType(
    selectedType: QuestionType,
    onTypeSelected: (QuestionType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedType.label)
            // Icon(Icons.Default.ArrowDropDown, contentDescription = "Pilih tipe pertanyaan")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            QuestionType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DropdownSelector(value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$value")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            range.forEach { v ->
                DropdownMenuItem(
                    text = { Text("$v") },
                    onClick = {
                        onValueChange(v)
                        expanded = false
                    }
                )
            }
        }
    }
}
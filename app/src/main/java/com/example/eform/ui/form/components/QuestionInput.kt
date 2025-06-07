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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionInput(
    questionData: QuestionInputData,
    onDelete: () -> Unit,
    questionNumber: Int
) {
    var questionText by remember { mutableStateOf(questionData.questionText) }
    var questionType by remember { mutableStateOf(questionData.questionType) }
    var options by remember { mutableStateOf(questionData.options.toMutableList()) }
    var required by remember { mutableStateOf(questionData.required) }

    var minScale by remember { mutableStateOf(questionData.minScale) }
    var maxScale by remember { mutableStateOf(questionData.maxScale) }
    var minLabel by remember { mutableStateOf(questionData.minLabel) }
    var maxLabel by remember { mutableStateOf(questionData.maxLabel) }

    LaunchedEffect(questionText, questionType, options, required, minScale, maxScale, minLabel, maxLabel) {
        questionData.questionText = questionText
        questionData.questionType = questionType
        questionData.options = options.filter { it.isNotBlank() }.toMutableList()
        questionData.required = required
        questionData.minScale = minScale
        questionData.maxScale = maxScale
        questionData.minLabel = minLabel
        questionData.maxLabel = maxLabel
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = "Pertanyaan ${questionNumber + 1}",
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
                        if (newType != QuestionType.MultipleChoice && newType != QuestionType.Checkbox) {
                            options = mutableListOf()
                            questionData.options = mutableListOf()
                        } else if (options.isEmpty()) {
                            options = mutableListOf("")
                            questionData.options = mutableListOf("")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (questionType) {
                QuestionType.MultipleChoice, QuestionType.Checkbox -> {
                    Column {
                        options.forEachIndexed { index, option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                if (questionType == QuestionType.MultipleChoice) {
                                    RadioButton(selected = false, onClick = null, enabled = false)
                                } else {
                                    Checkbox(checked = false, onCheckedChange = null, enabled = false)
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
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                                )
                                if (options.size > 1) {
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
                                range = 0..1,
                                onValueChange = { minScale = it }
                            )
                            Text(" sampai ")
                            DropdownSelector(
                                value = maxScale,
                                range = 2..10,
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
                    Text(
                        "Responden akan menjawab dalam format teks bebas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                // Menambahkan case untuk TrueFalse dan FileUpload
                QuestionType.true_false -> {
                    Text(
                        "Responden akan memilih antara Benar atau Salah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
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
                Spacer(modifier = Modifier.weight(1f))
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
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            QuestionType.entries.forEach { type -> // Menggunakan .entries untuk enum modern
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

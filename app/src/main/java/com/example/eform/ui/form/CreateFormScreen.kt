package com.example.eform.ui.form

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.components.QuestionInput
import com.example.eform.ui.form.components.QuestionInputData
import com.example.eform.ui.theme.EformTheme // Pastikan tema diimpor jika MaterialTheme digunakan
import com.example.eform.ui.viewmodel.CreateFormResultUi
import com.example.eform.ui.viewmodel.CreateFormViewModel
import com.example.eform.utils.NotificationHelper // Jika notifikasi sistem dibuat dari UI


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFormScreen(
    navController: NavController,
    userIdentifier: String, // NIP guru
    createFormViewModel: CreateFormViewModel = viewModel(
        factory = CreateFormViewModel.CreateFormViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val questions = remember { mutableStateListOf<QuestionInputData>() }

    val createFormResult by createFormViewModel.createFormResult.collectAsState()
    val isTeacherReady by createFormViewModel.teacherInitialized.collectAsState()

    val isLoading = createFormResult is CreateFormResultUi.Loading || !isTeacherReady

    var showSuccessDialog by remember { mutableStateOf(false) }
    // Ubah tipe state dialog menjadi nullable String untuk mencerminkan kemungkinan null dari API
    var dialogGeneratedCode by remember { mutableStateOf<String?>(null) }
    var dialogGeneratedLink by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userIdentifier) {
        if (userIdentifier.isNotBlank()) {
            createFormViewModel.initializeTeacher(userIdentifier)
        } else {
            Toast.makeText(context, "NIP Guru tidak valid.", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(createFormResult) {
        when (val result = createFormResult) {
            is CreateFormResultUi.Success -> {
                // Ambil formCode dari result.createdForm.formCode
                // Jika formCode di FormApiModel nullable, ini bisa null
                dialogGeneratedCode = result.createdForm.formCode
                dialogGeneratedLink = result.generatedLink
                showSuccessDialog = true

                if (result.requiresSystemNotification && result.notificationMessage != null) {
                    NotificationHelper.showNotification(
                        context,
                        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                        "Pencapaian E-Form!",
                        result.notificationMessage,
                        targetScreenRoute = Screen.Notification.route.replace("{userIdentifier}", userIdentifier)
                    )
                }
            }
            is CreateFormResultUi.Error -> {
                Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                createFormViewModel.resetResult()
            }
            else -> { /* Idle atau Loading */ }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                createFormViewModel.resetResult()
                navController.popBackStack()
            },
            title = { Text("Formulir Disimpan!", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Kode Formulir:", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = dialogGeneratedCode ?: "Tidak Ada Kode", // <-- PERBAIKAN NPE
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Form Code", dialogGeneratedCode ?: "")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Kode Formulir disalin!", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.ContentCopy, "Salin Kode") }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Link Akses:", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = dialogGeneratedLink ?: "Link Tidak Tersedia", // <-- PERBAIKAN NPE
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 3, // Izinkan beberapa baris jika link panjang
                            softWrap = true
                        )
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Form Link", dialogGeneratedLink ?: "")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link Akses disalin!", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Default.ContentCopy, "Salin Link") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    createFormViewModel.resetResult()
                    navController.popBackStack()
                }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = { StandardTopAppBar(title = "Buat Formulir Baru", navController = navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!isLoading) questions.add(QuestionInputData()) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pertanyaan")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Judul formulir tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (questions.isEmpty()) {
                        Toast.makeText(context, "Minimal harus ada satu pertanyaan.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (questions.any { it.questionText.isBlank() }) {
                        Toast.makeText(context, "Teks pertanyaan tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    createFormViewModel.createForm(title, description, questions.toList())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isLoading
            ) {
                if (isLoading && createFormResult is CreateFormResultUi.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Simpan Formulir")
                }
            }
        }
    ) { innerPadding ->
        if (!isTeacherReady && createFormResult !is CreateFormResultUi.Loading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Menginisialisasi data guru...")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Detail Formulir", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Judul Formulir") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                                singleLine = true,
                                readOnly = isLoading
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Deskripsi (opsional)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
                                maxLines = 3,
                                readOnly = isLoading
                            )
                        }
                    }
                }
                itemsIndexed(questions, key = { _, itemData -> itemData.id }) { index, itemData ->
                    QuestionInput(
                        questionData = itemData,
                        onDelete = { if (!isLoading) questions.removeAt(index) },
                        questionNumber = index
                    )
                }
            }
        }
    }
}

// Preview tetap sama
@Preview(showBackground = true)
@Composable
fun CreateFormScreenPreview() {
    EformTheme {
        CreateFormScreen(navController = rememberNavController(), userIdentifier = "dummyNipGuru")
    }
}

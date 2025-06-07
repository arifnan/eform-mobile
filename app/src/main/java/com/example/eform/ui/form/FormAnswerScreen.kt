package com.example.eform.ui.form

import android.Manifest
import android.app.Activity
import android.app.Application
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.QuestionEntity
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.ui.viewmodel.FormAnswerUiState
import com.example.eform.ui.viewmodel.FormAnswerViewModel
import com.example.eform.utils.CameraCaptureHelper
import com.example.eform.utils.Constants
import com.example.eform.utils.NotificationHelper
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormAnswerScreen(
    navController: NavController,
    formId: Int,
    userIdentifier: String, // email siswa
    formAnswerViewModel: FormAnswerViewModel = viewModel(
        factory = FormAnswerViewModel.FormAnswerViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val context = LocalContext.current
    val uiState by formAnswerViewModel.uiState.collectAsState()

    // State lokal untuk UI yang tidak langsung dari ViewModel state
    val answers = remember { mutableStateMapOf<Int, String>() }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var photoFileForUpload by remember { mutableStateOf<File?>(null) }
    var locationTaken by remember { mutableStateOf(false) }
    var photoTaken by remember { mutableStateOf(false) }
    var isLocationCurrentlyValid by remember { mutableStateOf(false) }
    var locationMessageDisplay by remember { mutableStateOf("Lokasi belum diambil") }
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }

    val cameraHelper = remember { CameraCaptureHelper(context) }

    val cameraLauncherForResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cameraHelper.photoUri?.let { uri ->
                capturedImageUri = uri
                photoTaken = true
                // Konversi URI ke File untuk diupload
                val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    photoFileForUpload = tempFile
                    Toast.makeText(context, "Foto berhasil diambil!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("FormAnswerScreen", "Error converting URI to File: ${e.message}")
                    Toast.makeText(context, "Gagal memproses foto.", Toast.LENGTH_SHORT).show()
                    photoTaken = false
                }
            } ?: run {
                photoTaken = false
                Toast.makeText(context, "Gagal mendapatkan URI foto.", Toast.LENGTH_SHORT).show()
            }
        } else {
            photoTaken = false
            Toast.makeText(context, "Pengambilan foto dibatalkan.", Toast.LENGTH_SHORT).show()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val cameraIntent = cameraHelper.createCameraIntent()
            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                cameraLauncherForResult.launch(cameraIntent)
            } else {
                Toast.makeText(context, "Tidak ada aplikasi kamera.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera diperlukan.", Toast.LENGTH_LONG).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission() // Diubah ke single permission
    ) { isGranted ->
        if (isGranted) {
            // Langsung panggil validateLocation dari ViewModel
            formAnswerViewModel.validateLocation()
        } else {
            locationMessageDisplay = "❌ Izin lokasi ditolak."
            Toast.makeText(context, "Izin lokasi diperlukan untuk fitur ini.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(formId, userIdentifier) {
        formAnswerViewModel.loadFormDetails(formId, userIdentifier)
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is FormAnswerUiState.LocationValidationResult -> {
                locationTaken = true
                isLocationCurrentlyValid = state.isValid
                locationMessageDisplay = state.message
                currentLatitude = state.lat
                currentLongitude = state.lng
            }
            is FormAnswerUiState.SubmissionSuccess -> {
                Toast.makeText(context, state.successMessage, Toast.LENGTH_LONG).show()
                state.notificationMessageForSytem?.let { msg ->
                    NotificationHelper.showNotification(
                        context,
                        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), // ID notif unik sederhana
                        state.notificationTitle ?: "Formulir Terkirim!",
                        msg,
                        // Pastikan rute Screen.Notification menerima userIdentifier
                        targetScreenRoute = Screen.Notification.route.replace("{userIdentifier}", userIdentifier)
                    )
                }
                formAnswerViewModel.resetUiState()
                navController.popBackStack() // Kembali setelah sukses
            }
            is FormAnswerUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                formAnswerViewModel.resetUiState()
            }
            else -> { /* Idle, LoadingQuestions, Submitting dihandle oleh UI lain */ }
        }
    }

    var currentFormTitle by remember { mutableStateOf("Isi Formulir") }
    var currentFormDescription by remember { mutableStateOf("") }
    var questionsForDisplay by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }

    if (uiState is FormAnswerUiState.QuestionsLoaded) {
        val loadedState = uiState as FormAnswerUiState.QuestionsLoaded
        currentFormTitle = loadedState.form.title
        currentFormDescription = loadedState.form.description
        questionsForDisplay = loadedState.questions
        // Inisialisasi map jawaban jika belum ada (hanya sekali saat pertanyaan dimuat)
        LaunchedEffect(loadedState.questions) {
            loadedState.questions.forEach { q -> answers.putIfAbsent(q.id, "") }
        }
    }

    Scaffold(
        topBar = { StandardTopAppBar(title = currentFormTitle, navController = navController) }
    ) { innerPadding ->
        when (uiState) {
            FormAnswerUiState.LoadingQuestions, FormAnswerUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is FormAnswerUiState.QuestionsLoaded,
            is FormAnswerUiState.LocationValidationResult, // Tetap tampilkan form saat validasi lokasi
            is FormAnswerUiState.Submitting -> { // Tetap tampilkan form saat submitting, tombol akan disable
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = currentFormTitle, style = MaterialTheme.typography.headlineSmall)
                            if (currentFormDescription.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(currentFormDescription, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    questionsForDisplay.forEach { question ->
                        QuestionDisplayItem(
                            question = question,
                            currentAnswer = answers[question.id] ?: "",
                            onAnswerChange = { newAnswer -> answers[question.id] = newAnswer }
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    Divider(Modifier.padding(vertical = 16.dp))
                    Text("Verifikasi Lokasi", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(locationMessageDisplay, style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isLocationCurrentlyValid && locationTaken) Color(0xFF2E7D32) // Hijau tua
                        else if (locationTaken) Color.Red
                        else MaterialTheme.colorScheme.onSurface
                    ))
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // Cukup panggil launcher izin. Logika validasi sudah ada di ViewModel.
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, "Ambil Lokasi")
                        Spacer(Modifier.width(8.dp))
                        Text("Validasi Lokasi Saat Ini")
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(Modifier.padding(vertical = 16.dp))
                    Text("Bukti Foto", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (capturedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(capturedImageUri),
                            contentDescription = "Foto Bukti",
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, "Ambil Foto")
                        Spacer(Modifier.width(8.dp))
                        Text(if (capturedImageUri == null) "Ambil Foto (Wajib)" else "Ambil Ulang Foto")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val currentQuestions = (uiState as? FormAnswerUiState.QuestionsLoaded)?.questions ?: emptyList()
                            val unansweredRequired = currentQuestions.filter {
                                it.required && answers[it.id].isNullOrBlank()
                            }
                            if (unansweredRequired.isNotEmpty()) {
                                Toast.makeText(context, "Harap isi semua pertanyaan wajib (*)", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            formAnswerViewModel.submitAnswers(
                                formId = formId, // formId dari parameter fungsi
                                answersMap = answers.toMap(),
                                photoFile = photoFileForUpload,
                                latitude = currentLatitude,
                                longitude = currentLongitude,
                                isLocationPreviouslyValidatedAndCorrect = isLocationCurrentlyValid && locationTaken
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is FormAnswerUiState.Submitting && locationTaken && photoTaken && isLocationCurrentlyValid
                    ) {
                        if (uiState is FormAnswerUiState.Submitting) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Kirim Jawaban")
                        }
                    }
                }
            }
            is FormAnswerUiState.Error -> { // Ditangani oleh LaunchedEffect, tapi bisa ada UI fallback
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Gagal mengirim. Silakan coba lagi atau periksa koneksi Anda.")
                }
            }
            else -> { // State lain yang mungkin belum tercover eksplisit di UI utama
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Memuat...")
                }
            }
        }
    }
}

// Composable QuestionDisplayItem (tetap sama seperti sebelumnya)
@Composable
fun QuestionDisplayItem(
    question: QuestionEntity,
    currentAnswer: String,
    onAnswerChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)){
            Column(Modifier.padding(12.dp)){
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (question.required) {
                        Text(" *", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (question.questionType) {
                    QuestionType.Text -> {
                        OutlinedTextField(
                            value = currentAnswer,
                            onValueChange = onAnswerChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Jawaban Anda") },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
                            maxLines = 5
                        )
                    }
                    QuestionType.MultipleChoice -> {
                        Column {
                            question.options.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAnswerChange(option) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = (currentAnswer == option),
                                        onClick = { onAnswerChange(option) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(option)
                                }
                            }
                        }
                    }
                    QuestionType.Checkbox -> {
                        val selectedOptions = remember(currentAnswer) {
                            currentAnswer.split(";;").filter { it.isNotBlank() }.toMutableSet()
                        }
                        Column {
                            question.options.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedOptions.contains(option)) {
                                                selectedOptions.remove(option)
                                            } else {
                                                selectedOptions.add(option)
                                            }
                                            onAnswerChange(selectedOptions.joinToString(";;"))
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedOptions.contains(option),
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                selectedOptions.add(option)
                                            } else {
                                                selectedOptions.remove(option)
                                            }
                                            onAnswerChange(selectedOptions.joinToString(";;"))
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(option)
                                }
                            }
                        }
                    }
                    QuestionType.LinearScale -> {
                        val scaleValue = currentAnswer.toFloatOrNull() ?: question.options.getOrNull(0)?.toFloatOrNull() ?: 0f
                        val minScale = question.options.getOrNull(0)?.toFloatOrNull() ?: 0f
                        val maxScale = question.options.getOrNull(1)?.toFloatOrNull() ?: 5f
                        val steps = if (maxScale > minScale) (maxScale.toInt() - minScale.toInt() - 1).coerceAtLeast(0) else 0

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ){
                                Text(question.options.getOrNull(2) ?: minScale.toInt().toString(), style = MaterialTheme.typography.bodySmall)
                                Text(question.options.getOrNull(3) ?: maxScale.toInt().toString(), style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = scaleValue,
                                onValueChange = { onAnswerChange(it.toInt().toString()) },
                                valueRange = minScale..maxScale,
                                steps = steps,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )
                            Text(
                                text = "Pilihan Anda: ${currentAnswer.ifEmpty { "-" }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    // Menambahkan case untuk TrueFalse dan FileUpload
                    QuestionType.true_false -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start // Atau SpaceEvenly
                        ) {
                            val optionTrue = "Benar"
                            val optionFalse = "Salah"

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onAnswerChange(optionTrue) }
                                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                RadioButton(
                                    selected = (currentAnswer == optionTrue),
                                    onClick = { onAnswerChange(optionTrue) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(optionTrue)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onAnswerChange(optionFalse) }
                                    .padding(top = 4.dp, bottom = 4.dp)
                            ) {
                                RadioButton(
                                    selected = (currentAnswer == optionFalse),
                                    onClick = { onAnswerChange(optionFalse) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(optionFalse)
                            }
                        }
                    }
                    QuestionType.file_upload -> {
                        // Untuk FileUpload, kita bisa tampilkan nama file jika sudah dipilih,
                        // atau tombol untuk memilih file.
                        // Implementasi pemilihan file sebenarnya memerlukan ActivityResultLauncher.
                        // Di sini kita buat placeholder UI.
                        val context = LocalContext.current
                        var fileName by remember(currentAnswer) { mutableStateOf(if (currentAnswer.isNotBlank()) Uri.parse(currentAnswer).lastPathSegment else "Belum ada file dipilih") }

                        // Launcher untuk memilih file (contoh untuk semua jenis file)
                        val filePickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            if (uri != null) {
                                onAnswerChange(uri.toString()) // Simpan URI sebagai string
                                fileName = uri.lastPathSegment ?: "File dipilih"
                                Toast.makeText(context, "File dipilih: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Column {
                            Text("File: $fileName", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                filePickerLauncher.launch("*/*") // Membuka pemilih file
                            }) {
                                Text(if (currentAnswer.isNotBlank()) "Ganti File" else "Pilih File")
                            }
                            Text(
                                "Catatan: Bukti foto utama diunggah terpisah di bawah.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
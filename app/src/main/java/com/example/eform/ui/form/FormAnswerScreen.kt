package com.example.eform.ui.form

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.eform.R
import com.example.eform.data.model.api.QuestionApiModel
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.ui.viewmodel.FormAnswerUiState
import com.example.eform.ui.viewmodel.FormAnswerViewModel
import com.example.eform.ui.viewmodel.SubmitFormResult
import com.example.eform.utils.CameraCaptureHelper
import com.example.eform.utils.LocationHelper
import com.example.eform.utils.NotificationHelper
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormAnswerScreen(
    navController: NavController,
    formId: Int,
    userIdentifier: String,
    formAnswerViewModel: FormAnswerViewModel = viewModel(
        factory = FormAnswerViewModel.FormAnswerViewModelFactory(
            LocalContext.current.applicationContext as Application,
            formId,
            userIdentifier
        )
    )
) {
    val context = LocalContext.current
    val uiState by formAnswerViewModel.uiState.collectAsState()
    val submitResult by formAnswerViewModel.submitResult.collectAsState()

    val answers by formAnswerViewModel.answers.collectAsState()
    val photoUri by formAnswerViewModel.photoUri.collectAsState()
    val currentPhotoPathFromDraft by formAnswerViewModel.currentPhotoPathFromDraft.collectAsState()
    val currentLocation by formAnswerViewModel.location.collectAsState()

    val cameraHelper = remember { CameraCaptureHelper(context) }
    val locationHelper = remember { LocationHelper(context) }

    // State untuk memicu peluncuran kamera secara terpisah (jembatan)
    var pendingCameraLaunchUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraHelper.photoUri.value?.let { uri ->
                formAnswerViewModel.onPhotoUriChanged(uri)
                Toast.makeText(context, "Foto berhasil diambil!", Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(context, "Gagal mendapatkan URI foto.", Toast.LENGTH_SHORT).show()
        } else {
            formAnswerViewModel.onPhotoUriChanged(null)
            Toast.makeText(context, "Pengambilan foto dibatalkan.", Toast.LENGTH_SHORT).show()
        }
    }

    // LaunchedEffect untuk meluncurkan kamera ketika pendingCameraLaunchUri diatur
    LaunchedEffect(pendingCameraLaunchUri) {
        pendingCameraLaunchUri?.let { uri ->
            cameraLauncher.launch(uri)
            pendingCameraLaunchUri = null
        }
    }

    // --- Permintaan Izin Kamera Manual ---
    val cameraPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsGranted ->
        val allPermissionsGranted = permissionsGranted.all { it.value }
        if (allPermissionsGranted) {
            val newPhotoUri = cameraHelper.createImageUri() // Ini akan mengatur URI di helper
            pendingCameraLaunchUri = newPhotoUri // Atur pendingCameraLaunchUri untuk memicu LaunchedEffect
        } else {
            Toast.makeText(context, "Izin kamera diperlukan.", Toast.LENGTH_LONG).show()
        }
    }

    // --- Permintaan Izin Lokasi Manual ---
    val locationPermissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsGranted ->
        val allPermissionsGranted = permissionsGranted.all { it.value }
        if (allPermissionsGranted) {
            formAnswerViewModel.validateLocation() // Panggil ViewModel's validateLocation
        } else {
            Toast.makeText(context, "Izin lokasi diperlukan.", Toast.LENGTH_LONG).show()
        }
    }


    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, formId, userIdentifier) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                (uiState as? FormAnswerUiState.Success)?.form?.title?.let { title ->
                    formAnswerViewModel.saveDraft(answers.toMap(), photoUri, title)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            (uiState as? FormAnswerUiState.Success)?.form?.title?.let { title ->
                formAnswerViewModel.saveDraft(answers.toMap(), photoUri, title)
            }
            locationHelper.stopLocationUpdates()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = true) {
        (uiState as? FormAnswerUiState.Success)?.form?.title?.let { title ->
            formAnswerViewModel.saveDraft(answers.toMap(), photoUri, title)
        }
        navController.popBackStack()
    }

    // Initial check for location permissions and fetch location if granted
    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            // Jika izin belum diberikan, minta izin saat pertama kali Composisi
            locationPermissionLauncher.launch(locationPermissions)
        } else {
            // Jika izin sudah ada, langsung coba ambil lokasi
            formAnswerViewModel.validateLocation() // Memanggil ViewModel untuk mengambil dan memvalidasi lokasi
        }
    }


    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is FormAnswerUiState.Success -> {
                // Form is loaded successfully
            }
            is FormAnswerUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                formAnswerViewModel.resetUiState()
            }
            FormAnswerUiState.Loading -> { /* Handled by CircularProgressIndicator below */ }
            FormAnswerUiState.Idle -> { /* Nothing specific to do */ }
        }
    }

    LaunchedEffect(submitResult) {
        when (submitResult) {
            is SubmitFormResult.Success -> {
                Toast.makeText(context, "Formulir berhasil dikirim!", Toast.LENGTH_LONG).show()
                val successData = submitResult as SubmitFormResult.Success
                successData.notificationMessageForSytem?.let { msg ->
                    NotificationHelper.showNotification(
                        context,
                        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                        successData.notificationTitle ?: "Formulir Terkirim!",
                        msg,
                        targetScreenRoute = Screen.Notification.route.replace("{userIdentifier}", userIdentifier)
                    )
                }
                formAnswerViewModel.deleteDraft()
                navController.popBackStack()
                navController.navigate(Screen.DashboardStudents.route.replace("{userIdentifier}", userIdentifier)) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
                formAnswerViewModel.resetSubmitResult()
            }
            is SubmitFormResult.Error -> {
                formAnswerViewModel.resetSubmitResult()
            }
            else -> { /* Loading atau Idle */ }
        }
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = (uiState as? FormAnswerUiState.Success)?.form?.title ?: "Memuat Formulir...",
                navController = navController,
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }
    ) { paddingValues ->
        when (val currentState = uiState) {
            FormAnswerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is FormAnswerUiState.Success -> {
                val form = currentState.form
                val currentQuestions = form.questions
                val photoFileToSubmit = photoUri?.let { uri ->
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        file
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = form.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = form.description ?: "Tidak ada deskripsi.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- BAGIAN PERTANYAAN FORMULIR (Sesuai SCRIPT A, ini di tengah) ---
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Pertanyaan Formulir:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentQuestions != null) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp), // Beri tinggi maksimum agar bisa di-scroll terpisah jika perlu
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(currentQuestions) { index, question ->
                                QuestionInput(
                                    question = question,
                                    currentAnswer = answers[question.id] ?: "",
                                    onAnswerChanged = { newAnswer ->
                                        formAnswerViewModel.onAnswerChanged(question.id, newAnswer)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (index < currentQuestions.lastIndex) {
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    } else {
                        Text("Pertanyaan tidak tersedia.", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // --- AKHIR BAGIAN PERTANYAAN FORMULIR ---


                    // --- BAGIAN FOTO & LOKASI (Disesuaikan Posisinya di BAWAH, sesuai permintaan) ---
                    // Mengikuti struktur dari ui.response.FormAnswerScreen.kt
                    // Lokasi dan Foto ditempatkan di sini.

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Verifikasi Lokasi", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Display Lokasi dan Status Verifikasi
                    val isLocationValidCalculated = currentLocation != null && formAnswerViewModel.allowedSchoolLocations.any { school ->
                        locationHelper.isWithinRadius(
                            currentLat = currentLocation!!.latitude,
                            currentLng = currentLocation!!.longitude,
                            targetLat = school.coordinate.latitude,
                            targetLng = school.coordinate.longitude,
                            radiusInMeters = school.radius
                        )
                    }
                    val locationStatusText = if (currentLocation == null) "Lokasi: Belum Terdeteksi"
                    else "Lokasi: ${currentLocation!!.latitude}, ${currentLocation!!.longitude}"
                    val locationVerificationText = if (currentLocation == null) "Status: Belum Diambil"
                    else if (isLocationValidCalculated) "Status: ✅ Valid di area sekolah"
                    else "Status: ❌ Di luar area sekolah yang diizinkan"

                    Text(locationStatusText, style = MaterialTheme.typography.bodySmall)
                    Text(
                        locationVerificationText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (currentLocation == null) Color.Gray
                            else if (isLocationValidCalculated) Color(0xFF2E7D32) // Hijau tua
                            else Color.Red
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Ambil Lokasi
                    Button(onClick = {
                        locationPermissionLauncher.launch(locationPermissions) // Meluncurkan permintaan izin
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.LocationOn, "Ambil Lokasi")
                        Spacer(Modifier.width(8.dp))
                        Text("Ambil Lokasi")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Bukti Foto", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Ambil Foto (Tanpa Permintaan Izin Kamera Eksplisit di sini)
                    Button(onClick = {
                        // Panggil createCameraIntent untuk membuat intent dan juga mengatur photoUri di cameraHelper
                        cameraHelper.createCameraIntent()
                        // Luncurkan cameraLauncher dengan URI tempat gambar akan disimpan (diambil dari helper)
                        cameraHelper.photoUri.value?.let { uri -> // Pastikan photoUri di helper sudah diatur dan bukan null
                            cameraLauncher.launch(uri) // Meluncurkan intent kamera dengan URI sebagai input
                        } ?: run {
                            Toast.makeText(context, "Gagal membuat file foto untuk kamera.", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CameraAlt, "Ambil Foto")
                        Spacer(Modifier.width(8.dp))
                        Text(if (photoUri == null && currentPhotoPathFromDraft == null) "Ambil Foto" else "Ambil Ulang Foto")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tampilan Foto
                    PhotoDisplay(
                        photoUri = photoUri,
                        currentPhotoPath = currentPhotoPathFromDraft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // --- AKHIR BAGIAN FOTO & LOKASI ---


                    Spacer(modifier = Modifier.height(24.dp))

                    // Tombol Kirim Jawaban
                    Button(onClick = {
                        val allQuestionsAnswered = currentQuestions?.all { question ->
                            val answer = answers[question.id]
                            if (question.required) {
                                when (QuestionType.fromString(question.questionType)) {
                                    QuestionType.Text -> !answer.isNullOrBlank()
                                    QuestionType.MultipleChoice -> !answer.isNullOrBlank()
                                    QuestionType.Checkbox -> {
                                        !answer.isNullOrBlank() && answer != "[]" && answer != "[\"\"]"
                                    }
                                    QuestionType.LinearScale -> answer?.toIntOrNull() != null
                                    QuestionType.true_false -> !answer.isNullOrBlank()
                                    else -> false
                                }
                            } else {
                                true
                            }
                        } ?: false


                        val isLocationRequired = form.locationRequired == true
                        val isPhotoRequired = form.photoRequired == true

                        if (isLocationRequired && currentLocation == null) {
                            Toast.makeText(context, "Lokasi wajib diisi.", Toast.LENGTH_SHORT).show()
                        } else if (isLocationRequired && !isLocationValidCalculated) {
                            Toast.makeText(context, "Lokasi tidak valid atau di luar area yang diizinkan.", Toast.LENGTH_SHORT).show()
                        } else if (isPhotoRequired && photoUri == null && currentPhotoPathFromDraft == null) {
                            Toast.makeText(context, "Foto wajib diambil.", Toast.LENGTH_SHORT).show()
                        } else if (!allQuestionsAnswered) {
                            Toast.makeText(context, "Harap lengkapi semua pertanyaan wajib (*).", Toast.LENGTH_SHORT).show()
                        } else {
                            formAnswerViewModel.submitAnswers(
                                photoFile = photoFileToSubmit,
                                currentLocation = currentLocation
                            )
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Kirim Jawaban")
                    }
                }
            }
            is FormAnswerUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Error: ${currentState.message}")
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun PhotoDisplay(photoUri: Uri?, currentPhotoPath: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageToDisplay = when {
        photoUri != null -> {
            Log.d("PhotoDisplay", "Displaying new photo from Uri: $photoUri")
            photoUri
        }
        !currentPhotoPath.isNullOrBlank() -> {
            val uri = try {
                Uri.parse(currentPhotoPath)
            } catch (e: Exception) {
                Log.e("PhotoDisplay", "Error parsing draft photo path to Uri: $currentPhotoPath, ${e.message}")
                null
            }
            Log.d("PhotoDisplay", "Displaying draft photo from path: $currentPhotoPath -> Uri: $uri")
            uri
        }
        else -> {
            Log.d("PhotoDisplay", "No photo to display.")
            null
        }
    }

    if (imageToDisplay != null) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(imageToDisplay)
                    .error(R.drawable.ic_default_profile)
                    .placeholder(R.drawable.ic_default_profile)
                    .build()
            ),
            contentDescription = "Captured Photo",
            modifier = modifier
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .background(Color.LightGray, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Belum ada foto", color = Color.Gray, fontSize = 16.sp)
        }
    }
}


@Composable
fun QuestionInput(
    question: QuestionApiModel,
    currentAnswer: String,
    onAnswerChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(12.dp)) {
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

                when (QuestionType.fromString(question.questionType)) {
                    QuestionType.Text -> {
                        OutlinedTextField(
                            value = currentAnswer,
                            onValueChange = onAnswerChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Jawaban Anda") },
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default),
                            maxLines = 5
                        )
                    }
                    QuestionType.MultipleChoice -> {
                        Column {
                            question.options?.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAnswerChanged(option) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = (currentAnswer == option),
                                        onClick = { onAnswerChanged(option) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(option)
                                }
                            }
                        }
                    }
                    QuestionType.Checkbox -> {
                        val selectedOptionsSet = remember(currentAnswer) {
                            if (currentAnswer.startsWith("[") && currentAnswer.endsWith("]")) {
                                try {
                                    val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                                    com.google.gson.Gson().fromJson<List<String>>(currentAnswer, type)?.toMutableSet() ?: mutableSetOf()
                                } catch (e: Exception) {
                                    Log.e("QuestionInput", "Failed to parse JSON for Checkbox: $currentAnswer, ${e.message}")
                                    mutableSetOf()
                                }
                            } else {
                                currentAnswer.split(",").filter { it.isNotBlank() }.map { it.trim() }.toMutableSet()
                            }
                        }

                        Column {
                            question.options?.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newSet = selectedOptionsSet.toMutableSet()
                                            if (newSet.contains(option)) {
                                                newSet.remove(option)
                                            } else {
                                                newSet.add(option)
                                            }
                                            onAnswerChanged(com.google.gson.Gson().toJson(newSet.toList()))
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedOptionsSet.contains(option),
                                        onCheckedChange = { isChecked ->
                                            val newSet = selectedOptionsSet.toMutableSet()
                                            if (isChecked) {
                                                newSet.add(option)
                                            } else {
                                                newSet.remove(option)
                                            }
                                            onAnswerChanged(com.google.gson.Gson().toJson(newSet.toList()))
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(option)
                                }
                            }
                        }
                    }
                    QuestionType.LinearScale -> {
                        val scaleValue = currentAnswer.toFloatOrNull() ?: question.options?.getOrNull(0)?.toFloatOrNull() ?: 0f
                        val minScale = question.options?.getOrNull(0)?.toFloatOrNull() ?: 0f
                        val maxScale = question.options?.getOrNull(1)?.toFloatOrNull() ?: 5f
                        val steps = if (maxScale > minScale) (maxScale.toInt() - minScale.toInt() - 1).coerceAtLeast(0) else 0

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ){
                                Text(question.options?.getOrNull(2) ?: minScale.toInt().toString(), style = MaterialTheme.typography.bodySmall)
                                Text(question.options?.getOrNull(3) ?: maxScale.toInt().toString(), style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = scaleValue,
                                onValueChange = { onAnswerChanged(it.toInt().toString()) },
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
                    QuestionType.true_false -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            val optionTrue = "Benar"
                            val optionFalse = "Salah"

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onAnswerChanged(optionTrue) }
                                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                RadioButton(
                                    selected = (currentAnswer == optionTrue),
                                    onClick = { onAnswerChanged(optionTrue) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(optionTrue)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onAnswerChanged(optionFalse) }
                                    .padding(top = 4.dp, bottom = 4.dp)
                            ) {
                                RadioButton(
                                    selected = (currentAnswer == optionFalse),
                                    onClick = { onAnswerChanged(optionFalse) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(optionFalse)
                            }
                        }
                    }
                    else -> {
                        Text("Tipe pertanyaan tidak didukung: ${question.questionType}", color = Color.Red)
                    }
                }
            }
        }
    }
}
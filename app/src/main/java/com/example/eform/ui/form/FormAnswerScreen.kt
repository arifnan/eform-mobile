package com.example.eform.ui.form

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.*
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.utils.CameraCaptureHelper
import com.example.eform.utils.LocationHelper
import com.example.eform.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FormAnswerScreen(
    navController: NavController,
    formData: FormEntity,
    userIdentifier: String // email siswa
) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val formDao = db.formDao()
    val userDao = db.userDao()
    val notificationDao = db.notificationDao()
    val formResponseDao = db.formResponseDao() // DAO baru untuk menyimpan respons
    val coroutineScope = rememberCoroutineScope()

    var questions by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    val answers = remember { mutableStateMapOf<Int, String>() } // Key: questionId, Value: answer
    var isLoadingQuestions by remember { mutableStateOf(true) }
    var currentStudentId by remember { mutableStateOf<Int?>(null) }

    // State untuk Lokasi
    val locationHelper = remember { LocationHelper(context) }
    var locationStatus by remember { mutableStateOf("Lokasi belum diambil") }
    var capturedLatitude by remember { mutableStateOf<Double?>(null) }
    var capturedLongitude by remember { mutableStateOf<Double?>(null) }
    var isLocationValid by remember { mutableStateOf(false) }
    var isLocationTaken by remember { mutableStateOf(false) }

    // State untuk Kamera
    val cameraHelper = remember { CameraCaptureHelper(context) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPhotoTaken by remember { mutableStateOf(false) }

    // Target Lokasi (contoh, ganti dengan lokasi sekolah/target Anda)
    // Medan, Sumatera Utara - sekitar Lapangan Merdeka
    val targetLatitude = 3.5917
    val targetLongitude = 98.6753
    val targetRadius = 100.0 // meter

    // Launcher untuk izin lokasi
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
                override fun onLocationResult(lat: Double, lng: Double) {
                    capturedLatitude = lat
                    capturedLongitude = lng
                    isLocationValid = locationHelper.isWithinRadius(lat, lng, targetLatitude, targetLongitude, targetRadius)
                    locationStatus = if (isLocationValid) {
                        "✅ Lokasi valid ($lat, $lng)"
                    } else {
                        "❌ Di luar area yang diizinkan ($lat, $lng). Pengisian formulir mungkin ditolak."
                    }
                    isLocationTaken = true
                }

                override fun onError(message: String) {
                    locationStatus = "❌ Gagal mendapatkan lokasi: $message"
                    isLocationTaken = false
                }
            })
        } else {
            locationStatus = "❌ Izin lokasi ditolak."
            Toast.makeText(context, "Izin lokasi diperlukan untuk fitur ini.", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher untuk izin kamera & ambil foto
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Izin diberikan, luncurkan kamera
            val cameraIntent = cameraHelper.createCameraIntent()
            // Kita butuh launcher baru untuk startActivityForResult
            // cameraLauncherForResult.launch(cameraIntent) // Akan didefinisikan di bawah
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto.", Toast.LENGTH_LONG).show()
        }
    }
    val cameraLauncherForResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            capturedImageUri = cameraHelper.photoUri
            isPhotoTaken = true
            Toast.makeText(context, "Foto berhasil diambil!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Pengambilan foto dibatalkan.", Toast.LENGTH_SHORT).show()
            isPhotoTaken = false
        }
    }


    LaunchedEffect(formData.id, userIdentifier) {
        isLoadingQuestions = true
        withContext(Dispatchers.IO) {
            val student = userDao.getUserByEmail(userIdentifier)
            currentStudentId = student?.id
            questions = formDao.getQuestionsForForm(formData.id)
            questions.forEach { question -> answers[question.id] = "" }
        }
        isLoadingQuestions = false
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = "Isi Formulir: ${formData.title}", navController = navController)
        }
    ) { innerPadding ->
        if (isLoadingQuestions) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()) // Buat seluruh Column bisa di-scroll
                    .padding(16.dp)
            ) {
                // Detail Formulir (Judul & Deskripsi)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = formData.title, style = MaterialTheme.typography.headlineSmall)
                        if (formData.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = formData.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Daftar Pertanyaan
                questions.forEach { question ->
                    QuestionDisplayItem(
                        question = question,
                        currentAnswer = answers[question.id] ?: "",
                        onAnswerChange = { newAnswer ->
                            answers[question.id] = newAnswer
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Garis Pemisah
                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Bagian Lokasi
                Text("Verifikasi Lokasi", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(locationStatus, style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isLocationValid && isLocationTaken) Color.Green.copy(red=0.1f, green=0.5f, blue=0.1f) else if (isLocationTaken) Color.Red else MaterialTheme.colorScheme.onSurface
                ))
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val permissionsToRequest = mutableListOf<String>()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                            // ACCESS_COARSE_LOCATION sudah termasuk jika FINE diberikan
                        } else {
                            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Ambil Lokasi")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ambil Lokasi Saat Ini")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Bagian Kamera
                Text("Bukti Foto", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (capturedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(capturedImageUri),
                        contentDescription = "Foto Bukti",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        // Minta izin kamera dulu
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        // Jika izin sudah ada, launcher di atas akan langsung ke else atau tidak melakukan apa-apa
                        // Jadi, kita perlu meluncurkan kamera setelah izin diberikan
                        // Ini akan dihandle oleh cameraLauncherForResult yang dipanggil setelah cameraPermissionLauncher
                        // Jika izin sudah ada, langsung luncurkan
                        val cameraIntent = cameraHelper.createCameraIntent()
                        if (cameraIntent.resolveActivity(context.packageManager) != null) {
                            cameraLauncherForResult.launch(cameraIntent)
                        } else {
                            Toast.makeText(context, "Tidak ada aplikasi kamera ditemukan.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Ambil Foto")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (capturedImageUri == null) "Ambil Foto (Wajib)" else "Ambil Ulang Foto")
                }


                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Kirim Jawaban
                Button(
                    onClick = {
                        if (currentStudentId == null) {
                            Toast.makeText(context, "Gagal mengidentifikasi pengguna.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (!isLocationTaken) {
                            Toast.makeText(context, "Harap ambil lokasi terlebih dahulu.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (!isLocationValid) {
                            Toast.makeText(context, "Lokasi Anda di luar radius yang diizinkan. Pengisian ditolak.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (!isPhotoTaken || capturedImageUri == null) {
                            Toast.makeText(context, "Harap ambil foto bukti terlebih dahulu.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val unansweredRequiredQuestions = questions.filter {
                            it.required && answers[it.id].isNullOrBlank()
                        }
                        if (unansweredRequiredQuestions.isNotEmpty()) {
                            Toast.makeText(context, "Harap isi semua pertanyaan wajib (*)", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            val response = FormResponseEntity(
                                formId = formData.id,
                                studentId = currentStudentId!!,
                                photoUri = capturedImageUri.toString(),
                                latitude = capturedLatitude,
                                longitude = capturedLongitude,
                                isLocationValid = isLocationValid
                            )
                            val responseId = withContext(Dispatchers.IO) {
                                formResponseDao.insertFormResponse(response)
                            }

                            val answerEntities = answers.map { (questionId, answerText) ->
                                AnswerEntity(
                                    formResponseId = responseId.toInt(),
                                    questionId = questionId,
                                    answerText = answerText
                                )
                            }
                            withContext(Dispatchers.IO) {
                                formResponseDao.insertAnswers(answerEntities)
                            }

                            // Notifikasi untuk Murid
                            val notificationMessage = "Anda berhasil mengisi formulir: ${formData.title}!"
                            val newNotification = NotificationEntity(
                                userId = currentStudentId!!,
                                title = "Formulir Terkirim!",
                                message = notificationMessage
                            )
                            val notificationIdFromDb = withContext(Dispatchers.IO) {
                                notificationDao.insertNotification(newNotification).toInt()
                            }

                            val currentNotifCount = withContext(Dispatchers.IO) {
                                notificationDao.getNotificationCountForUser(currentStudentId!!)
                            }
                            if (currentNotifCount > 8) {
                                val oldestNotif = withContext(Dispatchers.IO) { notificationDao.getOldestNotificationForUser(currentStudentId!!) }
                                oldestNotif?.let { withContext(Dispatchers.IO) { notificationDao.deleteNotificationById(it.id) } }
                            }

                            NotificationHelper.showNotification(
                                context,
                                notificationIdFromDb,
                                "Formulir Berhasil Diisi!",
                                notificationMessage,
                                targetScreenRoute = Screen.Notification.route.replace("{userIdentifier}", userIdentifier)
                            )

                            Toast.makeText(context, "Jawaban berhasil dikirim!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isLocationTaken && isPhotoTaken // Tombol kirim aktif jika lokasi & foto sudah diambil
                ) {
                    Text(text = "Kirim Jawaban")
                }
            }
        }
    }
}

// Composable QuestionDisplayItem tetap sama seperti respons sebelumnya
@Composable
fun QuestionDisplayItem(
    question: QuestionEntity,
    currentAnswer: String,
    onAnswerChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) { // Align top agar bintang sejajar dengan baris pertama teks
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (question.required) {
                Text(" *", color = Color.Red, style = MaterialTheme.typography.titleMedium) // Beri spasi sebelum bintang
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
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Default), // ImeAction.Next atau Done
                    maxLines = 5 // Izinkan multiple lines untuk jawaban teks yang lebih panjang
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
                    currentAnswer.split(";;").filter { it.isNotBlank() }.toMutableSet() // Gunakan pemisah yang unik
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
                // Pastikan maxScale lebih besar dari minScale untuk menghindari error pada steps
                val steps = if (maxScale > minScale) (maxScale.toInt() - minScale.toInt() - 1).coerceAtLeast(0) else 0

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp), // Padding untuk label skala
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
        }
    }
}
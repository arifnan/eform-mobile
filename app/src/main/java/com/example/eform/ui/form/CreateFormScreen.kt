package com.example.eform.ui.form

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.NotificationEntity
import com.example.eform.data.model.QuestionEntity
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.components.QuestionInput
import com.example.eform.ui.form.components.QuestionInputData
import com.example.eform.ui.form.components.QuestionType
import com.example.eform.utils.Constants
import com.example.eform.utils.NotificationHelper
import com.example.eform.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFormScreen(
    navController: NavController,
    userIdentifier: String // NIP guru
) {
    val context = LocalContext.current
//    Log.d("CreateFormScreen", "Received userIdentifier: $userIdentifier")
//    Toast.makeText(context, "Guru Identifier: $userIdentifier", Toast.LENGTH_LONG).show()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val questions = remember { mutableStateListOf<QuestionInputData>() }
    val coroutineScope = rememberCoroutineScope()

    val db = AppDatabase.getDatabase(context)
    val formDao = db.formDao()
    val notificationDao = db.notificationDao()
    val userDao = db.userDao()

    var showDialog by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf("") }
    var generatedLink by remember { mutableStateOf("") }
    var currentTeacherId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(userIdentifier) {
        Log.d("CreateFormScreen", "LaunchedEffect triggered with userIdentifier: $userIdentifier")
        withContext(Dispatchers.IO) {
            val teacher = userDao.getUserByNip(userIdentifier)
            currentTeacherId = teacher?.id
            Log.d("CreateFormScreen", "Teacher found: ${teacher?.name}, Teacher ID set to: $currentTeacherId")
        }
    }


    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                navController.popBackStack()
            },
            title = { Text("Formulir Disimpan!", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Kode Formulir:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedCode,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Form Code", generatedCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Kode Formulir disalin!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Salin Kode Formulir")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Link Akses:", style = MaterialTheme.typography.titleSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier
                        = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = generatedLink,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            softWrap = true
                        )
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Form Link", generatedLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link Akses disalin!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Salin Link Akses")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    navController.popBackStack()
                }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = "Buat Formulir Baru", navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { questions.add(QuestionInputData()) },
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
                        Toast.makeText(context, "Judul formulir tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (questions.any { it.questionText.isBlank() && (it.questionType != QuestionType.Text || it.options.all { opt -> opt.isBlank() }) }) {
                        Toast.makeText(context, "Pastikan semua teks pertanyaan dan opsi (jika ada) telah diisi", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
//                    if (currentTeacherId == null) {
//                        Toast.makeText(context, "Gagal mengidentifikasi guru.", Toast.LENGTH_LONG).show()
//                        return@Button
//                    }

                    coroutineScope.launch {
                        val teacherId = currentTeacherId!! // Sudah dicek tidak null

                        val formCode = UUID.randomUUID().toString().take(8).uppercase()
                        // TODO: Pertimbangkan menambahkan teacherId ke FormEntity jika form dimiliki oleh guru tertentu
                        val formEntity = FormEntity(title = title, description = description, formCode = formCode)

                        val formId = withContext(Dispatchers.IO) { formDao.insertForm(formEntity).toInt() }

                        val questionEntities = questions.map { qData ->
                            QuestionEntity(
                                formId = formId,
                                questionText = qData.questionText,
                                questionType = qData.questionType,
                                options = qData.options.filter { it.isNotBlank() },
                                answer = "", // Untuk LinearScale, 'options' di QuestionEntity perlu menyimpan [min, max, minLabel, maxLabel]
                                required = qData.required
                            )
                        }
                        withContext(Dispatchers.IO) { formDao.insertQuestions(questionEntities) }

                        // Logika Notifikasi untuk Guru
                        val formCount = withContext(Dispatchers.IO) {
                            // Jika ingin menghitung form yang dibuat oleh guru ini saja, FormEntity perlu teacherId
                            // dan query di DAO perlu diubah. Saat ini hitung semua form.
                            formDao.getAllForms().size
                        }

                        if (formCount % 10 == 0 && formCount > 0) {
                            val notificationMessage = "Anda telah membuat total ${formCount} formulir!"
                            val newNotification = NotificationEntity(
                                userId = teacherId,
                                title = "Pencapaian Pembuatan Formulir!",
                                message = notificationMessage
                            )
                            val notificationIdFromDb = withContext(Dispatchers.IO) { notificationDao.insertNotification(newNotification).toInt() }

                            val currentNotifCount = withContext(Dispatchers.IO) { notificationDao.getNotificationCountForUser(teacherId) }
                            if (currentNotifCount > 8) {
                                val oldest = withContext(Dispatchers.IO) {notificationDao.getOldestNotificationForUser(teacherId)}
                                oldest?.let { withContext(Dispatchers.IO) {notificationDao.deleteNotificationById(it.id)} }
                            }

                            NotificationHelper.showNotification(
                                context,
                                notificationIdFromDb,
                                "Pencapaian E-Form!",
                                notificationMessage,
                                targetScreenRoute = Screen.Notification.route.replace("{userIdentifier}", userIdentifier)
                            )
                        }
                        generatedCode = formCode
                        generatedLink = "${Constants.APP_DEEP_LINK_SCHEME}://${Constants.APP_DEEP_LINK_HOST}/$formCode"
                        showDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Simpan Formulir")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
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
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Deskripsi (opsional)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            maxLines = 3
                        )
                    }
                }
            }
            itemsIndexed(questions, key = { _, itemData -> itemData.id }) { index, itemData ->
                QuestionInput(
                    questionData = itemData,
                    onDelete = { questions.removeAt(index) },
                    questionNumber = index
                )
            }
            item {
                Spacer(modifier = Modifier.height(60.dp)) // Spacer untuk memberi ruang dari bottom bar (tombol simpan)
            }
        }
    }
}
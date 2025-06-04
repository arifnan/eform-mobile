package com.example.eform.ui.dashboard

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination // Import findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.Form // Jika Form masih dipakai untuk sampleFormsStudents
import com.example.eform.data.model.Teacher // Jika Teacher masih dipakai untuk sampleFormsStudents
import com.example.eform.navigation.Screen
// Hapus import BottomBarItem jika tidak digunakan langsung di sini
// import com.example.eform.ui.components.BottomBarItem
import com.example.eform.ui.components.BottomBarItemStudents // Pastikan ini diimpor
import com.example.eform.ui.components.SimpleBottomNavigationBarStudents
import com.example.eform.ui.viewmodel.DashboardStudentsViewModel
import com.example.eform.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DashboardScreenStudents(
    navController: NavController,
    userIdentifier: String,
    dashboardStudentsViewModel: DashboardStudentsViewModel = viewModel(
    factory = DashboardStudentsViewModel.DashboardStudentsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    var searchQuery by remember { mutableStateOf("") }
    var forms by remember { mutableStateOf(sampleFormsStudents()) }
    var isLoading by remember { mutableStateOf(false) }
    // var sortOrder by remember { mutableStateOf("asc") } // Tidak digunakan di UI saat ini

    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val formDao = db.formDao()
    var userName by remember { mutableStateOf("Student") }

    var formCodeInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userIdentifier) {
        launch(Dispatchers.IO) {
            val user = db.userDao().getUserByEmail(userIdentifier)
            user?.let {
                userName = it.name
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary) // Gunakan warna tema
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                navController.navigate(Screen.Profile.route.replace("{userIdentifier}", userIdentifier))
                            }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = "Hello, $userName", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Welcome back", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifikasi",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            navController.navigate(Screen.Notification.route.replace("{userIdentifier}", userIdentifier))
                        }
                )
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background) // Gunakan warna tema
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Cari formulir...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = formCodeInput,
                    onValueChange = { formCodeInput = it.uppercase() },
                    label = { Text("Masukkan Kode Formulir") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    trailingIcon = {
                        Button(
                            onClick = {
                                if (formCodeInput.isNotBlank()) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val form = formDao.getFormByCode(formCodeInput)
                                        launch(Dispatchers.Main) {
                                            if (form != null) {
                                                navController.navigate(
                                                    Screen.FormAnswer.route.replace("{formId}", "${form.id}")
                                                )
                                            } else {
                                                Toast.makeText(context, "Kode formulir tidak valid", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Kode tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp) // Sedikit lebih membulat
                        ) {
                            Text("Akses")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text("Riwayat Formulir Saya:", style = MaterialTheme.typography.titleMedium) // Atau "Formulir Tersedia"
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) { // Anda perlu memuat 'forms' dari database jika ini bukan hanya sampel
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (forms.filter { it.title.contains(searchQuery, ignoreCase = true) }.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if(searchQuery.isNotBlank()) "Tidak ada formulir ditemukan." else "Belum ada formulir yang dikerjakan/tersedia.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(forms.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description?.contains(searchQuery, ignoreCase = true) == true }) { form ->
                            Card(
                                modifier = Modifier
                                    .aspectRatio(1f) // Menjaga rasio kotak
                                    .clickable {
                                        navController.navigate(Screen.PreviewForm.route.replace("{formId}", "${form.id}"))
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // Warna kartu
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = form.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    if (form.description?.isNotBlank() == true) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = form.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 3,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            var selectedIndex by remember { mutableStateOf(0) } // State untuk selectedIndex
            SimpleBottomNavigationBarStudents(
                selectedIndex = selectedIndex,
                onItemSelected = { newIndex ->
                    selectedIndex = newIndex // Update state selectedIndex
                    val item = BottomBarItemStudents.values()[newIndex]
                    val currentRoute = navController.currentBackStackEntry?.destination?.route

                    // Tentukan targetRoute berdasarkan item BottomBarItemStudents
                    val targetRoute = when (item) {
                        BottomBarItemStudents.Home -> userIdentifier.let { Screen.DashboardStudents.route.replace("{userIdentifier}", it) }
                        // >>> PERBAIKAN UTAMA DI SINI <<<
                        BottomBarItemStudents.FavoriteForms -> userIdentifier.let { Screen.FavoriteForms.route.replace("{userIdentifier}", it) }
                        BottomBarItemStudents.History -> Screen.HistoryForm.route // Jika HistoryForm tidak butuh userIdentifier
                        BottomBarItemStudents.Profile -> userIdentifier.let { Screen.Profile.route.replace("{userIdentifier}", it) }
                    }

                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                navController = navController,
                userIdentifier = userIdentifier
            )
        }
    )
}

// sampleFormsStudents dan PreviewDashboardStudents tetap sama
fun sampleFormsStudents(): List<Form> {
    val teacher = Teacher(
        id = 1,
        name = "Ibu Guru",
        nip = 12345678,
        email = "guru@example.com",
        subject = "Matematika"
    )
    return listOf(
        Form(id = 1, title = "Kuis Harian Bab 1", description = "Kuis singkat tentang materi Aljabar.", teacher_id = teacher.id, createdAt = "2025-05-20", teacher = teacher),
        Form(id = 2, title = "Survey Kepuasan Belajar", description = "Mohon isi survey ini dengan jujur.", teacher_id = teacher.id, createdAt = "2025-05-15", teacher = teacher),
        Form(id = 3, title = "Umpan Balik PJJ", description = "Berikan masukan Anda.", teacher_id = teacher.id, createdAt = "2025-05-10", teacher = teacher)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreviewStudents() {
    val dummyNavController = rememberNavController()
    DashboardScreenStudents(navController = dummyNavController, userIdentifier = "student@example.com")
}
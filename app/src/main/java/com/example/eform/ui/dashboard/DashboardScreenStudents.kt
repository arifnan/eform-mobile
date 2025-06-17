package com.example.eform.ui.dashboard

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.SimpleBottomNavigationBarStudents
import com.example.eform.ui.viewmodel.DashboardStudentsViewModel
import com.example.eform.ui.viewmodel.FormCodeValidationResult
import com.example.eform.ui.viewmodel.StudentDashboardUiState

@Composable
fun DashboardScreenStudents(
    navController: NavController,
    userIdentifier: String,
    dashboardStudentsViewModel: DashboardStudentsViewModel = viewModel(
        factory = DashboardStudentsViewModel.DashboardStudentsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by dashboardStudentsViewModel.uiState.collectAsState()

    LaunchedEffect(userIdentifier) {
        dashboardStudentsViewModel.loadStudentDashboardData()
    }

    when (val state = uiState) {
        is StudentDashboardUiState.Loading, StudentDashboardUiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is StudentDashboardUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { dashboardStudentsViewModel.loadStudentDashboardData() }) {
                        Text("Coba Lagi")
                    }
                }
            }
        }
        is StudentDashboardUiState.Success -> {
            DashboardStudentsContent(
                navController = navController,
                userIdentifier = userIdentifier,
                state = state,
                dashboardStudentsViewModel = dashboardStudentsViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardStudentsContent(
    navController: NavController,
    userIdentifier: String,
    state: StudentDashboardUiState.Success,
    dashboardStudentsViewModel: DashboardStudentsViewModel
) {
    val context = LocalContext.current
    val formCodeValidationResult by dashboardStudentsViewModel.formCodeValidationResult.collectAsState()
    val student = state.student // Ambil data siswa dari state

    var formCodeInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(formCodeValidationResult) {
        when (val result = formCodeValidationResult) {
            is FormCodeValidationResult.Valid -> {
                Toast.makeText(context, "Formulir ditemukan: ${result.form.title}", Toast.LENGTH_SHORT).show()
                navController.navigate(
                    Screen.FormAnswer.route
                        .replace("{formId}", "${result.form.id}")
                        .replace("{userIdentifier}", userIdentifier)
                )
                dashboardStudentsViewModel.resetFormCodeValidation()
            }
            is FormCodeValidationResult.Invalid -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                dashboardStudentsViewModel.resetFormCodeValidation()
            }
            else -> { /* Idle atau Loading */ }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ========== PERBAIKAN PROFIL GAMBAR SISWA ==========
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                            .clickable {
                                navController.navigate(Screen.Profile.route.replace("{userIdentifier}", userIdentifier))
                            }
                    ) {
                        if (student.profilePhotoUrl.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = student.profilePhotoUrl,
                                contentDescription = "Foto Profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    // ===============================================
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(text = "Hello, ${student.name}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Welcome back", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifikasi",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp).clickable {
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
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari riwayat formulir...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val isValidationLoading = formCodeValidationResult is FormCodeValidationResult.Loading

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = formCodeInput,
                        onValueChange = { formCodeInput = it.uppercase() },
                        label = { Text("Masukkan Kode Formulir") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        readOnly = isValidationLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { dashboardStudentsViewModel.validateFormCode(formCodeInput) },
                        enabled = !isValidationLoading,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (isValidationLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Akses")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Riwayat Formulir Saya:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val filteredItems by remember(searchQuery, state.recentResponses) {
                    derivedStateOf {
                        if (searchQuery.isBlank()) {
                            state.recentResponses
                        } else {
                            state.recentResponses.filter { displayItem ->
                                val form = displayItem.response.form
                                form?.title?.contains(searchQuery, ignoreCase = true) == true ||
                                        form?.description?.contains(searchQuery, ignoreCase = true) == true
                            }
                        }
                    }
                }

                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isNotBlank()) "Tidak ada riwayat ditemukan." else "Belum ada riwayat pengisian formulir.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredItems, key = { it.response.id }) { displayItem ->
                            val form = displayItem.response.form
                            if (form != null) {
                                FormCardItem(
                                    form = form,
                                    isFavorite = displayItem.isFavorite,
                                    onFormClick = {
                                        navController.navigate(
                                            Screen.FormResponseDetail.route.replace("{responseId}", "${displayItem.response.id}")
                                        )
                                    },
                                    onToggleFavorite = { formId, newStatus ->
                                        dashboardStudentsViewModel.toggleFavoriteStatus(formId, newStatus)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            var selectedIndex by remember { mutableStateOf(0) }
            SimpleBottomNavigationBarStudents(
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it },
                navController = navController,
                userIdentifier = userIdentifier
            )
        }
    )
}
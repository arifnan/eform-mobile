package com.example.eform.ui.dashboard

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.SimpleBottomNavigationBarStudents
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.DashboardStudentsViewModel
import com.example.eform.ui.viewmodel.FormCodeValidationResult
import com.example.eform.ui.viewmodel.StudentDashboardUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenStudents(
    navController: NavController,
    userIdentifier: String, // Ini adalah email siswa
    dashboardStudentsViewModel: DashboardStudentsViewModel = viewModel(
        factory = DashboardStudentsViewModel.DashboardStudentsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val uiState by dashboardStudentsViewModel.uiState.collectAsState()
    val formCodeValidationResult by dashboardStudentsViewModel.formCodeValidationResult.collectAsState()

    var formCodeInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(userIdentifier) {
        dashboardStudentsViewModel.loadStudentDashboardData(userIdentifier)
    }

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
            val studentName = (uiState as? StudentDashboardUiState.Success)?.student?.name ?: "Siswa"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
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
                        Text(text = "Hello, $studentName", color = Color.White, style = MaterialTheme.typography.titleMedium)
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

                // ===============================================
                // === SEARCH BAR DITAMBAHKAN KEMBALI DI SINI ===
                // ===============================================
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari formulir...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
                )
                // ===============================================

                Spacer(modifier = Modifier.height(16.dp))

                val isValidationLoading = formCodeValidationResult is FormCodeValidationResult.Loading

                // ===============================================
                // === PERBAIKAN LAYOUT ADA DI SINI ===
                // ===============================================
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), // <-- Tambahkan ini
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
                        onClick = {
                            dashboardStudentsViewModel.validateFormCode(formCodeInput)
                        },
                        enabled = !isValidationLoading,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isValidationLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Akses")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Riwayat Formulir Saya:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                when(val state = uiState) {
                    is StudentDashboardUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is StudentDashboardUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is StudentDashboardUiState.Success -> {
                        val forms = state.availableForms.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.description?.contains(searchQuery, ignoreCase = true) == true
                        }
                        if (forms.isEmpty()) {
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
                                items(forms, key = { it.id }) { form ->
                                    FormHistoryCard(
                                        title = form.title,
                                        description = form.description ?: "Tidak ada deskripsi",
                                        date = "Dibuat: ${formatApiDateTime(form.createdAt)}",
                                        onClick = { navController.navigate(Screen.PreviewForm.route.replace("{formId}", "${form.id}")) }
                                    )
                                }
                            }
                        }
                    }
                    else -> { /* State Idle */ }
                }
            }
        },
        bottomBar = {
            var selectedIndex by remember { mutableIntStateOf(0) }
            SimpleBottomNavigationBarStudents(
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it },
                navController = navController,
                userIdentifier = userIdentifier
            )
        }
    )
}

@Composable
fun FormHistoryCard(title: String, description: String, date: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

fun formatApiDateTime(dateTimeString: String?): String {
    if (dateTimeString.isNullOrBlank()) return "Tanggal tidak diketahui"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(dateTimeString)
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        date?.let { outputFormat.format(it) } ?: dateTimeString
    } catch (e: Exception) {
        dateTimeString
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreviewStudents() {
    EformTheme {
        val dummyNavController = rememberNavController()
        DashboardScreenStudents(navController = dummyNavController, userIdentifier = "student@example.com")
    }
}
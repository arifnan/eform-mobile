package com.example.eform.ui.form

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.model.api.FormApiModel
import com.example.eform.data.model.api.FormResponseApiModel
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.HistoryFormViewModel
import com.example.eform.ui.viewmodel.HistoryUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFormScreen(
    navController: NavController,
    userIdentifier: String, // Diterima dari NavHost, bisa digunakan untuk UI atau validasi awal
    historyFormViewModel: HistoryFormViewModel = viewModel(
        factory = HistoryFormViewModel.HistoryFormViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by historyFormViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { // Cukup panggil sekali saat screen dibuat
        historyFormViewModel.loadHistory()
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = "Riwayat", navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                HistoryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is HistoryUiState.SuccessForms -> {
                    if (state.forms.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Anda belum membuat formulir apapun.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.forms, key = { it.id }) { form ->
                                FormHistoryCard(
                                    title = form.title,
                                    description = "Kode: ${form.formCode}",
                                    date = "Dibuat: ${formatApiDateTime(form.createdAt)}",
                                    onClick = {
                                        navController.navigate(
                                            Screen.PreviewForm.route.replace("{formId}", "${form.id}")
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                is HistoryUiState.SuccessResponses -> {
                    if (state.responses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Anda belum mengisi formulir apapun.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(state.responses, key = { it.id }) { response ->
                                FormHistoryCard(
                                    title = response.form?.title ?: "Formulir Tanpa Judul",
                                    description = "Jawaban Anda",
                                    date = "Diisi: ${formatApiDateTime(response.submittedAt)}",
                                    onClick = {
                                        // Navigasi ke layar detail dengan mengirim ID respons
                                        navController.navigate(
                                            Screen.FormResponseDetail.route.replace("{responseId}", "${response.id}")
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                is HistoryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${state.message}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { historyFormViewModel.loadHistory() }) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                HistoryUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada riwayat untuk ditampilkan.")
                    }
                }
                HistoryUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Memuat riwayat...")
                    }
                }
            }
        }
    }
}

@Composable
fun FormHistoryCard(title: String, description: String, date: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
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
        val inputFormatWithMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        inputFormatWithMillis.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormatWithMillis.parse(dateTimeString)

        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        date?.let { outputFormat.format(it) } ?: dateTimeString
    } catch (e: Exception) {
        try {
            val inputFormatFallback = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormatFallback.parse(dateTimeString)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            date?.let { outputFormat.format(it) } ?: dateTimeString
        } catch (e2: Exception) {
            dateTimeString
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryFormScreenPreview() {
    EformTheme {
        HistoryFormScreen(
            navController = rememberNavController(),
            userIdentifier = "testUser" // Berikan userIdentifier dummy untuk preview
        )
    }
}

package com.example.eform.ui.response

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.formatApiDateTime // Pastikan fungsi ini bisa diakses atau pindahkan ke file utilitas
import com.example.eform.ui.viewmodel.ResponseListUiState
import com.example.eform.ui.viewmodel.ResponseListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponseListScreen(
    navController: NavController,
    formId: Int,
    formTitle: String,
    viewModel: ResponseListViewModel = viewModel(
        factory = ResponseListViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(formId) {
        viewModel.loadResponses(formId)
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = "Jawaban untuk: $formTitle", navController = navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is ResponseListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ResponseListUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ResponseListUiState.Empty -> {
                    Text(
                        text = "Belum ada siswa yang mengisi formulir ini.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ResponseListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.responses, key = { it.id }) { response ->
                            ResponseCard(
                                studentName = response.student?.name ?: "Siswa tidak dikenal",
                                submissionTime = formatApiDateTime(response.submittedAt),
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
        }
    }
}

@Composable
fun ResponseCard(studentName: String, submissionTime: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Mengisi pada: $submissionTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text("Lihat Detail >")
        }
    }
}
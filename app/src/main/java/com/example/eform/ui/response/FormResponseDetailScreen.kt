package com.example.eform.ui.response

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eform.data.model.api.AnswerApiModel
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.formatApiDateTime // Pastikan fungsi ini bisa diakses
import com.example.eform.ui.viewmodel.FormResponseDetailViewModel
import com.example.eform.ui.viewmodel.ResponseDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormResponseDetailScreen(
    navController: NavController,
    responseId: Int,
    viewModel: FormResponseDetailViewModel = viewModel(
        factory = FormResponseDetailViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(responseId) {
        viewModel.loadResponseDetail(responseId)
    }

    Scaffold(
        topBar = {
            val title = when (val state = uiState) {
                is ResponseDetailUiState.Success -> state.response.form?.title ?: "Detail Jawaban"
                else -> "Detail Jawaban"
            }
            StandardTopAppBar(title = title, navController = navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is ResponseDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ResponseDetailUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ResponseDetailUiState.Success -> {
                    val response = state.response
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Informasi Pengisi
                        Text("Diisi oleh: ${response.student?.name ?: "Siswa tidak dikenal"}", style = MaterialTheme.typography.titleLarge)
                        Text("Waktu: ${formatApiDateTime(response.submittedAt)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        // Foto Bukti
                        Text("Foto Bukti Pengisian:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!response.photoUrl.isNullOrEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = response.photoUrl),
                                contentDescription = "Foto Bukti Pengisian",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Text("Foto tidak tersedia.")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Peta Lokasi
                        Text("Lokasi Pengisian:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        val latitude = response.latitude
                        val longitude = response.longitude
                        if (latitude != null && longitude != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Lokasi Pengisian Formulir)")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: ActivityNotFoundException) {
                                            try {
                                                val genericMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude"))
                                                context.startActivity(genericMapIntent)
                                            } catch (e2: ActivityNotFoundException) {
                                                Toast.makeText(context, "Tidak ada aplikasi peta yang ditemukan.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Ikon Lokasi", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                    Text("Lat: $latitude", style = MaterialTheme.typography.bodyMedium)
                                    Text("Lng: $longitude", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("(Tekan untuk lihat di Peta)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text("Status Validasi Lokasi: ${if(response.isLocationValid) "Valid" else "Tidak Valid"}", style = MaterialTheme.typography.bodySmall, color = if(response.isLocationValid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                        } else {
                            Text("Data lokasi tidak tersedia untuk respons ini.")
                        }

                        Divider(modifier = Modifier.padding(vertical = 24.dp))

                        // Detail Jawaban
                        Text("Detail Jawaban:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        response.answers?.forEach { answer ->
                            AnswerDetailCard(answer)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnswerDetailCard(answer: AnswerApiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = answer.question?.questionText ?: "Pertanyaan tidak ditemukan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = answer.answerText?.replace(";;", ", ") ?: "Tidak dijawab",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
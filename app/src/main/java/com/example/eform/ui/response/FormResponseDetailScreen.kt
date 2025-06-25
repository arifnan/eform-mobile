package com.example.eform.ui.response

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.eform.data.model.api.AnswerApiModel
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.form.formatApiDateTime
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
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ResponseDetailUiState.Loading -> {
                    // --- Tampilan Shimmer Loading ---
                    LoadingShimmerEffect()
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconInfoRow(
                                icon = Icons.Default.Person,
                                text = response.student?.name ?: "Siswa tidak dikenal"
                            )
                            IconInfoRow(
                                icon = Icons.Default.CalendarMonth,
                                text = formatApiDateTime(response.submittedAt)
                            )
                        }

                        DetailSection(title = "Detail Jawaban") {
                            // --- Menambahkan penomoran ---
                            response.answers?.forEachIndexed { index, answer ->
                                AnswerDetailCard(
                                    answer = answer,
                                    questionNumber = index + 1 // Nomor dimulai dari 1
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        DetailSection(title = "Foto Bukti Pengisian") {
                            if (!response.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(response.photoUrl)
                                        .crossfade(true).build(),
                                    contentDescription = "Foto Bukti Pengisian",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Foto tidak tersedia.")
                                }
                            }
                        }

                        DetailSection(title = "Lokasi Pengisian") {
                            val latitude = response.latitude
                            val longitude = response.longitude
                            if (latitude != null && longitude != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Lokasi Pengisian Formulir)")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            try {
                                                context.startActivity(mapIntent)
                                            } catch (e: ActivityNotFoundException) {
                                                Toast.makeText(context, "Aplikasi peta tidak ditemukan.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = "Ikon Lokasi", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                                        Text("Lat: $latitude, Lng: $longitude", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("(Tekan untuk lihat di Peta)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                ValidationStatusChip(isValid = response.isLocationValid)

                            } else {
                                Text("Data lokasi tidak tersedia.")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Composable Baru dan yang Diperbarui ---

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Column(content = content)
    }
}

@Composable
fun IconInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ValidationStatusChip(isValid: Boolean) {
    val backgroundColor = if (isValid) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (isValid) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onErrorContainer
    val text = if (isValid) "Lokasi Valid" else "Lokasi Tidak Valid"

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
@Composable
fun AnswerDetailCard(answer: AnswerApiModel, questionNumber: Int) {
    val answerText = answer.answerText ?: "Tidak dijawab"
    val isImageUrl = answerText.startsWith("http://") || answerText.startsWith("https://")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // --- PERUBAHAN 1: Mengubah warna latar belakang kartu ---
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // --- PERUBAHAN 2: Mengubah warna teks agar kontras ---
                Text(
                    text = "$questionNumber.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary // Warna teks untuk latar primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = answer.question?.questionText ?: "Pertanyaan tidak ditemukan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary // Warna teks untuk latar primary
                )
            }

            // --- PERUBAHAN 3: Mengubah warna divider agar terlihat ---
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
            )

            if (isImageUrl) {
                // Untuk gambar, tidak perlu perubahan karena akan menutupi latar belakang
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(answerText)
                        .crossfade(true).build(),
                    contentDescription = "Jawaban gambar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // --- PERUBAHAN 4: Mengubah warna teks jawaban ---
                Text(
                    text = answerText.replace(";;", ", "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary, // Warna teks untuk latar primary
                    lineHeight = 22.sp
                )
            }
        }
    }
}
// --- Composable untuk Shimmer Effect ---
@Composable
fun LoadingShimmerEffect() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Shimmer for user info
        Row {
            Spacer(modifier = Modifier.size(24.dp).clip(CircleShape).shimmerEffect())
            Spacer(modifier = Modifier.width(8.dp))
            Spacer(modifier = Modifier.height(24.dp).width(200.dp).shimmerEffect())
        }
        Row {
            Spacer(modifier = Modifier.size(24.dp).clip(CircleShape).shimmerEffect())
            Spacer(modifier = Modifier.width(8.dp))
            Spacer(modifier = Modifier.height(24.dp).width(250.dp).shimmerEffect())
        }

        // Shimmer for answers section
        Spacer(modifier = Modifier.height(24.dp).width(150.dp).shimmerEffect())
        repeat(3) {
            Spacer(modifier = Modifier.fillMaxWidth().height(120.dp).shimmerEffect())
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Shimmer for photo section
        Spacer(modifier = Modifier.height(24.dp).width(180.dp).shimmerEffect())
        Spacer(modifier = Modifier.fillMaxWidth().height(250.dp).shimmerEffect())
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "Shimmer Float"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.9f),
            Color.LightGray.copy(alpha = 0.4f),
            Color.LightGray.copy(alpha = 0.9f)
        ),
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    background(brush)
}
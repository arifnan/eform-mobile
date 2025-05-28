package com.example.eform.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eform.data.model.api.FormApiModel // <<< --- IMPORT FormApiModel
import com.example.eform.ui.theme.EformTheme

@Composable
fun FormCardItem(
    form: FormApiModel, // <<<--- PARAMETER SEKARANG BERNAMA 'form' DAN BERTIPE 'FormApiModel'
    isFavorite: Boolean,
    onFormClick: (Int) -> Unit,
    onToggleFavorite: (formId: Int, newFavoriteStatus: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryCardColor = Color(0xFF3D1860)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onFormClick(form.id) }, // Menggunakan form.id dari FormApiModel
        colors = CardDefaults.cardColors(containerColor = primaryCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = form.title, // Menggunakan form.title dari FormApiModel
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                // Deskripsi dari FormApiModel bisa null
                if (!form.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = form.description, // Menggunakan form.description dari FormApiModel
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            IconButton(
                onClick = { onToggleFavorite(form.id, !isFavorite) }, // Menggunakan form.id
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                    tint = if (isFavorite) Color(0xFFFF80AB) else Color.White
                )
            }
        }
    }
}

//// --- PREVIEW COMPOSABLE (Disesuaikan dengan FormApiModel) ---
//@Preview(name = "Form Card Item - Not Favorite", showBackground = true, widthDp = 200)
//@Composable
//fun FormCardItemPreviewNotFavorite() {
//    EformTheme {
//        var isFavoriteState by remember { mutableStateOf(false) }
//        FormCardItem(
//            form = FormApiModel( // Menggunakan FormApiModel untuk data preview
//                id = 1,
//                title = "Judul Formulir Contoh API",
//                description = "Ini adalah deskripsi singkat dari API.",
//                formCode = "API123",
//                teacherId = 1,
//                createdAt = "2023-01-01T10:00:00Z",
//                updatedAt = "2023-01-01T10:00:00Z"
//            ),
//            isFavorite = isFavoriteState,
//            onFormClick = { /* Aksi klik form */ },
//            onToggleFavorite = { _, newStatus -> isFavoriteState = newStatus }
//        )
//    }
//}
//
//@Preview(name = "Form Card Item - Favorite", showBackground = true, widthDp = 200)
//@Composable
//fun FormCardItemPreviewFavorite() {
//    EformTheme {
//        var isFavoriteState by remember { mutableStateOf(true) }
//        FormCardItem(
//            form = FormApiModel(
//                id = 2,
//                title = "Formulir Favorit API Saya",
//                description = "Deskripsi untuk formulir favorit dari API.",
//                formCode = "APIF456",
//                teacherId = 1,
//                createdAt = "2023-01-01T10:00:00Z",
//                updatedAt = "2023-01-01T10:00:00Z"
//            ),
//            isFavorite = isFavoriteState,
//            onFormClick = {},
//            onToggleFavorite = { _, newStatus -> isFavoriteState = newStatus }
//        )
//    }
//}

// ... (Preview lainnya bisa Anda sesuaikan dengan FormApiModel juga) ...
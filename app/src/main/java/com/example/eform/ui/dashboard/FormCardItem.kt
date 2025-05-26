package com.example.eform.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import mutableStateOf dan remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview // Import Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eform.data.model.FormEntity
import com.example.eform.ui.theme.EformTheme // Import tema Anda

@Composable
fun FormCardItem(
    form: FormEntity,
    isFavorite: Boolean,
    onFormClick: (Int) -> Unit,
    onToggleFavorite: (formId: Int, newFavoriteStatus: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryCardColor = Color(0xFF3D1860) // Warna primer Anda untuk background card

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.2f) // Atau rasio lain yang Anda inginkan
            .clip(RoundedCornerShape(12.dp))
            .clickable { onFormClick(form.id) },
        colors = CardDefaults.cardColors(containerColor = primaryCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) { // Box untuk menampung konten dan ikon favorit
            // Kolom untuk Judul dan Deskripsi, diposisikan di tengah
            Column(
                modifier = Modifier
                    .fillMaxSize() // Mengisi seluruh ruang Card
                    .padding(12.dp), // Padding internal
                verticalArrangement = Arrangement.Center, // Tengah secara vertikal
                horizontalAlignment = Alignment.CenterHorizontally // Tengah secara horizontal
            ) {
                Text(
                    text = form.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center // Teks judul di tengah
                )
                if (form.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = form.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center // Teks deskripsi di tengah
                    )
                }
            }

            // Tombol Favorit tetap di pojok kanan atas
            IconButton(
                onClick = { onToggleFavorite(form.id, !isFavorite) },
                modifier = Modifier
                    .align(Alignment.TopEnd) // Posisikan di pojok kanan atas Box
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                    tint = if (isFavorite) Color(0xFFFF80AB) else Color.White // Warna hati berbeda saat favorit
                )
            }
        }
    }
}

// --- PREVIEW COMPOSABLE ---
@Preview(name = "Form Card Item - Not Favorite", showBackground = true, widthDp = 200)
@Composable
fun FormCardItemPreviewNotFavorite() {
    EformTheme { // Bungkus dengan tema aplikasi Anda
        // State untuk isFavorite agar bisa diubah saat preview
        var isFavoriteState by remember { mutableStateOf(false) }
        FormCardItem(
            form = FormEntity(
                id = 1,
                title = "Judul Formulir Contoh",
                description = "Ini adalah deskripsi singkat untuk formulir contoh.",
                formCode = "ABC123X"
            ),
            isFavorite = isFavoriteState,
            onFormClick = { /* Aksi klik form */ },
            onToggleFavorite = { _, newStatus -> isFavoriteState = newStatus }
        )
    }
}

@Preview(name = "Form Card Item - Favorite", showBackground = true, widthDp = 200)
@Composable
fun FormCardItemPreviewFavorite() {
    EformTheme {
        var isFavoriteState by remember { mutableStateOf(true) }
        FormCardItem(
            form = FormEntity(
                id = 2,
                title = "Formulir Favorit Saya",
                description = "Deskripsi untuk formulir yang sudah difavoritkan.",
                formCode = "FAV456"
            ),
            isFavorite = isFavoriteState,
            onFormClick = {},
            onToggleFavorite = { _, newStatus -> isFavoriteState = newStatus }
        )
    }
}

@Preview(name = "Form Card Item - No Description", showBackground = true, widthDp = 200)
@Composable
fun FormCardItemPreviewNoDescription() {
    EformTheme {
        var isFavoriteState by remember { mutableStateOf(false) }
        FormCardItem(
            form = FormEntity(
                id = 3,
                title = "Form Tanpa Deskripsi",
                description = "", // Deskripsi kosong
                formCode = "NODESC"
            ),
            isFavorite = isFavoriteState,
            onFormClick = {},
            onToggleFavorite = { _, newStatus -> isFavoriteState = newStatus }
        )
    }
}

@Preview(name = "Form Card Item - Long Title", showBackground = true, widthDp = 200)
@Composable
fun FormCardItemPreviewLongTitle() {
    EformTheme {
        var isFavoriteState by remember { mutableStateOf(false) }
        FormCardItem(
            form = FormEntity(
                id = 4,
                title = "Ini Adalah Contoh Judul Formulir yang Sangat Panjang Sekali Melebihi Batas",
                description = "Deskripsi pendek.",
                formCode = "LONGT"
            ),
            isFavorite = isFavoriteState,
            onFormClick = {},
            onToggleFavorite = { _, newStatus -> isFavoriteState = newStatus }
        )
    }
}

@Preview(name = "Form Card Item - Long Description", showBackground = true, widthDp = 200)
@Composable
fun FormCardItemPreviewLongDescription() {
    EformTheme {
        var isFavoriteState by remember { mutableStateOf(true) }
        FormCardItem(
            form = FormEntity(
                id = 5,
                title = "Deskripsi Panjang",
                description = "Ini adalah contoh deskripsi formulir yang sangat panjang sekali dan seharusnya akan terpotong atau ditampilkan dalam beberapa baris hingga batas maksimum yang telah ditentukan pada komponen teks.",
                formCode = "LONGD"
            ),
            isFavorite = isFavoriteState,
            onFormClick = {},
            onToggleFavorite = { _, newStatus -> isFavoriteState = newStatus }
        )
    }
}

// Tambahkan Preview untuk melihat beberapa kartu dalam sebuah Column
@Preview(name = "Multiple Form Cards", showBackground = true)
@Composable
fun MultipleFormCardsPreview() {
    EformTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // State terpisah untuk setiap kartu di preview multipel
            var fav1 by remember { mutableStateOf(false) }
            var fav2 by remember { mutableStateOf(true) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    FormCardItem(
                        form = FormEntity(id = 10, title = "Form A", description = "Deskripsi A", formCode = "FA"),
                        isFavorite = fav1,
                        onFormClick = {},
                        onToggleFavorite = {_, new -> fav1 = new}
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    FormCardItem(
                        form = FormEntity(id = 11, title = "Form B (Favorit)", description = "Deskripsi B yang lebih panjang sedikit dari biasanya.", formCode = "FB"),
                        isFavorite = fav2,
                        onFormClick = {},
                        onToggleFavorite = {_, new -> fav2 = new}
                    )
                }
            }
        }
    }
}
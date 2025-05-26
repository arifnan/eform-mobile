package com.example.eform.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTopAppBar(
    title: String,
    navController: NavController,
    showBackButton: Boolean = true, // Tambahkan parameter untuk menampilkan tombol kembali atau tidak
    onBackClicked: (() -> Unit)? = null // Aksi kustom saat tombol kembali ditekan
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 22.sp, // Sedikit sesuaikan ukuran font jika perlu
                fontWeight = FontWeight.Bold,
                color = Color.White // Warna teks judul
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = {
                    if (onBackClicked != null) {
                        onBackClicked()
                    } else {
                        navController.popBackStack() // Aksi default tombol kembali
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White // Warna ikon kembali
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary // Warna background TopAppBar
        )
    )
}
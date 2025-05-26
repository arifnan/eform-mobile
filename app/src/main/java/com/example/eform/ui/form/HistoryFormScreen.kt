package com.example.eform.ui.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.input.pointer.HistoricalChange // Tidak terpakai, bisa dihapus
import androidx.navigation.NavController
import com.example.eform.ui.components.StandardTopAppBar // <<< --- IMPORT StandardTopAppBar

@Composable
fun HistoryFormScreen(navController: NavController){
    Scaffold( // <<< --- GUNAKAN SCAFFOLD
        topBar = {
            StandardTopAppBar(title = "Riwayat Formulir", navController = navController)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) { // Gunakan Column untuk padding
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Riwayat Formulir Anda Akan Tampil di Sini") // Konten placeholder
            }
        }
    }
}
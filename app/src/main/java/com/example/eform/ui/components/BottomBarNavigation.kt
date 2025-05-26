package com.example.eform.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.unit.dp // Tidak terpakai langsung, bisa dihapus jika tidak ada Modifier.padding(dp)
import androidx.navigation.NavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.ui.dashboard.DashboardScreen
import com.example.eform.ui.profile.ProfileScreen
import com.example.eform.ui.form.CreateFormScreen
import com.example.eform.ui.form.FavoriteFormsScreen
import com.example.eform.ui.form.HistoryFormScreen


@Composable
fun BottomBarNavigation(navController: NavController, userIdentifier: String) {
    var selectedIndex by remember { mutableStateOf(0) } // Indeks default adalah 0 (Home)

    val context = LocalContext.current
    val userDao = AppDatabase.getDatabase(context).userDao()
    // val userEmail = "nanda@email.com" // Ini sudah benar dihapus/dikomentari

    Box(modifier = Modifier.fillMaxSize()) {
        // Menampilkan konten layar berdasarkan selectedIndex
        when (BottomBarItem.entries.getOrNull(selectedIndex)) { // Menggunakan getOrNull untuk keamanan
            BottomBarItem.Home -> DashboardScreen(navController = navController, userIdentifier = userIdentifier)
            BottomBarItem.FavoriteForms -> FavoriteFormsScreen(navController, userIdentifier = userIdentifier) // TODO: Tambahkan userIdentifier jika perlu
            BottomBarItem.Add -> CreateFormScreen(navController,userIdentifier)
            BottomBarItem.History -> HistoryFormScreen(navController) // TODO: Tambahkan userIdentifier jika perlu
            BottomBarItem.Profile -> ProfileScreen(
                navController = navController,
                userDao = userDao,
                userIdentifier = userIdentifier // Sudah benar menggunakan userIdentifier
            )
            null -> { /* Handle kasus indeks di luar rentang jika perlu, misal default ke Home */
                DashboardScreen(navController = navController, userIdentifier = userIdentifier)
            }
        }

        // Menempatkan BottomNavigationBar di bagian bawah
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
            // .padding(16.dp) // Umumnya, bottom bar tidak butuh padding tambahan dari parent Box-nya
        ) {
            // Asumsi SimpleBottomNavigationBar sudah didefinisikan untuk menerima userIdentifier
            SimpleBottomNavigationBar(
                selectedIndex = selectedIndex,
                onItemSelected = { newIndex -> selectedIndex = newIndex },
                navController = navController,
                userIdentifier = userIdentifier // Sudah benar mengirimkan userIdentifier
            )
        }
    }
}
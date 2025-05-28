package com.example.eform.ui.form

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.dashboard.FormCardItem // FormCardItem sekarang akan menerima FormApiModel
import com.example.eform.ui.viewmodel.FavoriteFormsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteFormsScreen(
    navController: NavController,
    userIdentifier: String,
    favoriteFormsViewModel: FavoriteFormsViewModel = viewModel(
        factory = FavoriteFormsViewModel.FavoriteFormsViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val favoriteFormsApiList by favoriteFormsViewModel.favoriteFormsApi.collectAsState() // <<< Gunakan StateFlow baru
    val isLoading by favoriteFormsViewModel.isLoading.collectAsState()

    LaunchedEffect(userIdentifier) {
        favoriteFormsViewModel.loadFavoriteFormsAndSetIdentifier(userIdentifier) // Gunakan fungsi baru
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = "Formulir Favorit", navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (favoriteFormsApiList.isEmpty()) { // <<< Cek list baru
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Anda belum memiliki formulir favorit.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favoriteFormsApiList, key = { it.id }) { formApiModel -> // <<< Iterasi list baru
                        FormCardItem( // FormCardItem sekarang menerima FormApiModel
                            form = formApiModel, // <<< Kirim FormApiModel
                            isFavorite = true, // Semua di sini pasti favorit
                            onFormClick = { formId ->
                                navController.navigate(Screen.PreviewForm.route.replace("{formId}", "$formId"))
                            },
                            onToggleFavorite = { formId, newFavoriteStatus ->
                                favoriteFormsViewModel.toggleFavoriteStatus(formId, newFavoriteStatus)
                            }
                        )
                    }
                }
            }
        }
    }
}

//lokal
//import android.app.Application // Diperlukan untuk ViewModel Factory
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.grid.items
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.MaterialTheme // Import MaterialTheme
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel // Import viewModel
//import androidx.navigation.NavController
//import com.example.eform.data.model.FormEntity // ViewModel kita masih menggunakan FormEntity untuk Favorite
//import com.example.eform.navigation.Screen
//import com.example.eform.ui.components.StandardTopAppBar
//import com.example.eform.ui.dashboard.FormCardItem // Untuk menampilkan kartu
//import com.example.eform.ui.viewmodel.FavoriteFormsViewModel // Import ViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FavoriteFormsScreen(
//    navController: NavController,
//    userIdentifier: String,
//    favoriteFormsViewModel: FavoriteFormsViewModel = viewModel(
//        factory = FavoriteFormsViewModel.FavoriteFormsViewModelFactory(
//            LocalContext.current.applicationContext as Application
//        )
//    )
//) {
//    // val context = LocalContext.current // Tidak digunakan langsung
//    // val db = AppDatabase.getDatabase(context) // Tidak lagi diakses langsung
//    // val formDao = db.formDao()
//    // val userDao = db.userDao()
//    // val userFavoriteFormDao = db.userFavoriteFormDao()
//    // val coroutineScope = rememberCoroutineScope() // Digantikan viewModelScope
//
//    val favoriteFormsApiList by favoriteFormsViewModel.favoriteFormsApi.collectAsState() // <<< Gunakan StateFlow baru
//    val isLoading by favoriteFormsViewModel.isLoading.collectAsState()
//
//    LaunchedEffect(userIdentifier) {
//        favoriteFormsViewModel.loadFavoriteFormsAndSetIdentifier(userIdentifier) // Gunakan fungsi baru
//    }
//
//    Scaffold(
//        topBar = {
//            StandardTopAppBar(title = "Formulir Favorit", navController = navController)
//        }
//    ) { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .padding(16.dp)
//                .background(MaterialTheme.colorScheme.background)
//        ) {
//            if (isLoading) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator()
//                }
//            } else if (favoriteFormsApiList.isEmpty()) { // <<< Cek list baru
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Text("Anda belum memiliki formulir favorit.", style = MaterialTheme.typography.bodyLarge)
//                }
//            } else {
//                LazyVerticalGrid(
//                    columns = GridCells.Fixed(2),
//                    contentPadding = PaddingValues(vertical = 8.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp),
//                    modifier = Modifier.fillMaxSize()
//                ) {
//                    items(favoriteFormsList, key = { it.id }) { formEntity ->
//                        // FormCardItem saat ini menerima FormApiModel, jadi perlu konversi atau
//                        // FavoriteFormsViewModel mengembalikan FormApiModel, atau FormCardItem diubah.
//                        // Untuk saat ini, kita asumsikan FormCardItem dimodifikasi untuk bisa menerima FormEntity juga
//                        // atau kita buat Composable Card khusus untuk layar favorit.
//                        // Pilihan: Ubah FormCardItem untuk menerima FormEntity jika data dari Room
//                        // Atau, FavoriteFormsViewModel mengambil FormApiModel jika favorit dari server
//                        // Karena FavoriteFormsViewModel saat ini mengambil FormEntity dari Room (berdasarkan query FormDao),
//                        // maka FormCardItem perlu menerima FormEntity.
//                        FormCardItem( // Pastikan FormCardItem bisa menerima FormEntity
//                            form = formEntity, // Ini adalah FormEntity
//                            isFavorite = true, // Semua di sini pasti favorit
//                            onFormClick = { formId ->
//                                navController.navigate(Screen.PreviewForm.route.replace("{formId}", "$formId"))
//                            },
//                            onToggleFavorite = { formId, newFavoriteStatus ->
//                                // Di layar favorit, toggle berarti menghapus dari favorit
//                                favoriteFormsViewModel.toggleFavoriteStatus(formId, newFavoriteStatus)
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
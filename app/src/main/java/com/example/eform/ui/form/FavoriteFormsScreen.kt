package com.example.eform.ui.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.UserFavoriteFormEntity
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.dashboard.FormCardItem // Gunakan FormCardItem yang sama
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FavoriteFormsScreen(navController: NavController, userIdentifier: String) { // <<< --- TAMBAHKAN userIdentifier
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val formDao = db.formDao()
    val userDao = db.userDao()
    val userFavoriteFormDao = db.userFavoriteFormDao()
    val coroutineScope = rememberCoroutineScope()

    var favoriteFormsList by remember { mutableStateOf<List<FormEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUserId by remember { mutableStateOf<Int?>(null) }


    suspend fun getUserIdFromIdentifier(identifier: String): Int? {
        val user = userDao.getUserByNip(identifier) // Asumsi guru menggunakan NIP
        return user?.id
    }

    fun fetchFavoriteFormsForUser() {
        coroutineScope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                val userId = currentUserId ?: getUserIdFromIdentifier(userIdentifier)
                if (userId != null) {
                    currentUserId = userId
                    favoriteFormsList = formDao.getFavoriteFormsByUserId(userId)
                } else {
                    favoriteFormsList = emptyList() // Handle jika user tidak ditemukan
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(userIdentifier) { // Re-fetch jika userIdentifier berubah atau saat pertama kali
        withContext(Dispatchers.IO){
            currentUserId = getUserIdFromIdentifier(userIdentifier)
        }
        fetchFavoriteFormsForUser()
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
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (favoriteFormsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Anda belum memiliki formulir favorit.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favoriteFormsList, key = { it.id }) { formEntity ->
                        FormCardItem(
                            form = formEntity,
                            isFavorite = true, // Semua di sini pasti favorit
                            onFormClick = { formId ->
                                navController.navigate(Screen.PreviewForm.route.replace("{formId}", "$formId"))
                            },
                            onToggleFavorite = { formId, newFavoriteStatus -> // Akan selalu false di sini
                                coroutineScope.launch(Dispatchers.IO) {
                                    currentUserId?.let { userId ->
                                        if (newFavoriteStatus) { // Seharusnya tidak terjadi dari layar ini
                                            userFavoriteFormDao.addFavorite(UserFavoriteFormEntity(userId, formId))
                                        } else {
                                            userFavoriteFormDao.removeFavorite(UserFavoriteFormEntity(userId, formId))
                                        }
                                        withContext(Dispatchers.Main){
                                            fetchFavoriteFormsForUser() // Refresh daftar
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
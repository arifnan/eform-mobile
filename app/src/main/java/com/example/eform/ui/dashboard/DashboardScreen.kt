package com.example.eform.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.UserFavoriteFormEntity
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.BottomBarItem
import com.example.eform.ui.components.SimpleBottomNavigationBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FormDisplayItem(
    val formEntity: FormEntity,
    var isFavorite: Boolean
)

@Composable
fun DashboardScreen(navController: NavController, userIdentifier: String) {
    var searchQuery by remember { mutableStateOf("") }
    // Gunakan List<FormDisplayItem> untuk menyimpan status favorit
    var formsToDisplay by remember { mutableStateOf<List<FormDisplayItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var sortOrder by remember { mutableStateOf("asc") }
    var userName by remember { mutableStateOf("Guru") }
    var currentUserId by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val formDao = db.formDao()
    val userDao = db.userDao()
    val userFavoriteFormDao = db.userFavoriteFormDao()
    val coroutineScope = rememberCoroutineScope()

    suspend fun getUserIdFromIdentifier(identifier: String): Int? {
        // Asumsi guru menggunakan NIP sebagai identifier
        val user = userDao.getUserByNip(identifier)
        return user?.id
    }

    fun fetchFormsAndFavorites() {
        coroutineScope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                val userId = currentUserId ?: getUserIdFromIdentifier(userIdentifier)
                if (userId == null) {
                    // Handle kasus userId tidak ditemukan (seharusnya tidak terjadi jika login benar)
                    isLoading = false
                    formsToDisplay = emptyList()
                    return@withContext
                }
                currentUserId = userId // Simpan userId untuk penggunaan selanjutnya

                val user = userDao.getUserByNip(userIdentifier) // atau getUserByEmail jika identifier adalah email
                userName = user?.name ?: "Guru"

                val allForms = formDao.getAllForms()
                val favoriteFormIds = userFavoriteFormDao.getFavoriteFormIdsByUserId(userId).toSet()

                val displayList = allForms.map { formEntity ->
                    FormDisplayItem(
                        formEntity = formEntity,
                        isFavorite = favoriteFormIds.contains(formEntity.id)
                    )
                }
                formsToDisplay = if (sortOrder == "asc") {
                    displayList.sortedBy { it.formEntity.title.lowercase() }
                } else {
                    displayList.sortedByDescending { it.formEntity.title.lowercase() }
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(userIdentifier, sortOrder) {
        // Dapatkan userId dulu sebelum fetch forms
        withContext(Dispatchers.IO) {
            currentUserId = getUserIdFromIdentifier(userIdentifier)
        }
        fetchFormsAndFavorites()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                navController.navigate(Screen.Profile.route.replace("{userIdentifier}", userIdentifier))
                            }
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(text = "Hello, $userName",color= Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Welcome back",color= Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifikasi",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            navController.navigate(Screen.Notification.route.replace("{userIdentifier}", userIdentifier))
                        }
                )
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari formulir Anda...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = {
                    sortOrder = if (sortOrder == "asc") "desc" else "asc"
                }) {
                    Text("Sort by Title (${if (sortOrder == "asc") "A-Z" else "Z-A"})")
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredList = formsToDisplay.filter {
                        it.formEntity.title.contains(searchQuery, ignoreCase = true) ||
                                it.formEntity.description.contains(searchQuery, ignoreCase = true)
                    }
                    if (filteredList.isEmpty()){
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if(searchQuery.isNotBlank()) "Tidak ada formulir ditemukan." else "Anda belum membuat formulir.")
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredList, key = { it.formEntity.id }) { formDisplayItem ->
                                FormCardItem( // Menggunakan FormCardItem yang sama
                                    form = formDisplayItem.formEntity, // Berikan FormEntity
                                    isFavorite = formDisplayItem.isFavorite, // Berikan status favorit
                                    onFormClick = { formId ->
                                        navController.navigate(Screen.PreviewForm.route.replace("{formId}", "$formId"))
                                    },
                                    onToggleFavorite = { formId, newFavoriteStatus ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            currentUserId?.let { userId ->
                                                if (newFavoriteStatus) {
                                                    userFavoriteFormDao.addFavorite(UserFavoriteFormEntity(userId, formId))
                                                } else {
                                                    userFavoriteFormDao.removeFavorite(UserFavoriteFormEntity(userId, formId))
                                                }
                                                // Update UI dengan memuat ulang data
                                                withContext(Dispatchers.Main){
                                                    fetchFormsAndFavorites()
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
        },
        bottomBar = {
            SimpleBottomNavigationBar(
                selectedIndex = 0,
                onItemSelected = { index ->
                    val item = BottomBarItem.values()[index]
                    val targetRoute = when (item) {
                        BottomBarItem.Home -> userIdentifier.let { Screen.Dashboard.route.replace("{userIdentifier}", it) }
                        BottomBarItem.FavoriteForms -> Screen.FavoriteForms.route // Navigasi ke FavoriteForms
                        BottomBarItem.Add -> Screen.CreateForm.route
                        BottomBarItem.History -> Screen.HistoryForm.route
                        BottomBarItem.Profile -> userIdentifier.let { Screen.Profile.route.replace("{userIdentifier}", it) }
                    }
                    if (navController.currentDestination?.route != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                navController = navController,
                userIdentifier = userIdentifier
            )
        }
    )
}

// Preview tetap, tapi FormCardItem perlu diupdate juga untuk Preview jika ingin menampilkan favorit
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    val dummyNavController = rememberNavController()
    DashboardScreen(navController = dummyNavController, userIdentifier = "dummyNip123")
}
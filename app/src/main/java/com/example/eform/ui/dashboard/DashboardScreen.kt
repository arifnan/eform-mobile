package com.example.eform.ui.dashboard

import android.app.Application // Diperlukan untuk ViewModel Factory
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.BottomBarItem // <<< Gunakan BottomBarItem yang benar
import com.example.eform.ui.components.SimpleBottomNavigationBar
import com.example.eform.ui.theme.EformTheme // Import tema untuk Preview
import com.example.eform.ui.viewmodel.DashboardFormsResult
import com.example.eform.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    userIdentifier: String, // NIP Guru
    dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.DashboardViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current // Masih bisa berguna untuk Toast atau resource lain
    val formsResultState by dashboardViewModel.formsResult.collectAsState()
    val userName by dashboardViewModel.userName.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    // sortOrder sekarang dikelola oleh ViewModel atau diteruskan ke loadTeacherDashboard
    var sortOrderState by remember { mutableStateOf("asc") }


    LaunchedEffect(userIdentifier, sortOrderState) {
        dashboardViewModel.loadTeacherDashboard(userIdentifier, sortOrderState)
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
                        Text(text = "Hello, $userName", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Welcome back", color = Color.White, style = MaterialTheme.typography.bodySmall)
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
                    sortOrderState = if (sortOrderState == "asc") "desc" else "asc"
                    // LaunchedEffect akan memanggil dashboardViewModel.loadTeacherDashboard dengan sortOrderState baru
                }) {
                    Text("Sort by Title (${if (sortOrderState == "asc") "A-Z" else "Z-A"})")
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (val result = formsResultState) {
                    is DashboardFormsResult.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is DashboardFormsResult.Success -> {
                        val displayableForms = result.forms.filter {
                            it.formApiData.title.contains(searchQuery, ignoreCase = true) ||
                                    it.formApiData.description?.contains(searchQuery, ignoreCase = true) == true
                        }
                        if (displayableForms.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (searchQuery.isNotBlank()) "Tidak ada formulir ditemukan." else "Anda belum membuat formulir.")
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(displayableForms, key = { it.formApiData.id }) { formDisplayItem ->
                                    FormCardItem(
                                        form = formDisplayItem.formApiData, // Berikan FormApiModel
                                        isFavorite = formDisplayItem.isUserFavorite,
                                        onFormClick = { formId ->
                                            navController.navigate(Screen.PreviewForm.route.replace("{formId}", "$formId"))
                                        },
                                        onToggleFavorite = { formId, newFavoriteStatus ->
                                            dashboardViewModel.toggleFavoriteStatus(formId, newFavoriteStatus)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is DashboardFormsResult.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${result.message}")
                            // Tambahkan tombol Retry jika perlu
                        }
                    }
                }
            }
        },
        bottomBar = {
            // State selectedIndex untuk bottom bar dikelola di sini
            var selectedIndex by remember { mutableStateOf(0) }
            SimpleBottomNavigationBar(
                selectedIndex = selectedIndex,
                onItemSelected = { newIndex ->
                    selectedIndex = newIndex // Update state selectedIndex
                    // Gunakan BottomBarItem yang benar (untuk guru)
                    val item = BottomBarItem.values()[newIndex]
                    val currentRoute = navController.currentBackStackEntry?.destination?.route

                    val targetRoute = when (item) {
                        BottomBarItem.Home -> Screen.Dashboard.route.replace("{userIdentifier}", userIdentifier)
                        BottomBarItem.FavoriteForms -> Screen.FavoriteForms.route.replace("{userIdentifier}", userIdentifier)
                        BottomBarItem.Add -> Screen.CreateForm.route.replace("{userIdentifier}", userIdentifier)
                        BottomBarItem.History -> Screen.HistoryForm.route // Tambahkan userIdentifier jika perlu
                        BottomBarItem.Profile -> Screen.Profile.route.replace("{userIdentifier}", userIdentifier)
                        // Tidak perlu 'else' jika semua kasus enum sudah ditangani
                    }

                    // Hanya navigasi jika target rute berbeda dengan rute saat ini
                    if (currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() { // Ganti nama Preview agar unik jika ada DashboardPreviewStudents
    EformTheme {
        val dummyNavController = rememberNavController()
        DashboardScreen(
            navController = dummyNavController,
            userIdentifier = "dummyNip123"
            // ViewModel akan di-provide oleh default factory di preview jika tidak ada factory khusus preview
        )
    }
}
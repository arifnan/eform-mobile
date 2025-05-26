package com.example.eform.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.eform.R
import com.example.eform.navigation.Screen // Pastikan Screen diimpor

// Enum BottomBarItem tetap sama
enum class BottomBarItem(val icon: ImageVector, val label: String) {
    Home(Icons.Default.Home, "Beranda"),
    FavoriteForms(Icons.Filled.Favorite, "Favorit"),
    Add(Icons.Default.AddCircleOutline, "Tambah"), // Menggunakan AddCircleOutline atau Add
    History(Icons.Default.History, "Riwayat"),
    Profile(Icons.Default.Person, "Akun")
}

@Composable
fun SimpleBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    navController: NavController,
    userIdentifier: String? // userIdentifier (NIP guru)
) {
    NavigationBar(
        modifier = Modifier.height(70.dp),
        containerColor = colorResource(id = R.color.primary)
    ) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        BottomBarItem.values().forEachIndexed { index, item ->
            // Tentukan targetRoute berdasarkan item BottomBarItem
            val targetRoute = when (item) {
                BottomBarItem.Home -> userIdentifier?.let { Screen.Dashboard.route.replace("{userIdentifier}", it) }
                BottomBarItem.FavoriteForms -> userIdentifier?.let { Screen.FavoriteForms.route.replace("{userIdentifier}", it) }
                // >>> PERBAIKAN UTAMA DI SINI untuk BottomBarItem.Add <<<
                BottomBarItem.Add -> userIdentifier?.let { Screen.CreateForm.route.replace("{userIdentifier}", it) }
                BottomBarItem.History -> Screen.HistoryForm.route // Jika HistoryForm butuh userIdentifier, tambahkan juga
                BottomBarItem.Profile -> userIdentifier?.let { Screen.Profile.route.replace("{userIdentifier}", it) }
            }
            val isSelected = selectedIndex == index || (currentRoute == targetRoute && targetRoute != null)


            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label, tint = if (isSelected) Color.White else Color.LightGray) },
                label = { Text(item.label, color = if (isSelected) Color.White else Color.LightGray) },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.LightGray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.LightGray,
                    indicatorColor = colorResource(id = R.color.primary).copy(alpha = 0.5f)
                ),
                onClick = {
                    onItemSelected(index)
                    if (targetRoute != null && currentRoute != targetRoute) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSimpleBottomNavigationBar() {
    val dummyNavController = rememberNavController()
    SimpleBottomNavigationBar(
        selectedIndex = 0,
        onItemSelected = { },
        navController = dummyNavController,
        userIdentifier = "guruNip123"
    )
}
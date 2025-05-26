package com.example.eform.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Pastikan Favorite diimpor (Filled.Favorite atau Outlined.Favorite)
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
import com.example.eform.navigation.Screen // Pastikan Screen diimpor dengan benar

// Enum untuk Bottom Bar Students
enum class BottomBarItemStudents(val icon: ImageVector, val label: String) {
    Home(Icons.Default.Home, "Beranda"),
    FavoriteForms(Icons.Filled.Favorite, "Favorit"), // Nama enum sudah benar di sini
    History(Icons.Default.History, "Riwayat"), // Anda mungkin ingin menambahkan ini kembali jika terhapus
    Profile(Icons.Default.Person, "Akun")
}

@Composable
fun SimpleBottomNavigationBarStudents(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    navController: NavController,
    userIdentifier: String?
) {
    NavigationBar(
        modifier = Modifier.height(70.dp),
        containerColor = colorResource(id = R.color.primary)
    ) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        BottomBarItemStudents.values().forEachIndexed { index, item ->
            // Tentukan targetRoute berdasarkan item BottomBarItemStudents
            val targetRoute = when (item) { // 'item' di sini adalah tipe BottomBarItemStudents
                BottomBarItemStudents.Home -> userIdentifier?.let { Screen.DashboardStudents.route.replace("{userIdentifier}", it) }
                // >>> PERBAIKAN UTAMA DI SINI <<<
                // Gunakan BottomBarItemStudents.FavoriteForms
                BottomBarItemStudents.FavoriteForms -> userIdentifier?.let { Screen.FavoriteForms.route.replace("{userIdentifier}", it) } // Mengirim email siswa
                BottomBarItemStudents.History -> Screen.HistoryForm.route // Jika HistoryForm tidak butuh userIdentifier
                // Jika HistoryForm butuh userIdentifier:
                // BottomBarItemStudents.History -> userIdentifier?.let { Screen.HistoryForm.route.replace("{userIdentifier}", it) }
                BottomBarItemStudents.Profile -> userIdentifier?.let { Screen.Profile.route.replace("{userIdentifier}", it) }
            }
            val isSelected = selectedIndex == index // Anda bisa juga membandingkan currentRoute dengan targetRoute jika lebih disukai

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
                    // Hanya navigasi jika targetRoute tidak null dan berbeda dari rute saat ini
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
fun PreviewSimpleBottomNavigationBarStudents() {
    val dummyNavController = rememberNavController()
    SimpleBottomNavigationBarStudents(
        selectedIndex = 0,
        onItemSelected = { },
        navController = dummyNavController,
        userIdentifier = "student@example.com"
    )
}
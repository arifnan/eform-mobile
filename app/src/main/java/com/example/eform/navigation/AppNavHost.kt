package com.example.eform.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.FormEntity
import com.example.eform.ui.auth.OnboardingScreen
import com.example.eform.ui.splash.SplashScreen
import com.example.eform.ui.auth.LoginScreen
import com.example.eform.ui.auth.LoginStudentsScreen
import com.example.eform.ui.auth.RegisterScreen
import com.example.eform.ui.auth.RegisterStudentsScreen
import com.example.eform.ui.auth.RoleSelectionScreen
import com.example.eform.ui.dashboard.DashboardScreen
import com.example.eform.ui.dashboard.DashboardScreenStudents
import com.example.eform.ui.form.CreateFormScreen
import com.example.eform.ui.form.FavoriteFormsScreen
import com.example.eform.ui.form.FormAnswerScreen // Pastikan ini dari package ui.form
import com.example.eform.ui.form.HistoryFormScreen
// import com.example.eform.ui.form.MyFormsScreen // Ini sudah digantikan FavoriteFormsScreen
import com.example.eform.ui.form.PreviewFormScreen
import com.example.eform.ui.notification.NotificationScreen
import com.example.eform.ui.profile.ProfileScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    onLoginSuccess: (String) -> Unit // Pastikan parameter ini ada
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val userDao = db.userDao()
    val formDao = db.formDao()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController)
        }
        composable(Screen.Login.route) {
            // <<< PERBAIKAN: Teruskan onLoginSuccess
            LoginScreen(navController, userDao, onLoginSuccess = onLoginSuccess)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController, userDao)
        }

        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                DashboardScreen(navController, userIdentifier)
            }
        }

        composable(
            route = Screen.DashboardStudents.route,
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.LoginStudents.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                DashboardScreenStudents(navController, userIdentifier)
            }
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Role.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                ProfileScreen(navController, userDao, userIdentifier)
            }
        }

        composable(
            route = Screen.Notification.route, // Ini seharusnya "notification/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType }) // <<< PERBAIKAN: Tambahkan arguments
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier") // <<< PERBAIKAN: Ambil dari backStackEntry
            if (userIdentifier.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("User tidak dikenal untuk menampilkan notifikasi.")
                }
            } else {
                NotificationScreen(navController, userIdentifier)
            }
        }

        composable(
            route = Screen.CreateForm.route, // Ini seharusnya "create_form/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType }) // <<< PERBAIKAN: Tambahkan arguments
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier") // <<< PERBAIKAN: Ambil dari backStackEntry
            if (userIdentifier.isNullOrBlank()){
                LaunchedEffect(Unit){ navController.navigate(Screen.Login.route) { popUpTo(navController.graph.startDestinationId){inclusive = true} } }
            } else {
                CreateFormScreen(navController, userIdentifier) // <<< PERBAIKAN: Teruskan userIdentifier
            }
        }

        composable(Screen.HistoryForm.route) {
            HistoryFormScreen(navController)
        }

        composable(
            route = Screen.FavoriteForms.route, // Ini "favorite_forms/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Role.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                FavoriteFormsScreen(navController, userIdentifier)
            }
        }

        composable(
            route = Screen.PreviewForm.route,
            arguments = listOf(navArgument("formId") { type = NavType.IntType })
        ) { backStackEntry ->
            val formId = backStackEntry.arguments?.getInt("formId") ?: 0
            PreviewFormScreen(formId = formId)
        }

        composable(
            route = Screen.FormAnswer.route, // Ini seharusnya "form_answer/{formId}/{userIdentifier}"
            arguments = listOf(
                navArgument("formId") { type = NavType.IntType },
                navArgument("userIdentifier") { type = NavType.StringType } // <<< PERBAIKAN: Tambahkan argumen userIdentifier
            )
        ) { backStackEntry ->
            val formId = backStackEntry.arguments?.getInt("formId") ?: 0
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier") // <<< PERBAIKAN: Ambil userIdentifier
            var formEntityState by remember { mutableStateOf<FormEntity?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(formId) {
                isLoading = true
                val entity = formDao.getFormWithQuestions(formId)
                formEntityState = entity
                isLoading = false
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (formEntityState != null && !userIdentifier.isNullOrBlank()) {
                    // <<< PERBAIKAN: Teruskan navController dan userIdentifier
                    FormAnswerScreen(navController = navController, formData = formEntityState!!, userIdentifier = userIdentifier)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Formulir tidak ditemukan atau pengguna tidak valid.")
                    }
                }
            }
        }

        composable(
            route = Screen.EditForm.route,
            arguments = listOf(navArgument("formId") { type = NavType.IntType })
        ) { backStackEntry ->
            val formId = backStackEntry.arguments?.getInt("formId") ?: 0
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Halaman Edit Form untuk ID: $formId (Belum diimplementasikan)")
            }
        }

        composable(Screen.Role.route) {
            RoleSelectionScreen(navController)
        }
        composable(Screen.LoginStudents.route) {
            // <<< PERBAIKAN: Teruskan onLoginSuccess
            LoginStudentsScreen(navController, userDao, onLoginSuccess = onLoginSuccess)
        }
        composable(Screen.RegisterStudents.route) {
            RegisterStudentsScreen(navController, userDao)
        }
    }
}
package com.example.eform.navigation

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.eform.ui.auth.*
import com.example.eform.ui.dashboard.DashboardScreen
import com.example.eform.ui.dashboard.DashboardScreenStudents
import com.example.eform.ui.form.*
import com.example.eform.ui.notification.NotificationScreen
import com.example.eform.ui.profile.ProfileScreen
import com.example.eform.ui.response.FormResponseDetailScreen
import com.example.eform.ui.response.ResponseListScreen
import com.example.eform.ui.splash.SplashScreen
import com.example.eform.ui.viewmodel.*

@Composable
fun AppNavHost(
    navController: NavHostController,
    onLoginSuccess: (String) -> Unit // Callback untuk menyimpan userIdentifier di MainActivity
) {
    val application = LocalContext.current.applicationContext as Application

    // ViewModel Factories
    val authViewModelFactory = AuthViewModel.AuthViewModelFactory(application)
    val dashboardViewModelFactory = DashboardViewModel.DashboardViewModelFactory(application)
    val dashboardStudentsViewModelFactory = DashboardStudentsViewModel.DashboardStudentsViewModelFactory(application)
    val createFormViewModelFactory = CreateFormViewModel.CreateFormViewModelFactory(application)
    val formAnswerViewModelFactory = FormAnswerViewModel.FormAnswerViewModelFactory(application)
    val notificationViewModelFactory = NotificationViewModel.NotificationViewModelFactory(application)
    val favoriteFormsViewModelFactory = FavoriteFormsViewModel.FavoriteFormsViewModelFactory(application)
    val profileViewModelFactory = ProfileViewModel.ProfileViewModelFactory(application)
    val historyFormViewModelFactory = HistoryFormViewModel.HistoryFormViewModelFactory(application)
    val previewFormViewModelFactory = PreviewFormViewModel.Factory(application)

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController)
        }
        composable(Screen.Role.route) {
            RoleSelectionScreen(navController)
        }
        composable(Screen.Login.route) { // Untuk Guru
            LoginScreen(
                navController = navController,
                onLoginSuccess = onLoginSuccess,
                authViewModel = viewModel(factory = authViewModelFactory)
            )
        }
        composable(Screen.Register.route) { // Untuk Guru
            RegisterScreen(
                navController = navController,
                authViewModel = viewModel(factory = authViewModelFactory)
            )
        }
        composable(Screen.LoginStudents.route) {
            LoginStudentsScreen(
                navController = navController,
                onLoginSuccess = onLoginSuccess,
                authViewModel = viewModel(factory = authViewModelFactory)
            )
        }
        composable(Screen.RegisterStudents.route) {
            RegisterStudentsScreen(
                navController = navController,
                authViewModel = viewModel(factory = authViewModelFactory)
            )
        }

        composable(
            route = Screen.Dashboard.route, // "dashboard/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) { popUpTo(navController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true } }
            } else {
                DashboardScreen(
                    navController = navController,
                    userIdentifier = userIdentifier,
                    dashboardViewModel = viewModel(factory = dashboardViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.DashboardStudents.route, // "dashboardstudents/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) { navController.navigate(Screen.LoginStudents.route) { popUpTo(navController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true } }
            } else {
                DashboardScreenStudents(
                    navController = navController,
                    userIdentifier = userIdentifier,
                    dashboardStudentsViewModel = viewModel(factory = dashboardStudentsViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.Profile.route, // "profile/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) { navController.navigate(Screen.Role.route) { popUpTo(navController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true } }
            } else {
                ProfileScreen(
                    navController = navController,
                    userIdentifier = userIdentifier,
                    profileViewModel = viewModel(factory = profileViewModelFactory),
                    authViewModel = viewModel(factory = authViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.Notification.route, // "notification/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("User tidak dikenal untuk notifikasi.") }
            } else {
                NotificationScreen(
                    navController = navController,
                    userIdentifier = userIdentifier,
                    notificationViewModel = viewModel(factory = notificationViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.CreateForm.route, // "create_form/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()){
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: Sesi guru tidak valid untuk membuat formulir.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) { Text("Kembali ke Login") }
                }
            } else {
                CreateFormScreen(
                    navController = navController,
                    userIdentifier = userIdentifier,
                    createFormViewModel = viewModel(factory = createFormViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.HistoryForm.route, // Ini akan menjadi "history_form/{userIdentifier}" JIKA Screen.kt BENAR
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                // ... penanganan error ...
            } else {
                HistoryFormScreen(
                    navController = navController,
                    userIdentifier = userIdentifier,
                    historyFormViewModel = viewModel(factory = historyFormViewModelFactory)
                )
            }
        }


        composable(
            route = Screen.FavoriteForms.route, // "favorite_forms/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) { navController.navigate(Screen.Role.route) { popUpTo(navController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true } }
            } else {
                FavoriteFormsScreen(
                    navController = navController,
                    userIdentifier = userIdentifier,
                    favoriteFormsViewModel = viewModel(factory = favoriteFormsViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.PreviewForm.route, // "preview_form/{formId}"
            arguments = listOf(navArgument("formId") { type = NavType.IntType })
        ) { backStackEntry ->
            val formId = backStackEntry.arguments?.getInt("formId") ?: 0
            if (formId == 0) {
                // Tampilkan pesan error jika formId tidak valid
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("ID Formulir tidak valid.")
                }
            } else {
                // Berikan ViewModel ke Screen
                PreviewFormScreen(
                    navController = navController,
                    formId = formId,
                    viewModel = viewModel(factory = previewFormViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.FormAnswer.route, // "form_answer/{formId}/{userIdentifier}"
            arguments = listOf(
                navArgument("formId") { type = NavType.IntType },
                navArgument("userIdentifier") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val formId = backStackEntry.arguments?.getInt("formId") ?: 0
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")

            if (formId == 0 || userIdentifier.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Data formulir atau pengguna tidak valid untuk menjawab.")
                }
            } else {
                FormAnswerScreen(
                    navController = navController,
                    formId = formId,
                    userIdentifier = userIdentifier,
                    formAnswerViewModel = viewModel(factory = formAnswerViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.EditForm.route, // "edit_form/{formId}"
            arguments = listOf(navArgument("formId") { type = NavType.IntType })
        ) { backStackEntry ->
            val formId = backStackEntry.arguments?.getInt("formId") ?: 0
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Halaman Edit Form untuk ID: $formId (Belum diimplementasikan)")
            }
        }

        composable(
            route = Screen.ResponseList.route,
            arguments = listOf(
                navArgument("formId") { type = NavType.IntType },
                navArgument("formTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val formId = backStackEntry.arguments?.getInt("formId") ?: 0
            val formTitle = backStackEntry.arguments?.getString("formTitle") ?: "Respons"
            ResponseListScreen(
                navController = navController,
                formId = formId,
                formTitle = formTitle
            )
        }

        // Rute BARU untuk menampilkan detail satu respons
        composable(
            route = Screen.FormResponseDetail.route,
            arguments = listOf(navArgument("responseId") { type = NavType.IntType })
        ) { backStackEntry ->
            val responseId = backStackEntry.arguments?.getInt("responseId") ?: 0
            FormResponseDetailScreen(
                navController = navController,
                responseId = responseId
            )
        }



    }
}

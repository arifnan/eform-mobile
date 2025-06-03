package com.example.eform.navigation

import android.app.Application
import android.widget.Toast // Untuk fallback jika userIdentifier kosong di CreateForm
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
// import com.example.eform.data.database.AppDatabase // Tidak perlu akses DB langsung di sini jika ViewModel menangani
// import com.example.eform.data.model.FormEntity // Tidak perlu jika ViewModel menangani
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
import com.example.eform.ui.form.FormAnswerScreen
import com.example.eform.ui.form.HistoryFormScreen
import com.example.eform.ui.form.PreviewFormScreen
import com.example.eform.ui.notification.NotificationScreen
import com.example.eform.ui.profile.ProfileScreen
import com.example.eform.ui.viewmodel.* // Impor semua ViewModel
import com.example.eform.data.database.AppDatabase // Diperlukan untuk ProfileScreen jika userDao masih di-pass langsung

@Composable
fun AppNavHost(
    navController: NavHostController,
    onLoginSuccess: (String) -> Unit
) {
    val application = LocalContext.current.applicationContext as Application

    // Factory untuk ViewModel (seperti yang sudah Anda buat di AppNavHost sebelumnya)
    val authViewModelFactory = AuthViewModel.AuthViewModelFactory(application)
    val dashboardViewModelFactory = DashboardViewModel.DashboardViewModelFactory(application)
    val createFormViewModelFactory = CreateFormViewModel.CreateFormViewModelFactory(application)
    val formAnswerViewModelFactory = FormAnswerViewModel.FormAnswerViewModelFactory(application)
    val notificationViewModelFactory = NotificationViewModel.NotificationViewModelFactory(application)
    val favoriteFormsViewModelFactory = FavoriteFormsViewModel.FavoriteFormsViewModelFactory(application)
    // Tambahkan factory untuk ProfileViewModel jika Anda membuatnya

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                onLoginSuccess = onLoginSuccess,
                authViewModel = viewModel(factory = authViewModelFactory)
            )
        }
        composable(Screen.Register.route) {
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
            route = Screen.Dashboard.route,
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) { navController.navigate(Screen.Login.route) { popUpTo(navController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true } }
            } else {
                DashboardScreen( // <<< PERBAIKAN: Teruskan ViewModel
                    navController = navController,
                    userIdentifier = userIdentifier,
                    dashboardViewModel = viewModel(factory = dashboardViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.DashboardStudents.route,
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) { navController.navigate(Screen.LoginStudents.route) { popUpTo(navController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true } }
            } else {
                DashboardScreenStudents(
                    navController = navController,
                    userIdentifier = userIdentifier
                    // Jika DashboardScreenStudents juga butuh ViewModel, tambahkan di sini
                    // dashboardStudentsViewModel = viewModel(factory = ...)
                )
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
                val profileViewModelFactory = ProfileViewModel.ProfileViewModelFactory(application)
                ProfileScreen( // Hapus parameter userDao jika ProfileScreen akan menggunakan ProfileViewModel
                    navController = navController,
                    userIdentifier = userIdentifier, // ProfileScreen akan menggunakan ini untuk memanggil load di ViewModel
                    profileViewModel = viewModel(factory = profileViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.Notification.route, // Ini "notification/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("User tidak dikenal untuk notifikasi.") }
            } else {
                NotificationScreen( // <<< PERBAIKAN: Teruskan ViewModel
                    navController = navController,
                    userIdentifier = userIdentifier,
                    notificationViewModel = viewModel(factory = notificationViewModelFactory)
                )
            }
        }

        composable(
            route = Screen.CreateForm.route, // Ini "create_form/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            val context = LocalContext.current // Ambil context di sini

            if (userIdentifier.isNullOrBlank()){
                // Opsi 1: Tampilkan UI Error dan tombol kembali
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
                    }) {
                        Text("Kembali ke Login")
                    }
                }
                // Opsi 2 (alternatif): Navigasi langsung menggunakan LaunchedEffect (seperti yang sudah ada)
                // LaunchedEffect(Unit) {
                //     Toast.makeText(context, "Sesi guru tidak valid, kembali ke login.", Toast.LENGTH_LONG).show()
                //     navController.navigate(Screen.Login.route) {
                //         popUpTo(navController.graph.startDestinationId) { inclusive = true }
                //         launchSingleTop = true
                //     }
                // }
            } else {
                CreateFormScreen(
                    navController = navController,
                    userIdentifier = userIdentifier,
                    // createFormViewModel akan di-provide oleh default factory Composable jika tidak ada factory khusus yang diperlukan di sini
                    // Jika CreateFormViewModel memerlukan Application context, factory diperlukan.
                    createFormViewModel = viewModel(factory = createFormViewModelFactory) // createFormViewModelFactory sudah didefinisikan di atas AppNavHost
                )
            }
        }

        composable(Screen.HistoryForm.route) { // Asumsi rute "history_form"
            // Jika HistoryFormScreen memerlukan userIdentifier/ViewModel, tambahkan di sini
            HistoryFormScreen(navController)
        }

        composable(
            route = Screen.FavoriteForms.route, // Ini "favorite_forms/{userIdentifier}"
            arguments = listOf(navArgument("userIdentifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val userIdentifier = backStackEntry.arguments?.getString("userIdentifier")
            if (userIdentifier.isNullOrBlank()) {
                LaunchedEffect(Unit) { navController.navigate(Screen.Role.route) { popUpTo(navController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true } }
            } else {
                FavoriteFormsScreen( // <<< PERBAIKAN: Teruskan ViewModel
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
            PreviewFormScreen(formId = formId)
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
                FormAnswerScreen( // <<< PERBAIKAN: Teruskan ViewModel
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

        composable(Screen.Role.route) {
            RoleSelectionScreen(navController)
        }
    }
}
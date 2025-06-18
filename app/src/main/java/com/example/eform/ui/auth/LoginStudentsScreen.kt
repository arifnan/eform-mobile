package com.example.eform.ui.auth

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.LoginRequest
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.AuthResult
import com.example.eform.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginStudentsScreen(
    navController: NavController,
    formCode: String?,
    onLoginSuccess: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginResult by authViewModel.loginResult.collectAsState()
    val isLoading = loginResult is AuthResult.Loading

    val db = AppDatabase.getDatabase(context)
    val formDao = db.formDao()
    val coroutineScope = rememberCoroutineScope()

    // This LaunchedEffect will now handle the delay
    LaunchedEffect(loginResult) {
        when (val result = loginResult) {
            is AuthResult.Success -> {
                if (result.authResponse.user.role.equals("student", ignoreCase = true)) {
                    Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                    val userIdentifier = result.authResponse.user.email
                    onLoginSuccess(userIdentifier)
                    if (formCode != null) {
                        coroutineScope.launch {
                            val form = formDao.getFormByCode(formCode)
                            if (form != null) {
                                // Jika form ditemukan, langsung navigasi ke FormAnswerScreen
                                navController.navigate(
                                    Screen.FormAnswer.route
                                        .replace("{formId}", "${form.id}")
                                        .replace("{userIdentifier}", userIdentifier)
                                ) {
                                    popUpTo(Screen.LoginStudents.route) { inclusive = true }
                                }
                            } else {
                                // Jika form tidak ditemukan, navigasi ke dashboard biasa
                                Toast.makeText(context, "Form dari link tidak ditemukan, mengarahkan ke beranda.", Toast.LENGTH_LONG).show()
                                navController.navigate(Screen.DashboardStudents.route.replace("{userIdentifier}", userIdentifier)) {
                                    popUpTo(Screen.LoginStudents.route) { inclusive = true }
                                }
                            }
                        }
                    } else {
                        // Alur login normal (tanpa deep link)
                        navController.navigate(Screen.DashboardStudents.route.replace("{userIdentifier}", userIdentifier)) {
                            popUpTo(Screen.LoginStudents.route) { inclusive = true }
                        }
                    }
                    // Delay for 1 second after login is successful
                    delay(100)

                    navController.navigate(Screen.DashboardStudents.route.replace("{userIdentifier}", userIdentifier)) {
                        popUpTo(Screen.LoginStudents.route) { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Akun ini bukan akun siswa. Silakan login sebagai guru.", Toast.LENGTH_LONG).show()
                    authViewModel.logout()
                }
                authViewModel.resetLoginResult()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, "Login Gagal: ${result.message}", Toast.LENGTH_LONG).show()
                authViewModel.resetLoginResult()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login Murid", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), readOnly = isLoading)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), readOnly = isLoading)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    authViewModel.login(
                        LoginRequest(
                            email = email,
                            password = password,
                            role = "student"
                        )
                    )
                } else {
                    Toast.makeText(context, "Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Login")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = {
            if (!isLoading) navController.navigate(Screen.RegisterStudents.route)
        }) {
            Text("Belum punya akun? Daftar sebagai Murid")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            if(!isLoading) {
                navController.navigate(Screen.Role.route){
                    popUpTo(Screen.LoginStudents.route){ inclusive = true}
                }
            }
        }) {
            Text("Kembali ke Pemilihan Peran")
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun LoginStudentsScreenPreview() {
//    EformTheme {
//        LoginStudentsScreen(navController = rememberNavController(), onLoginSuccess = {}, formCode = ())
//    }
//}
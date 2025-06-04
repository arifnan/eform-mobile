// File: com/example/eform/ui/auth/LoginScreen.kt
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
import com.example.eform.data.model.LoginRequest // Pastikan ini diimpor
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.AuthResult
import com.example.eform.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    var nipOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val loginResult by authViewModel.loginResult.collectAsState()
    val isLoading = loginResult is AuthResult.Loading

    LaunchedEffect(loginResult) {
        when (val result = loginResult) {
            is AuthResult.Success -> {
                if (result.authResponse.user.role.equals("teacher", ignoreCase = true)) {
                    Toast.makeText(context, "Login Guru Berhasil! Selamat datang ${result.authResponse.user.name}", Toast.LENGTH_SHORT).show()
                    val userIdentifier = result.authResponse.user.nip ?: result.authResponse.user.email // Prioritaskan NIP untuk guru
                    onLoginSuccess(userIdentifier) // Callback ke MainActivity
                    navController.navigate(Screen.Dashboard.route.replace("{userIdentifier}", userIdentifier)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Akun ini bukan akun guru. Silakan login sebagai siswa.", Toast.LENGTH_LONG).show()
                    authViewModel.logout() // Logout jika peran salah
                }
                authViewModel.resetLoginResult()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, "Login Gagal: ${result.message}", Toast.LENGTH_LONG).show()
                authViewModel.resetLoginResult()
            }
            else -> { /* Idle atau Loading */ }
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
        Text("Login Guru", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nipOrEmail,
            onValueChange = { nipOrEmail = it },
            label = { Text("NIP atau Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            readOnly = isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            readOnly = isLoading
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nipOrEmail.isNotBlank() && password.isNotBlank()) {
                    authViewModel.login(
                        LoginRequest(
                            email = nipOrEmail, // Backend akan handle jika ini NIP atau Email
                            password = password,
                            role = "teacher" // <-- KIRIM ROLE "teacher"
                        )
                    )
                } else {
                    Toast.makeText(context, "NIP/Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
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
            if (!isLoading) navController.navigate(Screen.Register.route)
        }) {
            Text("Belum punya akun? Daftar sebagai Guru")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            if(!isLoading) {
                navController.navigate(Screen.Role.route){
                    popUpTo(Screen.Login.route){ inclusive = true}
                }
            }
        }) {
            Text("Kembali ke Pemilihan Peran")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    EformTheme {
        LoginScreen(navController = rememberNavController(), onLoginSuccess = {})
    }
}
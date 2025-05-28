package com.example.eform.ui.auth

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.model.RegisterRequest
// import com.example.eform.data.database.UserDao // Tidak lagi diperlukan
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.AuthResult
import com.example.eform.ui.viewmodel.AuthViewModel

@Composable
fun RegisterStudentsScreen(
    navController: NavController,
    // userDao: UserDao, // Tidak lagi diperlukan
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var genderValueForApi by remember { mutableStateOf<Boolean?>(true) } // Nilai boolean untuk API (true untuk Laki-laki)

    val registerResult by authViewModel.registerResult.collectAsState()
    val isLoading = registerResult is AuthResult.Loading

    LaunchedEffect(registerResult) {
        when (val result = registerResult) {
            is AuthResult.Success -> {
                Toast.makeText(context, "Registrasi Siswa Berhasil! Silakan Login.", Toast.LENGTH_LONG).show()
                authViewModel.resetRegisterResult()
                navController.navigate(Screen.LoginStudents.route) {
                    popUpTo(Screen.RegisterStudents.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is AuthResult.Error -> {
                Toast.makeText(context, "Registrasi Gagal: ${result.message}", Toast.LENGTH_LONG).show()
                authViewModel.resetRegisterResult()
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
        Text("Daftar Murid", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Konfirmasi Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(context, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Password tidak cocok", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                authViewModel.register(
                    RegisterRequest(
                        name = name,
                        email = email,
                        nip = null, // Siswa tidak punya NIP
                        password = password,
                        password_confirmation = confirmPassword,
                        role = "student", // Kirim role sebagai student
                        gender = genderValueForApi, // Ambil dari input jika ada
                        subject = null // Atau "" jika API Laravel mengharapkan string dan tidak wajib untuk siswa
                    )
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Daftar")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = {
            navController.navigate(Screen.LoginStudents.route)
        }) {
            Text("Sudah punya akun? Login sebagai Murid")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterStudentsScreenPreview() {
    EformTheme {
        RegisterStudentsScreen(navController = rememberNavController())
    }
}
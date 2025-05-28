package com.example.eform.ui.auth

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.model.LoginRequest
// Hapus UserDao jika tidak digunakan lagi setelah beralih ke API sepenuhnya untuk login
// import com.example.eform.data.database.UserDao
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.AuthResult
import com.example.eform.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (String) -> Unit, // Callback ke MainActivity untuk menyimpan userIdentifier
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    var nipOrEmail by remember { mutableStateOf("") } // Guru bisa login dengan NIP atau Email
    var password by remember { mutableStateOf("") }

    val loginResult by authViewModel.loginResult.collectAsState()
    val isLoading = loginResult is AuthResult.Loading

    LaunchedEffect(loginResult) {
        when (val result = loginResult) {
            is AuthResult.Success -> {
                Toast.makeText(context, "Login Berhasil! Selamat datang ${result.authResponse.user.name}", Toast.LENGTH_SHORT).show()
                // Ambil identifier yang sesuai (NIP untuk guru, email untuk siswa)
                // Dalam AuthController Laravel Anda, 'login' sepertinya tidak membedakan peran secara langsung untuk field login
                // Kita asumsikan jika peran "teacher", NIP ada dan digunakan. Jika "student", email digunakan.
                val user = result.authResponse.user
                val userIdentifier = if (user.role.equals("teacher", ignoreCase = true) && !user.nip.isNullOrBlank()) {
                    user.nip
                } else {
                    user.email
                }

                onLoginSuccess(userIdentifier) // Kirim identifier ke MainActivity

                if (user.role.equals("teacher", ignoreCase = true)) {
                    navController.navigate(Screen.Dashboard.route.replace("{userIdentifier}", userIdentifier)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else if (user.role.equals("student", ignoreCase = true)) {
                    // Jika endpoint login ini juga bisa untuk siswa
                    navController.navigate(Screen.DashboardStudents.route.replace("{userIdentifier}", userIdentifier)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Peran pengguna tidak dikenal: ${user.role}", Toast.LENGTH_LONG).show()
                }
                authViewModel.resetLoginResult() // Reset state setelah navigasi/tampil pesan
            }
            is AuthResult.Error -> {
                Toast.makeText(context, "Login Gagal: ${result.message}", Toast.LENGTH_LONG).show()
                authViewModel.resetLoginResult()
            }
            else -> { /* Idle atau Loading, ditangani oleh UI tombol */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
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
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nipOrEmail.isNotBlank() && password.isNotBlank()) {
                    // API Login Laravel Anda menggunakan field 'email' untuk login,
                    // jadi kita kirim nipOrEmail sebagai 'email'.
                    // Backend akan memvalidasi apakah itu NIP (jika guru) atau email.
                    authViewModel.login(LoginRequest(email = nipOrEmail, password = password))
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
            navController.navigate(Screen.Register.route)
        }) {
            Text("Belum punya akun? Daftar sebagai Guru")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            navController.navigate(Screen.Role.route){
                popUpTo(Screen.Login.route){ inclusive = true}
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
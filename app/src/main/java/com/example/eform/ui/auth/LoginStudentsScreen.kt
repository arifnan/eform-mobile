package com.example.eform.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eform.R
import com.example.eform.data.database.UserDao
import com.example.eform.navigation.Screen
import com.example.eform.utils.AesEncryptionHelper
import kotlinx.coroutines.launch

@Composable
fun LoginStudentsScreen(navController: NavController, userDao: UserDao,onLoginSuccess: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login Murid", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
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
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Email dan Password tidak boleh kosong"
                    return@Button
                }
                coroutineScope.launch {
                    isLoading = true
                    try {
                        val user = userDao.getUserByEmail(email)
                        if (user != null) {
                            val decryptedPassword = AesEncryptionHelper.decrypt(user.password)
                            if (password == decryptedPassword) {
                                onLoginSuccess(email)
                                navController.navigate(Screen.DashboardStudents.route.replace("{userIdentifier}", email)) {
                                    popUpTo(Screen.LoginStudents.route) { inclusive = true }
                                }
                            } else {
                                errorMessage = "Password salah"
                            }
                        } else {
                            errorMessage = "Email tidak ditemukan"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Gagal login: ${e.message}"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Loading..." else "Login")
        }

        Spacer(modifier = Modifier.height(12.dp))

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = {
            navController.navigate(Screen.RegisterStudents.route) // Navigasi ke Register Murid
        }) {
            Text("Belum punya akun? Daftar")
        }
    }
}

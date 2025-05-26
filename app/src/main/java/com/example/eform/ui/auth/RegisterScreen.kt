//package com.example.eform.ui.auth
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.*
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.input.*
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.example.eform.R
//import com.example.eform.data.api.RetrofitInstance
//import com.example.eform.data.model.RegisterRequest
//import com.example.eform.navigation.Screen
//import kotlinx.coroutines.launch
//
//@Composable
//fun RegisterScreen(navController: NavController) {
//    var name by remember { mutableStateOf("") }
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var confirmPassword by remember { mutableStateOf("") }
//    var isLoading by remember { mutableStateOf(false) }
//    var errorMessage by remember { mutableStateOf<String?>(null) }
//
//    val coroutineScope = rememberCoroutineScope()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text("Register", style = MaterialTheme.typography.headlineMedium)
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Image(
//            painter = painterResource(id = R.drawable.register_illustration),
//            contentDescription = null,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(200.dp)
//        )
//
//        OutlinedTextField(
//            value = name,
//            onValueChange = { name = it },
//            label = { Text("Nama") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        OutlinedTextField(
//            value = email,
//            onValueChange = { email = it },
//            label = { Text("Email") },
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            visualTransformation = PasswordVisualTransformation(),
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        OutlinedTextField(
//            value = confirmPassword,
//            onValueChange = { confirmPassword = it },
//            label = { Text("Konfirmasi Password") },
//            visualTransformation = PasswordVisualTransformation(),
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        Button(
//            onClick = {
//                coroutineScope.launch {
//                    isLoading = true
//                    try {
//                        val response = RetrofitInstance.api.register(
//                            RegisterRequest(name, email, password, confirmPassword)
//                        )
//                        if (response.isSuccessful && response.body()?.status == true) {
//                            navController.navigate(Screen.Login.route) {
//                                popUpTo(Screen.Register.route) { inclusive = true }
//                            }
//                        } else {
//                            errorMessage = response.body()?.message ?: "Register gagal"
//                        }
//                    } catch (e: Exception) {
//                        errorMessage = "Gagal koneksi ke server"
//                    }
//                    isLoading = false
//                }
//            },
//            enabled = !isLoading,
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text(if (isLoading) "Loading..." else "Register")
//        }
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        errorMessage?.let {
//            Text(text = it, color = MaterialTheme.colorScheme.error)
//        }
//
//        Spacer(modifier = Modifier.height(12.dp))
//
//        TextButton(onClick = {
//            navController.navigate(Screen.Login.route)
//        }) {
//            Text("Sudah punya akun? Login")
//        }
//    }
//}

//testing untuk lokal dibawah
package com.example.eform.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eform.R
import com.example.eform.data.database.UserDao
import com.example.eform.data.model.RegisterRequest
import com.example.eform.data.model.UserEntity
import com.example.eform.navigation.Screen
import com.example.eform.utils.AesEncryptionHelper
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController, userDao: UserDao) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nip by remember { mutableStateOf("") } // Tambahkan NIP
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Daftar Guru", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nip,
            onValueChange = { nip = it }, // Input NIP
            label = { Text("NIP") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Konfirmasi Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    try {
                        if (password == confirmPassword) {
                            val encryptedPassword = AesEncryptionHelper.encrypt(password)
                            val newUser   = UserEntity(name = name, email = email, nip = nip, password = encryptedPassword, role = "guru")
                            userDao.insertUser (newUser )
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Register.route) { inclusive = true }
                            }
                        } else {
                            errorMessage = "Password tidak cocok"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Gagal registrasi: ${e.message}"
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Loading..." else "Daftar")
        }

        Spacer(modifier = Modifier.height(12.dp))

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = {
            navController.navigate(Screen.Login.route) // Navigasi ke Login Guru
        }) {
            Text("Sudah punya akun? Login")
        }
    }
}



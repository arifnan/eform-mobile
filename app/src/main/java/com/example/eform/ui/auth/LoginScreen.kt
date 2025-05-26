//package com.example.eform.ui.auth
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.input.*
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.example.eform.R
//import com.example.eform.data.api.RetrofitInstance
//import com.example.eform.data.local.UserPreferences
//import com.example.eform.data.model.LoginRequest
//import com.example.eform.navigation.Screen
//import kotlinx.coroutines.launch
//
//@Composable
//fun LoginScreen(navController: NavController) {
//    val coroutineScope = rememberCoroutineScope()
//
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//    var isLoading by remember { mutableStateOf(false) }
//    var errorMessage by remember { mutableStateOf<String?>(null) }
//    val context = LocalContext.current
//    val userPrefs = remember { UserPreferences(context) }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        verticalArrangement = Arrangement.Center
//    ) {
//        Text("Login", style = MaterialTheme.typography.headlineMedium)
//        Spacer(modifier = Modifier.height(16.dp))
//
//        Image(
//            painter = painterResource(id = R.drawable.login_illustration),
//            contentDescription = null,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(200.dp)
//        )
//
//        OutlinedTextField(
//            value = email,
//            onValueChange = { email = it },
//            label = { Text("Email") },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        OutlinedTextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Password") },
//            singleLine = true,
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
//                        val response = RetrofitInstance.api.login(LoginRequest(email, password))
//                        if (response.isSuccessful && response.body()?.status == true) {
//                            // Lanjut ke dashboard atau simpan token
//                            response.body()?.token?.let { token ->
//                                userPrefs.saveToken(token)
//                            }
//                            navController.navigate(Screen.Dashboard.route) {
//                                popUpTo(Screen.Login.route) { inclusive = true }
//                            }
//                        } else {
//                            errorMessage = response.body()?.message ?: "Login gagal"
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
//            Text(if (isLoading) "Loading..." else "Login")
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
//            navController.navigate(Screen.Register.route)
//        }) {
//            Text("Belum punya akun? Daftar")
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eform.R
import com.example.eform.data.database.UserDao
import com.example.eform.data.local.UserPreferences
import com.example.eform.data.model.LoginRequest
import com.example.eform.navigation.Screen
import com.example.eform.utils.AesEncryptionHelper
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, userDao: UserDao, onLoginSuccess: (String) -> Unit ) {
    val coroutineScope = rememberCoroutineScope()
    var nip by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login Guru", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nip,
            onValueChange = { nip = it },
            label = { Text("NIP") },
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
                coroutineScope.launch {
                    isLoading = true
                    try {
                        val user = userDao.getUserByNip(nip) // Mengambil user berdasarkan NIP
                        if (user != null) {
                            val decryptedPassword = AesEncryptionHelper.decrypt(user.password)
                            if (password == decryptedPassword) {
                                onLoginSuccess(nip)
                                navController.navigate(Screen.Dashboard.route.replace("{userIdentifier}", nip)) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            } else {
                                errorMessage = "Password salah"
                            }
                        } else {
                            errorMessage = "NIP tidak ditemukan"
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
            navController.navigate(Screen.Register.route) // Navigasi ke Register Guru
        }) {
            Text("Belum punya akun? Daftar")
        }
    }
}



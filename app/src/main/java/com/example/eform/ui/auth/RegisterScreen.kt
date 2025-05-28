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
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.AuthResult
import com.example.eform.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class) // Untuk ExposedDropdownMenuBox
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nip by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    // Untuk Gender
    val genderOptions = listOf("Laki-laki", "Perempuan")
    var selectedGenderText by remember { mutableStateOf(genderOptions[0]) } // Teks yang ditampilkan
    var genderValueForApi by remember { mutableStateOf<Boolean?>(true) } // Nilai boolean untuk API (true untuk Laki-laki)
    var genderDropdownExpanded by remember { mutableStateOf(false) }


    val registerResult by authViewModel.registerResult.collectAsState()
    val isLoading = registerResult is AuthResult.Loading

    LaunchedEffect(registerResult) {
        when (val result = registerResult) {
            is AuthResult.Success -> {
                Toast.makeText(context, "Registrasi Guru Berhasil! Silakan Login.", Toast.LENGTH_LONG).show()
                authViewModel.resetRegisterResult()
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
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
        Text("Daftar Guru", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = nip, onValueChange = { nip = it }, label = { Text("NIP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(8.dp))

        // Input Gender
        ExposedDropdownMenuBox(
            expanded = genderDropdownExpanded,
            onExpandedChange = { genderDropdownExpanded = !genderDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedGenderText,
                onValueChange = {}, // Tidak diubah langsung
                readOnly = true,
                label = { Text("Jenis Kelamin") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderDropdownExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors()
            )
            ExposedDropdownMenu(
                expanded = genderDropdownExpanded,
                onDismissRequest = { genderDropdownExpanded = false }
            ) {
                genderOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            selectedGenderText = selectionOption
                            genderValueForApi = (selectionOption == "Laki-laki") // true jika Laki-laki, false jika Perempuan
                            genderDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Mata Pelajaran") }, modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Konfirmasi Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || nip.isBlank() || password.isBlank() || confirmPassword.isBlank() || subject.isBlank() || genderValueForApi == null) {
                    Toast.makeText(context, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Password dan konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                // Pastikan RegisterRequest menerima Boolean? untuk gender
                authViewModel.register(
                    RegisterRequest(
                        name = name,
                        email = email,
                        nip = nip,
                        password = password,
                        password_confirmation = confirmPassword,
                        role = "teacher",
                        gender = genderValueForApi, // Kirim nilai Boolean
                        subject = subject
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
        TextButton(onClick = { if (!isLoading) navController.navigate(Screen.Login.route) }) {
            Text("Sudah punya akun? Login sebagai Guru")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    EformTheme {
        RegisterScreen(navController = rememberNavController())
    }
}
// File: arifnan/eform-mobile/eform-mobile-commit-3-juni/app/src/main/java/com/example/eform/ui/auth/RegisterStudentsScreen.kt
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eform.data.model.RegisterRequest
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.AuthResult
import com.example.eform.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class) // Diperlukan untuk ExposedDropdownMenuBox
@Composable
fun RegisterStudentsScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    // Gender: true untuk Laki-laki, false untuk Perempuan. Default ke Laki-laki.
    var genderValueForApi by remember { mutableStateOf<Boolean?>(true) }
    val genderOptions = listOf("Laki-laki", "Perempuan")
    var selectedGenderText by remember { mutableStateOf(genderOptions[0]) }
    var genderDropdownExpanded by remember { mutableStateOf(false) }

    // State untuk Dropdown Kelas
    val classOptions = listOf("Pilih Kelas", "10", "11", "12")
    var selectedClassText by remember { mutableStateOf(classOptions[0]) }
    var expandedClassDropdown by remember { mutableStateOf(false) }

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

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(12.dp))

        // Dropdown untuk Gender
        ExposedDropdownMenuBox(
            expanded = genderDropdownExpanded,
            onExpandedChange = { genderDropdownExpanded = !genderDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedGenderText,
                onValueChange = {},
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
                            genderValueForApi = (selectionOption == "Laki-laki")
                            genderDropdownExpanded = false
                        },
                        enabled = !isLoading
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Dropdown untuk Kelas
        ExposedDropdownMenuBox(
            expanded = expandedClassDropdown,
            onExpandedChange = { if (!isLoading) expandedClassDropdown = !expandedClassDropdown },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedClassText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Kelas") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClassDropdown) },
                modifier = Modifier.menuAnchor().fillMaxWidth(), // Penting untuk ExposedDropdownMenuBox
                colors = OutlinedTextFieldDefaults.colors(),
                enabled = !isLoading
            )
            ExposedDropdownMenu(
                expanded = expandedClassDropdown,
                onDismissRequest = { expandedClassDropdown = false }
            ) {
                classOptions.forEach { selectionOption ->
                    if (selectionOption != "Pilih Kelas") { // Jangan tampilkan "Pilih Kelas" sebagai opsi valid
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedClassText = selectionOption
                                expandedClassDropdown = false
                            },
                            enabled = !isLoading
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Konfirmasi Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true, readOnly = isLoading)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() || genderValueForApi == null) {
                    Toast.makeText(context, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (selectedClassText == "Pilih Kelas") {
                    Toast.makeText(context, "Silakan pilih kelas", Toast.LENGTH_SHORT).show()
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
                        nip = null,
                        password = password,
                        password_confirmation = confirmPassword,
                        role = "student",
                        gender = genderValueForApi,
                        grade = selectedClassText, // <-- Mengirim nilai kelas yang dipilih
                        subject = null
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
            if (!isLoading) navController.navigate(Screen.LoginStudents.route)
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
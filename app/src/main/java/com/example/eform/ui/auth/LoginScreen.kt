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
import com.example.eform.data.model.LoginRequest
import com.example.eform.data.database.AppDatabase
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.ui.viewmodel.AuthResult
import com.example.eform.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (String) -> Unit,
    formCode: String? = null, // Added formCode parameter for deep linking
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    var emailOrNip by remember { mutableStateOf("") } // Unified input for email or NIP
    var password by remember { mutableStateOf("") }

    val loginResult by authViewModel.loginResult.collectAsState()
    val isLoading = loginResult is AuthResult.Loading

    val db = AppDatabase.getDatabase(context)
    val formDao = db.formDao()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(loginResult) {
        when (val result = loginResult) {
            is AuthResult.Success -> {
                if (result.authResponse.user.role.equals("teacher", ignoreCase = true)) {
                    Toast.makeText(context, "Login Guru Berhasil! Selamat datang ${result.authResponse.user.name}", Toast.LENGTH_SHORT).show()
                    val userIdentifier = result.authResponse.user.nip ?: result.authResponse.user.email // Prioritize NIP for teacher
                    onLoginSuccess(userIdentifier)
                    navController.navigate(Screen.Dashboard.route.replace("{userIdentifier}", userIdentifier)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } else if (result.authResponse.user.role.equals("student", ignoreCase = true)) {
                    Toast.makeText(context, "Login Siswa Berhasil! Selamat datang ${result.authResponse.user.name}", Toast.LENGTH_SHORT).show()
                    val userIdentifier = result.authResponse.user.email
                    onLoginSuccess(userIdentifier)
                    if (formCode != null) {
                        coroutineScope.launch {
                            val form = formDao.getFormByCode(formCode)
                            if (form != null) {
                                navController.navigate(
                                    Screen.FormAnswer.route
                                        .replace("{formId}", "${form.id}")
                                        .replace("{userIdentifier}", userIdentifier)
                                ) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                Toast.makeText(context, "Form from link not found, redirecting to student dashboard.", Toast.LENGTH_LONG).show()
                                navController.navigate(Screen.DashboardStudents.route.replace("{userIdentifier}", userIdentifier)) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    } else {
                        navController.navigate(Screen.DashboardStudents.route.replace("{userIdentifier}", userIdentifier)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                } else { // Unknown role
                    Toast.makeText(context, "Login failed: Unknown user role.", Toast.LENGTH_LONG).show()
                    authViewModel.logout()
                }
                authViewModel.resetLoginResult()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, "Login Failed: ${result.message}", Toast.LENGTH_LONG).show()
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
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = emailOrNip,
            onValueChange = { emailOrNip = it },
            label = { Text("Email or NIP") }, // Changed label for unified input
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
                if (emailOrNip.isNotBlank() && password.isNotBlank()) {
                    authViewModel.login(
                        LoginRequest(
                            email = emailOrNip, // Send unified input as email, backend handles NIP/email check
                            password = password,
                            role = null // Remove or comment out this line to send null role or omit it
                        )
                    )
                } else {
                    Toast.makeText(context, "Email/NIP and Password cannot be empty", Toast.LENGTH_SHORT).show()
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
        // Removed registration buttons as per request. Registration will be handled elsewhere.
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            if(!isLoading) {
                navController.navigate(Screen.Onboarding.route){ // Changed to Onboarding as RoleSelection is removed
                    popUpTo(Screen.Login.route){ inclusive = true }
                }
            }
        }) {
            Text("Back to Onboarding")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    EformTheme {
        LoginScreen(navController = rememberNavController(), onLoginSuccess = {}, formCode = null)
    }
}
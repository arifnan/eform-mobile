package com.example.eform.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eform.navigation.Screen

@Composable
fun RoleSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Pilih Peran Anda", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Navigasi ke RegisterScreen untuk Guru
                navController.navigate(Screen.Login.route)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guru")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Navigasi ke RegisterStudentsScreen untuk Murid
                navController.navigate(Screen.LoginStudents.route)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Murid")
        }
    }
}

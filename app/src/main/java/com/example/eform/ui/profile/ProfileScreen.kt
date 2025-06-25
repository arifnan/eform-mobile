package com.example.eform.ui.profile

import android.app.Application
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.eform.R
import com.example.eform.data.model.api.UserApiModel
import com.example.eform.navigation.Screen
import com.example.eform.ui.components.StandardTopAppBar
import com.example.eform.ui.viewmodel.AuthViewModel
import com.example.eform.ui.viewmodel.ProfileUiState
import com.example.eform.ui.viewmodel.ProfileViewModel
import com.example.eform.ui.viewmodel.UpdateProfileResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userIdentifier: String,
    profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.ProfileViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.AuthViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val uiState by profileViewModel.uiState.collectAsState()
    val updateResult by profileViewModel.updateResult.collectAsState()

    var editMode by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val editableName by profileViewModel.editableName.collectAsState()
    val editableAddress by profileViewModel.editableAddress.collectAsState()
    val imageUri by profileViewModel.profileImageUri.collectAsState()
    val currentProfilePhotoUrl by profileViewModel.currentProfilePhotoUrl.collectAsState()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        profileViewModel.onProfileImageUriChanged(uri)
    }

    LaunchedEffect(updateResult) {
        when (val result = updateResult) {
            is UpdateProfileResult.Success -> {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                editMode = false
                profileViewModel.resetUpdateResult()
            }
            is UpdateProfileResult.Error -> {
                Toast.makeText(context, "Update Gagal: ${result.message}", Toast.LENGTH_LONG).show()
                profileViewModel.resetUpdateResult()
            }
            else -> { /* Idle atau Loading */ }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Konfirmasi Logout") },
            text = { Text("Apakah Anda yakin ingin keluar?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.logout()
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }) { Text("Iya") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Tidak")
                }
            }
        )
    }

    fun resetEditableFieldsToCurrentProfileData(user: UserApiModel?) {
        user?.let {
            profileViewModel.onNameChanged(it.name ?: "")
            profileViewModel.onAddressChanged(it.address ?: "")
            profileViewModel.onProfileImageUriChanged(null)
        }
    }

    // Fungsi untuk membatalkan
    val cancelAction = {
        editMode = false
        if (uiState is ProfileUiState.Success) {
            resetEditableFieldsToCurrentProfileData((uiState as ProfileUiState.Success).user)
        }
        Toast.makeText(context, "Edit dibatalkan", Toast.LENGTH_SHORT).show()
    }

    if (editMode) {
        BackHandler(enabled = true) {
            cancelAction()
        }
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(
                title = "Profil",
                navController = navController,
                onBackClicked = {
                    if (editMode) {
                        cancelAction()
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val currentUiState = uiState) {
            ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileUiState.Success -> {
                val user = currentUiState.user
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .align(Alignment.CenterHorizontally)
                            .clickable(enabled = editMode) { pickImage.launch("image/*") }
                    ) {
                        val imageToDisplay = imageUri ?: currentProfilePhotoUrl

                        val painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageToDisplay ?: R.drawable.ic_default_profile)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build()
                        )

                        Image(
                            painter = painter,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (editMode) {
                            Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                Icon(Icons.Default.Edit, "Edit Foto", tint = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).padding(4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (editMode) editableName else (user.name ?: "Nama Tidak Tersedia"),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = user.email ?: "Email Tidak Tersedia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileDataField(label = "Nama Lengkap", value = editableName, editMode = editMode, onValueChange = { profileViewModel.onNameChanged(it) })
                    ProfileDataField(label = "Email", value = user.email ?: "-", editMode = false)

                    if (user.role.equals("teacher", ignoreCase = true)) {
                        ProfileDataField(label = "NIP", value = user.nip ?: "-", editMode = false)
                        ProfileDataField(label = "Mata Pelajaran", value = user.subject ?: "-", editMode = false)
                    } else if (user.role.equals("student", ignoreCase = true)) {
                        ProfileDataField(label = "Kelas", value = user.grade ?: "-", editMode = false)
                    }
                    ProfileDataField(label = "Alamat", value = editableAddress, editMode = editMode, onValueChange = { profileViewModel.onAddressChanged(it) }, singleLine = false, maxLines = 3)

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (editMode) {
                                profileViewModel.saveProfileChanges(context)
                            } else {
                                editMode = true
                                resetEditableFieldsToCurrentProfileData(user)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !(editMode && updateResult is UpdateProfileResult.Loading)
                    ) {
                        if (editMode && updateResult is UpdateProfileResult.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(imageVector = if (editMode) Icons.Default.Check else Icons.Default.Edit, contentDescription = if (editMode) "Simpan" else "Edit")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (editMode) "Simpan Perubahan" else "Edit Profile", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Button(
                        onClick = {
                            showLogoutDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("LOG OUT", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(currentUiState.message)
                }
            }
            ProfileUiState.Idle -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("Tidak ada data profil yang tersedia.")
                }
            }
        }
    }
}


@Composable
fun ProfileDataField(
    label: String,
    value: String,
    editMode: Boolean,
    onValueChange: (String) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        val isEditableField = (label == "Nama Lengkap" || label == "Alamat")
        if (editMode && isEditableField) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = if (singleLine) imeAction else ImeAction.Default
                ),
                singleLine = singleLine,
                maxLines = maxLines,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = value.ifEmpty { "-" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
    }
}
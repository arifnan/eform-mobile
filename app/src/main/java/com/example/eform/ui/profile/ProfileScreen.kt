package com.example.eform.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler // <<< --- IMPORT BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults.cardColors
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.eform.data.database.UserDao
import com.example.eform.data.model.UserEntity
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userDao: UserDao,
    userIdentifier: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<UserEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var editMode by remember { mutableStateOf(false) }
    var editableName by remember { mutableStateOf("") }
    var editableEmail by remember { mutableStateOf("") }
    var editableNip by remember { mutableStateOf("") }
    var editableAddress by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    fun loadUserData(user: UserEntity?) {
        currentUser = user
        user?.let {
            editableName = it.name
            editableEmail = it.email
            editableNip = it.nip
            editableAddress = it.address ?: ""
        }
    }

    LaunchedEffect(userIdentifier) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val userByNip = userDao.getUserByNip(userIdentifier)
            val user = if (userByNip != null) userByNip else userDao.getUserByEmail(userIdentifier)
            withContext(Dispatchers.Main) {
                loadUserData(user)
                isLoading = false
            }
        }
    }

    // Tangani tombol kembali sistem saat dalam mode edit
    if (editMode) {
        BackHandler(enabled = true) {
            editMode = false // Keluar dari mode edit
            currentUser?.let { loadUserData(it) } // Reset perubahan
            Toast.makeText(context, "Edit dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (editMode) {
                            editMode = false
                            currentUser?.let { loadUserData(it) }
                            Toast.makeText(context, "Edit dibatalkan", Toast.LENGTH_SHORT).show()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (editMode) "Batal Edit" else "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            currentUser?.let { user ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
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
                        if (imageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Default Profile",
                                modifier = Modifier.fillMaxSize(),
                                tint = Color.DarkGray
                            )
                        }
                        if (editMode) {
                            Box(
                                contentAlignment = Alignment.BottomEnd,
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit, contentDescription = "Edit Foto",
                                    tint = Color.White,
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).padding(4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (editMode) editableName else user.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileDataField(
                        label = "Nama Lengkap",
                        value = editableName,
                        editMode = editMode,
                        onValueChange = { editableName = it }
                    )
                    ProfileDataField(
                        label = "Email",
                        value = user.email,
                        editMode = false
                    )
                    if (user.role.equals("guru", ignoreCase = true) && user.nip.isNotBlank()) {
                        ProfileDataField(
                            label = "NIP",
                            value = user.nip,
                            editMode = false
                        )
                    }
                    ProfileDataField(
                        label = "Alamat",
                        value = editableAddress,
                        editMode = editMode,
                        onValueChange = { editableAddress = it },
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (editMode) {
                                coroutineScope.launch {
                                    val updatedUser = user.copy(
                                        name = editableName,
                                        address = editableAddress.ifBlank { null }
                                    )
                                    withContext(Dispatchers.IO) {
                                        userDao.updateUser(updatedUser)
                                    }
                                    loadUserData(updatedUser)
                                    editMode = false
                                    Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                editMode = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (editMode) "Simpan Perubahan" else "Edit Profile"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editMode) "Simpan Perubahan" else "Edit Profile",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                navController.navigate(Screen.Role.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("LOG OUT", color = MaterialTheme.colorScheme.onError)
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Gagal memuat data pengguna atau pengguna tidak ditemukan.")
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
        // Hanya Nama dan Alamat yang bisa jadi TextField (sesuai logika tombol edit tunggal)
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
                colors = cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = value.ifEmpty { "-" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
    }
}

// Preview dan FakeUserDao bisa tetap sama
class FakeUserDaoForProfilePreview : UserDao {
    private val dummyUserStudent = UserEntity(id = 1, name = "Murid Nanda", email = "nanda.murid@example.com", nip = "", password = "password123", role = "siswa", address = "Jl. Pelajar No. 1, Medan")
    private val dummyUserTeacher = UserEntity(id = 2, name = "Guru Nanda", email = "nanda.guru@example.com", nip = "123456789012345678", password = "password123", role = "guru", address = "Jl. Mengajar No. 10, Medan")

    override suspend fun insertUser(user: UserEntity) {}
    override suspend fun updateUser(user: UserEntity) {}
    override suspend fun getUserByEmail(email: String): UserEntity? {
        return when (email) {
            dummyUserStudent.email -> dummyUserStudent
            dummyUserTeacher.email -> dummyUserTeacher
            else -> null
        }
    }
    override suspend fun getUserByNip(nip: String): UserEntity? {
        return if (nip == dummyUserTeacher.nip) dummyUserTeacher else null
    }
}

@Preview(showBackground = true, name = "Profile Screen Student View Mode")
@Composable
fun PreviewProfileScreenStudentView() {
    EformTheme {
        ProfileScreen(
            navController = rememberNavController(),
            userDao = FakeUserDaoForProfilePreview(),
            userIdentifier = "nanda.murid@example.com"
        )
    }
}

@Preview(showBackground = true, name = "Profile Screen Teacher View Mode")
@Composable
fun PreviewProfileScreenTeacherView() {
    EformTheme {
        ProfileScreen(
            navController = rememberNavController(),
            userDao = FakeUserDaoForProfilePreview(),
            userIdentifier = "123456789012345678"
        )
    }
}
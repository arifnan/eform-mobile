package com.example.eform.ui.response

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.eform.utils.LocationHelper
import com.example.eform.utils.CameraCaptureHelper

@Composable
fun FormAnswerScreen (formId: Int, navController: NavController) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val cameraHelper = remember { CameraCaptureHelper(context) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    var locationGranted by remember { mutableStateOf(false) }
    var locationStatus by remember { mutableStateOf("Belum Diambil") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            locationGranted = granted
            if (granted) {
                locationHelper.getCurrentLocation(object : LocationHelper.LocationCallback {
                    override fun onLocationResult(lat: Double, lng: Double) {
                        val withinSchool = locationHelper.isWithinRadius(
                            currentLat = lat,
                            currentLng = lng,
                            targetLat = 5.1836,
                            targetLng = 97.1443
                        )

                        locationStatus = if (withinSchool) {
                            "✅ Lokasi valid ($lat, $lng)"
                        } else {
                            "❌ Di luar area yang diizinkan ($lat, $lng)"
                        }
                    }

                    override fun onError(message: String) {
                        locationStatus = "❌ $message"
                    }
                })
            } else {
                Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            capturedImageUri = cameraHelper.photoUri
        } else {
            Toast.makeText(context, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_FINE_LOCATION
        } else Manifest.permission.ACCESS_COARSE_LOCATION

        locationPermissionLauncher.launch(permission)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Isi Formulir", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Status Lokasi: $locationStatus")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val intent = cameraHelper.createCameraIntent()
            cameraLauncher.launch(intent)
        }) {
            Text("Ambil Foto")
        }

        capturedImageUri?.let { uri ->
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Foto jawaban",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            if (!locationStatus.contains("✅")) {
                Toast.makeText(context, "Anda tidak berada di lokasi yang diizinkan", Toast.LENGTH_SHORT).show()
                return@Button
            }
            if (capturedImageUri == null) {
                Toast.makeText(context, "Silakan ambil gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@Button
            }
            // TODO: Simpan jawaban + lokasi + URI foto
        }) {
            Text("Kirim Jawaban")
        }
    }
}

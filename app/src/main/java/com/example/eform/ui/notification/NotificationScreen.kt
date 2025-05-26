package com.example.eform.ui.notification

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done // Import Done untuk latar belakang
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eform.data.database.AppDatabase
import com.example.eform.data.model.NotificationEntity
import com.example.eform.ui.components.StandardTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    userIdentifier: String
) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val notificationDao = db.notificationDao()
    val userDao = db.userDao()
    val coroutineScope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<NotificationEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUserId by remember { mutableStateOf<Int?>(null) }

    suspend fun getUserId(identifier: String): Int? {
        var user = userDao.getUserByNip(identifier)
        if (user == null) {
            user = userDao.getUserByEmail(identifier)
        }
        return user?.id
    }

    fun loadNotifications() {
        coroutineScope.launch {
            isLoading = true
            val userIdToLoad = currentUserId ?: getUserId(userIdentifier)
            if (userIdToLoad != null) {
                currentUserId = userIdToLoad // Simpan jika baru didapatkan
                withContext(Dispatchers.IO) {
                    notifications = notificationDao.getNotificationsForUser(userIdToLoad)
                }
            } else {
                notifications = emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(userIdentifier) {
        withContext(Dispatchers.IO) { // Dapatkan userId di IO thread
            currentUserId = getUserId(userIdentifier)
        }
        loadNotifications() // Kemudian muat notifikasi
    }

    Scaffold(
        topBar = {
            StandardTopAppBar(title = "Notifikasi", navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada notifikasi.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        // Menggunakan SwipeToDismissBox
                        val dismissState = rememberSwipeToDismissBoxState( // Ganti rememberDismissState
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) { // Arah swipe untuk hapus
                                    coroutineScope.launch(Dispatchers.IO) {
                                        notificationDao.deleteNotificationById(notification.id)
                                        withContext(Dispatchers.Main) {
                                            loadNotifications() // Refresh list
                                            Toast.makeText(context, "Notifikasi dihapus", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    true // Konfirmasi bahwa state bisa berubah
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox( // Ganti SwipeToDismiss
                            state = dismissState,
                            modifier = Modifier.animateItemPlacement(),
                            enableDismissFromStartToEnd = true, // Izinkan swipe dari kiri ke kanan
                            enableDismissFromEndToStart = true, // Izinkan swipe dari kanan ke kiri
                            backgroundContent = { // Konten latar belakang saat swipe
                                val color by animateColorAsState(
                                    targetValue = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> Color.Red.copy(alpha = 0.8f)
                                        SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                        else -> Color.Transparent
                                    }, label = "background color"
                                )
                                val scale by animateDpAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.dp else 1.2.dp, // Contoh animasi skala ikon
                                    label = "icon scale"
                                )

                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        modifier = Modifier.scale(if (scale > 0.dp) 1f else 0f), // Sembunyikan/tampilkan ikon dengan skala
                                        tint = Color.White
                                    )
                                }
                            }
                        ) { // Konten utama (kartu notifikasi)
                            NotificationCard(notification = notification)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
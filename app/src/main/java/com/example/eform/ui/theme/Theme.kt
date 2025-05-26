package com.example.eform.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warna primer baru Anda (diambil dari Color.kt)
private val newPrimaryColor = primary // Ini adalah Color(0xFF3D1860)

// Skema Warna Terang (Light Theme)
private val LightColorScheme = lightColorScheme(
    primary = newPrimaryColor, // Menggunakan warna primer baru
    onPrimary = Color.White, // Teks/ikon di atas warna primer (putih cocok untuk #3D1860)
    primaryContainer = Color(0xFFEADDFF), // Contoh warna container, bisa disesuaikan
    onPrimaryContainer = Color(0xFF21005D), // Contoh teks di atas container, bisa disesuaikan

    secondary = Color(0xFF03DAC5), // Anda bisa sesuaikan warna sekunder jika perlu
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),

    tertiary = Color(0xFF7D5260), // Anda bisa sesuaikan warna tersier jika perlu
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = Color(0xFFF5F5F5), // Background untuk Light Theme
    onBackground = Color.Black,    // Teks/ikon di atas background

    surface = Color.White,         // Warna permukaan komponen seperti Card, Sheet
    onSurface = Color.Black,       // Teks/ikon di atas permukaan

    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),

    outline = Color(0xFF79747E)
)

// Skema Warna Gelap (Dark Theme)
private val DarkColorScheme = darkColorScheme(
    primary = newPrimaryColor, // Menggunakan warna primer yang sama untuk konsistensi merek
    onPrimary = Color.White,   // Teks/ikon di atas warna primer (putih cocok untuk #3D1860)
    primaryContainer = Color(0xFF4A008C), // Contoh warna container gelap, bisa disesuaikan
    onPrimaryContainer = Color(0xFFEADDFF), // Contoh teks di atas container gelap, bisa disesuaikan

    secondary = Color(0xFF03DAC5), // Anda bisa sesuaikan warna sekunder jika perlu
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),

    tertiary = Color(0xFFEFB8C8), // Anda bisa sesuaikan warna tersier jika perlu
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    background = Color(0xFF121212), // Background untuk Dark Theme
    onBackground = Color.White,    // Teks/ikon di atas background

    surface = Color(0xFF1C1B1F),         // Warna permukaan komponen seperti Card, Sheet
    onSurface = Color.White,       // Teks/ikon di atas permukaan

    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    outline = Color(0xFF938F99)
)

@Composable
fun EformTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Typography dari Type.kt
        content = content
    )
}
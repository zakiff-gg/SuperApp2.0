// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/theme/Theme.kt
package com.sirlasirliputeri.absenssp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp

// Sudut membulat modern & konsisten (kartu, dialog, tombol, text field, chip).
private val AbsenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private fun ColorScheme.applySsp(s: SspColorScheme): ColorScheme = copy(
    primary = s.primary, onPrimary = s.onPrimary,
    primaryContainer = s.primaryContainer, onPrimaryContainer = s.onPrimaryContainer,
    secondary = s.secondary, onSecondary = s.onSecondary,
    secondaryContainer = s.secondaryContainer, onSecondaryContainer = s.onSecondaryContainer,
    tertiary = s.tertiary, onTertiary = s.onTertiary,
    tertiaryContainer = s.tertiaryContainer, onTertiaryContainer = s.onTertiaryContainer,
    error = s.error, onError = s.onError,
    errorContainer = s.errorContainer, onErrorContainer = s.onErrorContainer,
    background = s.background, onBackground = s.onBackground,
    surface = s.surface, onSurface = s.onSurface,
    surfaceVariant = s.surfaceVariant, onSurfaceVariant = s.onSurfaceVariant,
    surfaceContainerLow = s.surfaceContainerLow, surfaceContainer = s.surfaceContainer,
    surfaceContainerHigh = s.surfaceContainerHigh,
    outline = s.outline, outlineVariant = s.outlineVariant
)

private val LightScheme: ColorScheme = androidx.compose.material3.lightColorScheme().applySsp(SspLightScheme)
private val DarkScheme: ColorScheme = androidx.compose.material3.darkColorScheme().applySsp(SspDarkScheme)

/**
 * Ubah nilai preferensi tersimpan ("system"/"light"/"dark") jadi boolean final
 * yang dipakai AbsenSSPTheme -- kalau "system", ikut pengaturan HP saat ini.
 */
@Composable
fun resolveDarkTheme(pref: String): Boolean = when (pref) {
    "dark" -> true
    "light" -> false
    else -> isSystemInDarkTheme()
}

@Composable
fun AbsenSSPTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AbsenTypography,
            shapes = AbsenShapes
        ) {
            // Bungkus SEMUA layar dengan padding system bars di satu tempat -- supaya
            // enableEdgeToEdge() di MainActivity aman dipakai tanpa perlu mengedit
            // setiap layar satu per satu untuk menghindari konten ketiban status bar /
            // navigation bar. Warna latar default sesuai background theme aktif.
            Surface(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
                color = MaterialTheme.colorScheme.background,
                content = content
            )
        }
    }
}

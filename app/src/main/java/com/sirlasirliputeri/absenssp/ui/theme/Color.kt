// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/theme/Color.kt
package com.sirlasirliputeri.absenssp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status mode gelap aktif, dipakai supaya token warna di bawah otomatis pilih
 * varian terang/gelap tanpa mengubah cara pemakaiannya di seluruh layar.
 * Diisi lewat CompositionLocalProvider di AbsenSSPTheme (lihat Theme.kt).
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

// =====================================================================================
// DESIGN SYSTEM — Absen SSP v2 (Material 3)
// Brand core: Deep Indigo Navy (identitas PT SSP) + Signal Red (aksi/alert) + Amber
// (peringatan) + Emerald (sukses) + Teal (aksen sekunder, baru). Palet dirancang penuh
// sesuai peran warna Material 3 (container / onContainer) supaya kontras & aksesibilitas
// konsisten di semua layar, bukan sekadar warna solid seperti versi sebelumnya.
// =====================================================================================

// ---------------- Mode Terang ----------------
private val PrimaryLight = Color(0xFF16255C)          // Indigo navy — lebih dalam & modern
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFDCE1FF)
private val OnPrimaryContainerLight = Color(0xFF0A1440)

private val SecondaryLight = Color(0xFFD7263D)         // Merah brand, sedikit lebih hangat
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val SecondaryContainerLight = Color(0xFFFFDAD9)
private val OnSecondaryContainerLight = Color(0xFF410008)

private val TertiaryLight = Color(0xFF0E7C7B)          // Teal — aksen baru utk data/insight
private val OnTertiaryLight = Color(0xFFFFFFFF)
private val TertiaryContainerLight = Color(0xFFB6F0EE)
private val OnTertiaryContainerLight = Color(0xFF00201F)

private val SuccessLight = Color(0xFF1E8E3E)
private val OnSuccessLight = Color(0xFFFFFFFF)
private val SuccessContainerLight = Color(0xFFC7F3CE)
private val OnSuccessContainerLight = Color(0xFF0A3818)

private val WarningLight = Color(0xFFB2650B)
private val OnWarningLight = Color(0xFFFFFFFF)
private val WarningContainerLight = Color(0xFFFFDDB3)
private val OnWarningContainerLight = Color(0xFF3D2400)

private val ErrorLight = Color(0xFFBA1A1A)
private val OnErrorLight = Color(0xFFFFFFFF)
private val ErrorContainerLight = Color(0xFFFFDAD6)
private val OnErrorContainerLight = Color(0xFF410002)

private val BackgroundLight = Color(0xFFF7F7FB)
private val OnBackgroundLight = Color(0xFF1A1B22)
private val SurfaceLight = Color(0xFFFFFFFF)
private val OnSurfaceLight = Color(0xFF1A1B22)
private val SurfaceVariantLight = Color(0xFFE7E7EF)
private val OnSurfaceVariantLight = Color(0xFF54566B)
private val SurfaceContainerLowLight = Color(0xFFF1F1F8)
private val SurfaceContainerLight = Color(0xFFECECF5)
private val SurfaceContainerHighLight = Color(0xFFE6E6F0)
private val OutlineLight = Color(0xFFC9C9D6)
private val OutlineVariantLight = Color(0xFFDEDEE8)

// ---------------- Mode Gelap ----------------
private val PrimaryDark = Color(0xFFBAC3FF)
private val OnPrimaryDark = Color(0xFF15205C)
private val PrimaryContainerDark = Color(0xFF2C3878)
private val OnPrimaryContainerDark = Color(0xFFDCE1FF)

private val SecondaryDark = Color(0xFFFFB3AF)
private val OnSecondaryDark = Color(0xFF680007)
private val SecondaryContainerDark = Color(0xFF930014)
private val OnSecondaryContainerDark = Color(0xFFFFDAD9)

private val TertiaryDark = Color(0xFF80D5D3)
private val OnTertiaryDark = Color(0xFF003736)
private val TertiaryContainerDark = Color(0xFF00504F)
private val OnTertiaryContainerDark = Color(0xFFB6F0EE)

private val SuccessDark = Color(0xFF8DDB9A)
private val OnSuccessDark = Color(0xFF0A3818)
private val SuccessContainerDark = Color(0xFF14562A)
private val OnSuccessContainerDark = Color(0xFFC7F3CE)

private val WarningDark = Color(0xFFFFB870)
private val OnWarningDark = Color(0xFF3D2400)
private val WarningContainerDark = Color(0xFF864C00)
private val OnWarningContainerDark = Color(0xFFFFDDB3)

private val ErrorDark = Color(0xFFFFB4AB)
private val OnErrorDark = Color(0xFF690005)
private val ErrorContainerDark = Color(0xFF93000A)
private val OnErrorContainerDark = Color(0xFFFFDAD6)

private val BackgroundDark = Color(0xFF121319)
private val OnBackgroundDark = Color(0xFFE3E2EA)
private val SurfaceDark = Color(0xFF1A1B22)
private val OnSurfaceDark = Color(0xFFE3E2EA)
private val SurfaceVariantDark = Color(0xFF2C2D38)
private val OnSurfaceVariantDark = Color(0xFFC5C5D2)
private val SurfaceContainerLowDark = Color(0xFF17181F)
private val SurfaceContainerDark = Color(0xFF1E1F27)
private val SurfaceContainerHighDark = Color(0xFF282A33)
private val OutlineDark = Color(0xFF56576A)
private val OutlineVariantDark = Color(0xFF3C3D4A)

// ---------------- Token @Composable yang dipakai langsung di layar ----------------
// Nama lama dipertahankan sebagai alias supaya kode di seluruh layar tetap kompatibel,
// tapi sekarang menunjuk ke palet baru yang jauh lebih lengkap perannya.
val NavyDark: Color @Composable get() = if (LocalDarkTheme.current) PrimaryDark else PrimaryLight
val RedAccent: Color @Composable get() = if (LocalDarkTheme.current) SecondaryDark else SecondaryLight
val CreamBg: Color @Composable get() = if (LocalDarkTheme.current) BackgroundDark else BackgroundLight
val BlueGray: Color @Composable get() = if (LocalDarkTheme.current) TertiaryDark else TertiaryLight
val GreenSuccess: Color @Composable get() = if (LocalDarkTheme.current) SuccessDark else SuccessLight
val OrangeWarning: Color @Composable get() = if (LocalDarkTheme.current) WarningDark else WarningLight
val CardWhite: Color @Composable get() = if (LocalDarkTheme.current) SurfaceContainerDark else SurfaceLight
val DividerGray: Color @Composable get() = if (LocalDarkTheme.current) OutlineVariantDark else OutlineVariantLight
val TextSecondary: Color @Composable get() = if (LocalDarkTheme.current) OnSurfaceVariantDark else OnSurfaceVariantLight

// ---------------- Token tambahan (baru) untuk komponen v2 ----------------
val SuccessContainer: Color @Composable get() = if (LocalDarkTheme.current) SuccessContainerDark else SuccessContainerLight
val OnSuccessContainer: Color @Composable get() = if (LocalDarkTheme.current) OnSuccessContainerDark else OnSuccessContainerLight
val WarningContainer: Color @Composable get() = if (LocalDarkTheme.current) WarningContainerDark else WarningContainerLight
val OnWarningContainer: Color @Composable get() = if (LocalDarkTheme.current) OnWarningContainerDark else OnWarningContainerLight
val TertiaryContainer: Color @Composable get() = if (LocalDarkTheme.current) TertiaryContainerDark else TertiaryContainerLight
val OnTertiaryContainerColor: Color @Composable get() = if (LocalDarkTheme.current) OnTertiaryContainerDark else OnTertiaryContainerLight
val SurfaceContainerLowColor: Color @Composable get() = if (LocalDarkTheme.current) SurfaceContainerLowDark else SurfaceContainerLowLight
val SurfaceContainerHighColor: Color @Composable get() = if (LocalDarkTheme.current) SurfaceContainerHighDark else SurfaceContainerHighLight

/** Skema M3 penuh — dipakai oleh Theme.kt untuk MaterialTheme.colorScheme. */
internal data class SspColorScheme(
    val primary: Color, val onPrimary: Color, val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color, val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color, val tertiaryContainer: Color, val onTertiaryContainer: Color,
    val error: Color, val onError: Color, val errorContainer: Color, val onErrorContainer: Color,
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color,
    val surfaceVariant: Color, val onSurfaceVariant: Color,
    val surfaceContainerLow: Color, val surfaceContainer: Color, val surfaceContainerHigh: Color,
    val outline: Color, val outlineVariant: Color
)

internal val SspLightScheme = SspColorScheme(
    PrimaryLight, OnPrimaryLight, PrimaryContainerLight, OnPrimaryContainerLight,
    SecondaryLight, OnSecondaryLight, SecondaryContainerLight, OnSecondaryContainerLight,
    TertiaryLight, OnTertiaryLight, TertiaryContainerLight, OnTertiaryContainerLight,
    ErrorLight, OnErrorLight, ErrorContainerLight, OnErrorContainerLight,
    BackgroundLight, OnBackgroundLight,
    SurfaceLight, OnSurfaceLight,
    SurfaceVariantLight, OnSurfaceVariantLight,
    SurfaceContainerLowLight, SurfaceContainerLight, SurfaceContainerHighLight,
    OutlineLight, OutlineVariantLight
)

internal val SspDarkScheme = SspColorScheme(
    PrimaryDark, OnPrimaryDark, PrimaryContainerDark, OnPrimaryContainerDark,
    SecondaryDark, OnSecondaryDark, SecondaryContainerDark, OnSecondaryContainerDark,
    TertiaryDark, OnTertiaryDark, TertiaryContainerDark, OnTertiaryContainerDark,
    ErrorDark, OnErrorDark, ErrorContainerDark, OnErrorContainerDark,
    BackgroundDark, OnBackgroundDark,
    SurfaceDark, OnSurfaceDark,
    SurfaceVariantDark, OnSurfaceVariantDark,
    SurfaceContainerLowDark, SurfaceContainerDark, SurfaceContainerHighDark,
    OutlineDark, OutlineVariantDark
)

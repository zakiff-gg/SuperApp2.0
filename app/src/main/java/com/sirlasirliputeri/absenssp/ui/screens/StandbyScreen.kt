// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/screens/StandbyScreen.kt
package com.sirlasirliputeri.absenssp.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirlasirliputeri.absenssp.R
import com.sirlasirliputeri.absenssp.model.AbsensiHariIniEntry
import com.sirlasirliputeri.absenssp.model.TapResult
import com.sirlasirliputeri.absenssp.ui.components.ProjectPickerDialog
import com.sirlasirliputeri.absenssp.ui.components.RincianKehadiranDialog
import com.sirlasirliputeri.absenssp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Halaman utama (Standby Dashboard) -- gaya modern & bersih ala iOS, model grid dashboard.
 */
@Composable
fun StandbyScreen(
    isOnline: Boolean,
    jamValid: Boolean,
    totalMasukHariIni: Int,
    absensiHariIni: List<AbsensiHariIniEntry>,
    showProjectPicker: Boolean,
    daftarProyek: List<String>,
    tapResult: TapResult?,
    onProyekDipilih: (String) -> Unit,
    onDismissProjectPicker: () -> Unit,
    onLogoTapped: () -> Unit,
    onGotoRegister: () -> Unit,
    onGotoHistory: () -> Unit,
    onGotoProfil: () -> Unit,
    onGotoOperasional: () -> Unit,
    onGotoGudang: () -> Unit,
    onGotoInvoice: () -> Unit,
    onGotoSinkronisasi: () -> Unit,
    isSyncing: Boolean,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var showRincian by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar: logo + nama perusahaan (kiri), status pill (kanan)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLogoTapped() } // Ketuk 5x untuk menu admin
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_brand),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(26.dp).width(54.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "PT SIRLA SIRLI PUTERI",
                        color = NavyDark,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(isOnline = isOnline)
                    Spacer(Modifier.width(8.dp))
                    DarkModeToggle(isDarkMode = isDarkMode, onToggle = onToggleDarkMode)
                }
            }

            // Sapaan dinamis + tanggal hari ini, menyesuaikan jam sistem
            SapaanDinamis()

            if (!isOnline) {
                InlineBanner(text = "Mode Offline · Absen aman tersimpan di HP", color = OrangeWarning)
            }
            if (!jamValid) {
                InlineBanner(text = "Jam Perangkat Tidak Valid — Hubungi Admin", color = RedAccent)
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                // Ikon NFC besar dengan animasi "bernapas" (pulsing) -- isyarat visual
                // bahwa layar ini aktif mendengarkan tap kartu, bukan gambar diam.
                PulsingNfcIcon()

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Tempelkan kartu ke bagian belakang HP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                // Kartu counter -- bisa diketuk untuk lihat rincian per proyek & nama.
                // Gradien halus + shadow lembut ala kartu premium, bukan putih polos.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp), ambientColor = NavyDark.copy(alpha = 0.08f), spotColor = NavyDark.copy(alpha = 0.08f))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.verticalGradient(listOf(CardWhite, NavyDark.copy(alpha = 0.045f))))
                        .clickable { showRincian = true }
                ) {
                    // Watermark grafis tipis di pojok kartu
                    val warnaWatermark = NavyDark.copy(alpha = 0.03f)
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = warnaWatermark,
                            radius = size.width * 0.45f,
                            center = Offset(size.width * 0.92f, -size.height * 0.10f)
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NavyDark.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            PeopleIcon(color = NavyDark, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "TOTAL KARYAWAN MASUK HARI INI",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$totalMasukHariIni",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Text("Orang", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text("Ketuk untuk lihat rincian", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                tapResult?.let { result ->
                    val bgColor = when (result.status) {
                        "SUKSES" -> GreenSuccess
                        "SUDAH_ABSEN" -> OrangeWarning
                        else -> RedAccent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 14.dp, shape = RoundedCornerShape(20.dp), ambientColor = bgColor.copy(alpha = 0.25f), spotColor = bgColor.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgColor)
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar inisial + lencana status -- konfirmasi visual yang lebih
                            // kuat daripada sekadar teks, supaya user yakin absennya tercatat
                            // untuk orang yang benar.
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        initialFromNama(result.nama),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (result.status == "SUKSES") {
                                        CentangIcon(color = bgColor, modifier = Modifier.size(12.dp))
                                    } else {
                                        SilangIcon(color = bgColor, modifier = Modifier.size(11.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            if (result.nama.isNotEmpty()) {
                                Text(result.nama, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                                Spacer(Modifier.height(2.dp))
                            }
                            Text(result.pesan, color = Color.White, textAlign = TextAlign.Center, fontSize = 15.sp)
                            if (result.keterangan.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(result.keterangan, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Menu dalam susunan GRID 2 kolom ala dashboard -- lebih ringkas & memanfaatkan
                // ruang layar lebar, dibanding daftar memanjang ke bawah.
                // Invoice & Gudang Alat ditonjolkan (warna solid) sebagai fitur prioritas.
                MenuGridRow {
                    MenuGridItem(
                        modifier = Modifier.weight(1f),
                        bgColor = BlueGray.copy(alpha = 0.10f),
                        iconColor = BlueGray,
                        icon = { RekamKartuIcon(color = it, modifier = Modifier.size(26.dp)) },
                        label = "Rekam Kartu Baru",
                        onClick = onGotoRegister
                    )
                    MenuGridItem(
                        modifier = Modifier.weight(1f),
                        bgColor = GreenSuccess.copy(alpha = 0.10f),
                        iconColor = GreenSuccess,
                        icon = { RiwayatIcon(color = it, modifier = Modifier.size(26.dp)) },
                        label = "Riwayat & Pencarian",
                        onClick = onGotoHistory
                    )
                }
                Spacer(Modifier.height(12.dp))
                MenuGridRow {
                    MenuGridItem(
                        modifier = Modifier.weight(1f),
                        bgColor = BlueGray.copy(alpha = 0.10f),
                        iconColor = BlueGray,
                        icon = { ProfilIcon(color = it, modifier = Modifier.size(26.dp)) },
                        label = "Profil Karyawan",
                        onClick = onGotoProfil
                    )
                    MenuGridItem(
                        modifier = Modifier.weight(1f),
                        bgColor = RedAccent.copy(alpha = 0.10f),
                        iconColor = RedAccent,
                        icon = { KameraIcon(color = it, modifier = Modifier.size(26.dp)) },
                        label = "Laporan Operasional",
                        onClick = onGotoOperasional
                    )
                }
                Spacer(Modifier.height(12.dp))
                MenuGridRow {
                    // Fitur prioritas: latar solid, ikon & teks putih -- menonjol dari yang lain.
                    MenuGridItem(
                        modifier = Modifier.weight(1f),
                        bgColor = GreenSuccess,
                        iconColor = Color.White,
                        textColor = Color.White,
                        icon = { GudangAlatIcon(color = it, modifier = Modifier.size(26.dp)) },
                        label = "Gudang Alat",
                        onClick = onGotoGudang
                    )
                    MenuGridItem(
                        modifier = Modifier.weight(1f),
                        bgColor = NavyDark,
                        iconColor = Color.White,
                        textColor = Color.White,
                        icon = { InvoiceIcon(color = it, modifier = Modifier.size(26.dp)) },
                        label = "Invoice",
                        onClick = onGotoInvoice
                    )
                }
                Spacer(Modifier.height(12.dp))
                MenuGridRow {
                    MenuGridItem(
                        modifier = Modifier.weight(1f),
                        bgColor = OrangeWarning.copy(alpha = 0.10f),
                        iconColor = OrangeWarning,
                        icon = { SinkronisasiIcon(color = it, modifier = Modifier.size(26.dp)) },
                        label = "Sinkronisasi Data",
                        onClick = onGotoSinkronisasi,
                        isLoading = isSyncing
                    )
                    Spacer(modifier = Modifier.weight(1f)) // slot kosong, ganjil jumlah menu
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "© 2026 SSP Super App",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        if (showProjectPicker) {
            ProjectPickerDialog(
                daftarProyek = daftarProyek,
                onProyekDipilih = onProyekDipilih,
                onDismiss = onDismissProjectPicker
            )
        }

        if (showRincian) {
            RincianKehadiranDialog(
                absensiHariIni = absensiHariIni,
                onDismiss = { showRincian = false }
            )
        }
    }
}

/** Sapaan dinamis (Pagi/Siang/Sore/Malam) + tanggal hari ini, dihitung dari jam sistem HP. */
@Composable
private fun SapaanDinamis() {
    val (sapaan, emoji) = remember {
        val jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            jam in 4..10 -> "Selamat Pagi, Tim!" to "☀️"
            jam in 11..14 -> "Selamat Siang, Tim!" to "🌤️"
            jam in 15..17 -> "Selamat Sore, Tim!" to "🌇"
            else -> "Selamat Malam, Tim!" to "🌙"
        }
    }
    val tanggalHariIni = remember {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        sdf.format(Calendar.getInstance().time)
    }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)) {
        Text("$sapaan $emoji", style = MaterialTheme.typography.titleLarge, color = NavyDark, fontWeight = FontWeight.SemiBold)
        Text(tanggalHariIni, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

/** Ikon NFC dalam kartu bulat dengan animasi "bernapas" (skala + transparansi berdenyut perlahan). */
@Composable
private fun PulsingNfcIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfc_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nfc_ring_alpha"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
        // Cincin luar yang membesar & memudar -- efek "gelombang" berdenyut
        Box(
            modifier = Modifier
                .size(104.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(NavyDark.copy(alpha = pulseAlpha))
        )
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(CardWhite),
            contentAlignment = Alignment.Center
        ) {
            NfcIcon(color = NavyDark, modifier = Modifier.size(38.dp))
        }
    }
}

/** Satu baris grid berisi 2 kotak menu bersisian (Row biasa, bukan LazyVerticalGrid, supaya bisa ikut scroll induk). */
@Composable
private fun MenuGridRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** Satu kotak menu gaya dashboard (squircle): ikon di tengah atas, label di bawahnya. */
@Composable
private fun MenuGridItem(
    modifier: Modifier = Modifier,
    bgColor: Color,
    iconColor: Color,
    textColor: Color = NavyDark,
    icon: @Composable (Color) -> Unit,
    label: String,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp), ambientColor = NavyDark.copy(alpha = 0.05f), spotColor = NavyDark.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .border(width = 1.dp, color = DividerGray.copy(alpha = 0.6f), shape = RoundedCornerShape(24.dp))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(vertical = 20.dp, horizontal = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = iconColor)
                } else {
                    icon(iconColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Tombol bulat kecil buat ganti mode terang/gelap -- ikon matahari (terang) / bulan (gelap). */
@Composable
private fun DarkModeToggle(isDarkMode: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(NavyDark.copy(alpha = 0.08f))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        val badgeBg = CreamBg
        val navy = NavyDark
        Canvas(modifier = Modifier.size(16.dp)) {
            val w = size.width
            val h = size.height
            if (isDarkMode) {
                // Ikon bulan sabit (lingkaran utama + lingkaran "potongan" warna latar)
                drawCircle(color = navy, radius = w * 0.42f, center = Offset(w * 0.42f, h * 0.5f))
                drawCircle(color = badgeBg, radius = w * 0.36f, center = Offset(w * 0.58f, h * 0.40f))
            } else {
                // Ikon matahari
                drawCircle(color = navy, radius = w * 0.26f, center = Offset(w * 0.5f, h * 0.5f))
                val stroke = Stroke(width = w * 0.08f)
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val cx = w * 0.5f
                    val cy = h * 0.5f
                    val r1 = w * 0.36f
                    val r2 = w * 0.48f
                    drawLine(
                        color = navy,
                        start = Offset(cx + (r1 * kotlin.math.cos(angle)).toFloat(), cy + (r1 * kotlin.math.sin(angle)).toFloat()),
                        end = Offset(cx + (r2 * kotlin.math.cos(angle)).toFloat(), cy + (r2 * kotlin.math.sin(angle)).toFloat()),
                        strokeWidth = stroke.width
                    )
                }
            }
        }
    }
}

/** Ambil 1-2 huruf inisial dari nama (untuk avatar bulat di kartu konfirmasi tap). */
private fun initialFromNama(nama: String): String {
    val kata = nama.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        kata.isEmpty() -> "?"
        kata.size == 1 -> kata[0].take(1).uppercase()
        else -> (kata[0].take(1) + kata[1].take(1)).uppercase()
    }
}

@Composable
private fun CentangIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.18f)
        drawLine(color = color, start = Offset(w * 0.08f, h * 0.52f), end = Offset(w * 0.40f, h * 0.84f), strokeWidth = stroke.width)
        drawLine(color = color, start = Offset(w * 0.40f, h * 0.84f), end = Offset(w * 0.92f, h * 0.18f), strokeWidth = stroke.width)
    }
}

@Composable
private fun SilangIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.18f)
        drawLine(color = color, start = Offset(w * 0.12f, h * 0.12f), end = Offset(w * 0.88f, h * 0.88f), strokeWidth = stroke.width)
        drawLine(color = color, start = Offset(w * 0.88f, h * 0.12f), end = Offset(w * 0.12f, h * 0.88f), strokeWidth = stroke.width)
    }
}

@Composable
private fun StatusPill(isOnline: Boolean) {
    val color = if (isOnline) GreenSuccess else RedAccent
    val label = if (isOnline) "Online" else "Offline"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InlineBanner(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Medium, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ---------------- Ikon vektor ringan (Canvas), tanpa perlu pustaka icon tambahan ----------------

@Composable
private fun NfcIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Badan HP
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.09f)
        )
        // Gelombang sinyal NFC
        val stroke = Stroke(width = w * 0.08f)
        drawArc(color = color, startAngle = -55f, sweepAngle = 110f, useCenter = false,
            topLeft = Offset(w * 0.42f, h * 0.28f), size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.44f), style = stroke)
        drawArc(color = color, startAngle = -55f, sweepAngle = 110f, useCenter = false,
            topLeft = Offset(w * 0.58f, h * 0.18f), size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.64f), style = stroke)
    }
}

@Composable
private fun PeopleIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = color, radius = w * 0.16f, center = Offset(w * 0.35f, h * 0.32f))
        drawArc(color = color, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(w * 0.08f, h * 0.5f), size = androidx.compose.ui.geometry.Size(w * 0.54f, h * 0.42f))
        drawCircle(color = color, radius = w * 0.13f, center = Offset(w * 0.72f, h * 0.36f))
        drawArc(color = color, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(w * 0.5f, h * 0.54f), size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.36f))
    }
}

@Composable
private fun RekamKartuIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.16f),
            size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.68f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = w * 0.09f)
        )
        drawLine(color = color, start = Offset(w * 0.28f, h * 0.42f), end = Offset(w * 0.72f, h * 0.42f), strokeWidth = w * 0.08f)
        drawLine(color = color, start = Offset(w * 0.28f, h * 0.60f), end = Offset(w * 0.58f, h * 0.60f), strokeWidth = w * 0.08f)
    }
}

@Composable
private fun RiwayatIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawArc(
            color = color,
            startAngle = -220f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(w * 0.10f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.80f),
            style = Stroke(width = w * 0.10f)
        )
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.32f), end = Offset(w * 0.5f, h * 0.52f), strokeWidth = w * 0.09f)
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.52f), end = Offset(w * 0.66f, h * 0.6f), strokeWidth = w * 0.09f)
    }
}

@Composable
private fun ProfilIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = color, radius = w * 0.20f, center = Offset(w * 0.5f, h * 0.32f))
        drawArc(color = color, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(w * 0.14f, h * 0.52f), size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.46f))
    }
}

@Composable
private fun KameraIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.26f),
            size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.60f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = w * 0.08f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.34f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f, w * 0.05f)
        )
        drawCircle(color = color, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.58f), style = Stroke(width = w * 0.07f))
    }
}

@Composable
private fun GudangAlatIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Kotak gudang/peti alat
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.32f),
            size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = w * 0.08f)
        )
        // Atap
        drawLine(color = color, start = Offset(w * 0.06f, h * 0.32f), end = Offset(w * 0.5f, h * 0.06f), strokeWidth = w * 0.08f)
        drawLine(color = color, start = Offset(w * 0.94f, h * 0.32f), end = Offset(w * 0.5f, h * 0.06f), strokeWidth = w * 0.08f)
        // Pegangan
        drawLine(color = color, start = Offset(w * 0.36f, h * 0.60f), end = Offset(w * 0.64f, h * 0.60f), strokeWidth = w * 0.08f)
    }
}

@Composable
private fun InvoiceIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.14f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.84f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = w * 0.08f)
        )
        drawLine(color = color, start = Offset(w * 0.30f, h * 0.36f), end = Offset(w * 0.70f, h * 0.36f), strokeWidth = w * 0.07f)
        drawLine(color = color, start = Offset(w * 0.30f, h * 0.54f), end = Offset(w * 0.70f, h * 0.54f), strokeWidth = w * 0.07f)
        drawLine(color = color, start = Offset(w * 0.30f, h * 0.72f), end = Offset(w * 0.54f, h * 0.72f), strokeWidth = w * 0.07f)
    }
}

@Composable
private fun SinkronisasiIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.10f)
        drawArc(color = color, startAngle = -160f, sweepAngle = 250f, useCenter = false,
            topLeft = Offset(w * 0.10f, h * 0.10f), size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.80f), style = stroke)
        drawArc(color = color, startAngle = 20f, sweepAngle = 250f, useCenter = false,
            topLeft = Offset(w * 0.10f, h * 0.10f), size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.80f), style = stroke)
    }
}

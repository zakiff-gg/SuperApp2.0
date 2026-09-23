// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/screens/GudangScreen.kt
package com.sirlasirliputeri.absenssp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sirlasirliputeri.absenssp.model.AlatDipinjam
import com.sirlasirliputeri.absenssp.model.Karyawan
import com.sirlasirliputeri.absenssp.ui.theme.*

/**
 * Menu utama Gudang Alat -- 3 pilihan: Ambil Alat, Kembalikan Alat, dan lihat status
 * (Alat Sedang Dipinjam / histori ringkas). SEMUA transaksi di menu ini WAJIB
 * ONLINE (langsung tulis ke sheet "Gudang" via Code.gs) -- TIDAK dicache/antre
 * offline seperti Absen, karena status pinjam-kembali harus akurat real-time.
 */
@Composable
fun GudangMenuScreen(
    isOnline: Boolean,
    onGotoAmbil: () -> Unit,
    onGotoKembali: () -> Unit,
    onGotoHistori: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Gudang Alat", style = MaterialTheme.typography.headlineLarge, color = NavyDark)
            Spacer(Modifier.height(4.dp))
            Text(
                "Catat pengambilan & pengembalian alat gudang lewat tap kartu.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))

            if (!isOnline) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(RedAccent.copy(alpha = 0.12f)).padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Tidak ada koneksi internet -- menu Gudang butuh internet karena langsung tersimpan di server.",
                        color = RedAccent, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            GudangMenuRow(
                iconBgColor = GreenSuccess.copy(alpha = 0.12f),
                iconColor = GreenSuccess,
                icon = { AmbilAlatIcon(color = GreenSuccess, modifier = it) },
                label = "Ambil Alat",
                sub = "Tap kartu, lalu ketik alat yang diambil",
                onClick = onGotoAmbil
            )
            Spacer(Modifier.height(10.dp))
            GudangMenuRow(
                iconBgColor = RedAccent.copy(alpha = 0.12f),
                iconColor = RedAccent,
                icon = { KembalikanAlatIcon(color = RedAccent, modifier = it) },
                label = "Kembalikan Alat",
                sub = "Tap kartu, lalu pilih alat yang dikembalikan",
                onClick = onGotoKembali
            )
            Spacer(Modifier.height(10.dp))
            GudangMenuRow(
                iconBgColor = OrangeWarning.copy(alpha = 0.12f),
                iconColor = OrangeWarning,
                icon = { HistoriGudangIcon(color = OrangeWarning, modifier = it) },
                label = "Alat Sedang Dipinjam",
                sub = "Lihat alat apa saja, dipinjam siapa, sudah berapa hari",
                onClick = onGotoHistori
            )
        }
    }
}

@Composable
private fun GudangMenuRow(
    iconBgColor: Color,
    iconColor: Color,
    icon: @Composable (Modifier) -> Unit,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                icon(Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleLarge, color = NavyDark)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text("›", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

/**
 * Layar tunggu tap kartu untuk Ambil / Kembalikan Alat (ikon NFC + instruksi).
 * Dipakai sama untuk keduanya, judul & warna beda tergantung mode.
 */
@Composable
fun GudangTapScreen(
    judul: String,
    instruksi: String,
    warna: Color,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(26.dp)).background(warna.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                GudangNfcIcon(color = warna, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(judul, style = MaterialTheme.typography.headlineLarge, color = NavyDark, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(instruksi, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Form Ambil Alat: tampil setelah kartu ditap & dikenali. Nama alat diketik BEBAS,
 * satu alat per baris -- boleh lebih dari satu alat dalam satu transaksi.
 */
@Composable
fun GudangAmbilFormScreen(
    karyawan: Karyawan,
    daftarAlatText: String,
    onDaftarAlatChange: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onGantiKartu: () -> Unit,
    onBack: () -> Unit
) {
    val jumlahAlat = daftarAlatText.lines().map { it.trim() }.filter { it.isNotEmpty() }.size

    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
            TextButton(onClick = onGantiKartu) { Text("Bukan ${karyawan.nama}? Tap ulang", color = TextSecondary, fontSize = 12.sp) }
        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp)) {
            Text("Ambil Alat", style = MaterialTheme.typography.headlineLarge, color = NavyDark)
            Spacer(Modifier.height(2.dp))
            Text("Peminjam: ${karyawan.nama}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            Text("Nama Alat (satu per baris)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = daftarAlatText,
                onValueChange = onDaftarAlatChange,
                placeholder = { Text("Contoh:\nBor Listrik\nMeteran\nGergaji Mesin") },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = CardWhite,
                    focusedContainerColor = CardWhite,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (jumlahAlat > 0) "$jumlahAlat alat akan dicatat." else "Belum ada alat diketik.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Button(
                onClick = onSubmit,
                enabled = jumlahAlat > 0 && !isSubmitting,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Simpan Pengambilan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Form Kembalikan Alat: tampil setelah kartu ditap & dikenali. Menampilkan daftar
 * alat yang SAAT INI masih tercatat dipinjam atas nama orang ini (dari server,
 * live -- bukan cache), dipilih lewat centang, boleh lebih dari satu sekaligus.
 */
@Composable
fun GudangKembaliFormScreen(
    karyawan: Karyawan,
    isLoading: Boolean,
    daftarAlatAktif: List<AlatDipinjam>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onGantiKartu: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
            TextButton(onClick = onGantiKartu) { Text("Bukan ${karyawan.nama}? Tap ulang", color = TextSecondary, fontSize = 12.sp) }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Kembalikan Alat", style = MaterialTheme.typography.headlineLarge, color = NavyDark)
            Spacer(Modifier.height(2.dp))
            Text("Peminjam: ${karyawan.nama}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            daftarAlatAktif.isEmpty() -> {
                Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Text("Tidak ada alat gudang yang sedang tercatat dipinjam atas nama ${karyawan.nama}.", color = TextSecondary, textAlign = TextAlign.Center)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(daftarAlatAktif, key = { it.id }) { alat ->
                        val dipilih = selectedIds.contains(alat.id)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (dipilih) RedAccent.copy(alpha = 0.10f) else CardWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onToggle(alat.id) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = dipilih, onCheckedChange = { onToggle(alat.id) })
                                Spacer(Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(alat.namaAlat, color = NavyDark, fontWeight = FontWeight.SemiBold)
                                    Text("Diambil ${alat.tanggalAmbil} · sudah ${alat.jumlahHari} hari", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (daftarAlatAktif.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Button(
                    onClick = onSubmit,
                    enabled = selectedIds.isNotEmpty() && !isSubmitting,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(
                            if (selectedIds.isEmpty()) "Pilih alat yang dikembalikan" else "Kembalikan ${selectedIds.size} Alat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Histori ringkas: SEMUA alat gudang yang saat ini masih berstatus dipinjam,
 * lintas orang -- "alat ini lagi dipinjam siapa dan sudah berapa hari". Selalu
 * diambil LIVE dari server (bukan cache) tiap layar ini dibuka, supaya akurat.
 */
@Composable
fun GudangHistoriScreen(
    isLoading: Boolean,
    daftarAlatAktif: List<AlatDipinjam>,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
            TextButton(onClick = onRefresh, enabled = !isLoading) { Text("Muat Ulang", color = BlueGray) }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Alat Sedang Dipinjam", style = MaterialTheme.typography.headlineLarge, color = NavyDark)
            Spacer(Modifier.height(4.dp))
            Text("${daftarAlatAktif.size} alat belum dikembalikan", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Text(errorMessage, color = RedAccent, textAlign = TextAlign.Center)
                }
            }
            daftarAlatAktif.isEmpty() -> {
                Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Text("Semua alat gudang sudah dikembalikan.", color = TextSecondary, textAlign = TextAlign.Center)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(daftarAlatAktif.sortedByDescending { it.jumlahHari }, key = { it.id }) { alat ->
                        val warnaLama = when {
                            alat.jumlahHari >= 7 -> RedAccent
                            alat.jumlahHari >= 3 -> OrangeWarning
                            else -> GreenSuccess
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(alat.namaAlat, color = NavyDark, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.height(2.dp))
                                    Text("Dipinjam: ${alat.nama}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text("Sejak ${alat.tanggalAmbil}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(warnaLama.copy(alpha = 0.14f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "${alat.jumlahHari} hari",
                                        color = warnaLama,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dialog hasil sederhana untuk transaksi Gudang (Ambil / Kembalikan Alat). */
@Composable
fun GudangResultDialog(isSukses: Boolean, pesan: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isSukses) "Berhasil" else "Gagal",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isSukses) GreenSuccess else RedAccent
                )
                Spacer(Modifier.height(8.dp))
                Text(pesan, color = TextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) { Text("Tutup") }
            }
        }
    }
}

// ---------------- Ikon vektor ringan (Canvas), gaya sama dengan StandbyScreen ----------------

@Composable
private fun GudangNfcIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.10f),
            size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.09f)
        )
        val stroke = Stroke(width = w * 0.08f)
        drawArc(color = color, startAngle = -55f, sweepAngle = 110f, useCenter = false,
            topLeft = Offset(w * 0.42f, h * 0.28f), size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.44f), style = stroke)
        drawArc(color = color, startAngle = -55f, sweepAngle = 110f, useCenter = false,
            topLeft = Offset(w * 0.58f, h * 0.18f), size = androidx.compose.ui.geometry.Size(w * 0.42f, h * 0.64f), style = stroke)
    }
}

@Composable
private fun AmbilAlatIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.30f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.09f)
        )
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.06f), end = Offset(w * 0.5f, h * 0.46f), strokeWidth = w * 0.09f)
        drawLine(color = color, start = Offset(w * 0.30f, h * 0.24f), end = Offset(w * 0.5f, h * 0.06f), strokeWidth = w * 0.09f)
        drawLine(color = color, start = Offset(w * 0.70f, h * 0.24f), end = Offset(w * 0.5f, h * 0.06f), strokeWidth = w * 0.09f)
    }
}

@Composable
private fun KembalikanAlatIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.09f)
        )
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.94f), end = Offset(w * 0.5f, h * 0.54f), strokeWidth = w * 0.09f)
        drawLine(color = color, start = Offset(w * 0.30f, h * 0.76f), end = Offset(w * 0.5f, h * 0.94f), strokeWidth = w * 0.09f)
        drawLine(color = color, start = Offset(w * 0.70f, h * 0.76f), end = Offset(w * 0.5f, h * 0.94f), strokeWidth = w * 0.09f)
    }
}

@Composable
private fun HistoriGudangIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawArc(
            color = color, startAngle = -220f, sweepAngle = 260f, useCenter = false,
            topLeft = Offset(w * 0.10f, h * 0.10f), size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.80f),
            style = Stroke(width = w * 0.10f)
        )
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.32f), end = Offset(w * 0.5f, h * 0.52f), strokeWidth = w * 0.09f)
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.52f), end = Offset(w * 0.66f, h * 0.6f), strokeWidth = w * 0.09f)
    }
}

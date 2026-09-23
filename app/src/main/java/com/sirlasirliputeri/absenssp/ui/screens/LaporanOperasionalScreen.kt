// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/screens/LaporanOperasionalScreen.kt
package com.sirlasirliputeri.absenssp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirlasirliputeri.absenssp.model.Karyawan
import com.sirlasirliputeri.absenssp.model.KategoriOperasional
import com.sirlasirliputeri.absenssp.model.OperasionalEntry
import com.sirlasirliputeri.absenssp.model.PendingOperasionalSubmission
import com.sirlasirliputeri.absenssp.model.RiwayatOperasionalEntry
import com.sirlasirliputeri.absenssp.model.TipeOperasional
import com.sirlasirliputeri.absenssp.ui.theme.*
import com.sirlasirliputeri.absenssp.util.RupiahFormatter

/**
 * Layar pemilihan identitas -- dipakai BERSAMA oleh Laporan Operasional maupun
 * Profil Karyawan (keduanya TIDAK PAKAI tap kartu lagi). Cukup pilih nama SEKALI,
 * tersimpan permanen di HP, dan bisa diganti kapan saja lewat tautan "Ganti".
 */
@Composable
fun PilihIdentitasScreen(
    judul: String,
    subjudul: String,
    daftarKaryawan: List<Karyawan>,
    onPilih: (Karyawan) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val hasil = remember(query, daftarKaryawan) {
        if (query.isBlank()) daftarKaryawan
        else daftarKaryawan.filter { it.nama.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(judul, style = MaterialTheme.typography.headlineLarge, color = NavyDark)
            Spacer(Modifier.height(4.dp))
            Text(subjudul, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari nama...") },
                shape = RoundedCornerShape(50),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = CardWhite,
                    focusedContainerColor = CardWhite,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        if (daftarKaryawan.isEmpty()) {
            Text(
                "Belum ada data karyawan. Lakukan Sinkronisasi Data terlebih dahulu.",
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(hasil) { k ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onPilih(k) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(k.nama, color = NavyDark, fontWeight = FontWeight.SemiBold)
                            if (k.posisi.isNotEmpty()) {
                                Text(k.posisi, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tampil setelah identitas dikenali: Nama, Saldo berjalan (offline dari cache),
 * tombol Buat Laporan Baru, dan daftar laporan hari ini milik orang ini (masing-masing
 * bisa diketuk untuk diedit). Laporan yang sedang/gagal dikirim di background juga
 * muncul di sini dengan status "Mengirim..." / "Gagal, ketuk untuk coba lagi".
 */
@Composable
fun OperasionalMenuScreen(
    nama: String,
    saldo: Double,
    laporanHariIni: List<OperasionalEntry>,
    pendingSubmissions: List<PendingOperasionalSubmission>,
    onBuatBaru: () -> Unit,
    onEditLaporan: (OperasionalEntry) -> Unit,
    onRetryPending: (PendingOperasionalSubmission) -> Unit,
    onGantiIdentitas: () -> Unit,
    onGotoRiwayat: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
            TextButton(onClick = onGantiIdentitas) { Text("Bukan $nama? Ganti", color = TextSecondary, fontSize = 12.sp) }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(nama, style = MaterialTheme.typography.headlineLarge, color = NavyDark)
            Spacer(Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SALDO ANDA", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        RupiahFormatter.format(saldo),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (saldo < 0) RedAccent else NavyDark
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onBuatBaru,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("+ Buat Laporan Baru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onGotoRiwayat,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Riwayat Laporan")
            }

            Spacer(Modifier.height(20.dp))
            Text("Laporan Hari Ini", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
        }

        if (laporanHariIni.isEmpty() && pendingSubmissions.isEmpty()) {
            Text(
                "Belum ada laporan hari ini.",
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(pendingSubmissions) { pending ->
                    val sedangGagal = pending.status == "GAGAL"
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sedangGagal) RedAccent.copy(alpha = 0.08f) else OrangeWarning.copy(alpha = 0.10f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                            .clickable(enabled = sedangGagal) { onRetryPending(pending) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row {
                                    Text(
                                        pending.tipe,
                                        color = if (pending.tipe == TipeOperasional.MASUK) GreenSuccess else RedAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(pending.kategori, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(RupiahFormatter.format(pending.jumlah), color = NavyDark, fontWeight = FontWeight.Bold)
                            }
                            if (sedangGagal) {
                                Text("Gagal, ketuk untuk coba lagi", color = RedAccent, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = OrangeWarning)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Mengirim...", color = OrangeWarning, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                items(laporanHariIni) { entry ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onEditLaporan(entry) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row {
                                    Text(
                                        entry.tipe,
                                        color = if (entry.tipe == TipeOperasional.MASUK) GreenSuccess else RedAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(entry.kategori, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(RupiahFormatter.format(entry.jumlah), color = NavyDark, fontWeight = FontWeight.Bold)
                                if (entry.keterangan.isNotEmpty()) {
                                    Text(entry.keterangan, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                }
                            }
                            Text("Edit ›", color = BlueGray, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Form Laporan Operasional -- dipakai untuk BUAT BARU maupun EDIT. SEMUA nilai form
 * (tipe/kategori/jumlah/keterangan) di-CONTROL dari luar (lifted state di MainActivity)
 * supaya TIDAK HILANG saat layar kamera dibuka lalu ditutup -- ini yang jadi penyebab
 * bug sebelumnya (state lokal ke-reset karena composable ini sempat "dilepas" dari
 * komposisi saat kamera full-screen ditampilkan).
 *
 * Foto bukti HANYA WAJIB untuk Tipe "Keluar" -- untuk "Masuk" kolom foto disembunyikan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperasionalFormScreen(
    isEdit: Boolean,
    tipe: String,
    onTipeChange: (String) -> Unit,
    kategori: String,
    onKategoriChange: (String) -> Unit,
    jumlahField: TextFieldValue,
    onJumlahFieldChange: (TextFieldValue) -> Unit,
    keterangan: String,
    onKeteranganChange: (String) -> Unit,
    fotoBitmap: Bitmap?,
    punyaFotoLama: Boolean,
    onAmbilFotoKamera: () -> Unit,
    onPilihDariGaleri: () -> Unit,
    onSubmit: () -> Unit,
    onHapus: (() -> Unit)?,
    onBack: () -> Unit
) {
    var expandedKategori by remember { mutableStateOf(false) }
    var showKonfirmasiHapus by remember { mutableStateOf(false) }

    val jumlahAngka = jumlahField.text.replace(".", "").toDoubleOrNull() ?: 0.0
    // Foto SELALU opsional untuk kedua Tipe -- tidak pernah menghalangi pengiriman laporan.
    val fotoWajibTerpenuhi = true

    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
            if (isEdit && onHapus != null) {
                TextButton(onClick = { showKonfirmasiHapus = true }) { Text("Hapus", color = RedAccent) }
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Text(
                if (isEdit) "Edit Laporan" else "Laporan Baru",
                style = MaterialTheme.typography.headlineLarge,
                color = NavyDark
            )
            Spacer(Modifier.height(20.dp))

            // Tipe: Masuk / Keluar (segmented)
            Text("Tipe", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardWhite)
                    .padding(4.dp)
            ) {
                listOf(TipeOperasional.MASUK, TipeOperasional.KELUAR).forEach { opsi ->
                    val dipilih = tipe == opsi
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (dipilih) NavyDark else CardWhite)
                            .clickable { onTipeChange(opsi) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            opsi,
                            color = if (dipilih) CardWhite else TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Kategori HANYA untuk Tipe "Keluar" -- Tipe "Masuk" tidak perlu Kategori
            // sama sekali (uang masuk tidak perlu dikelompokkan). Pakai
            // ExposedDropdownMenuBox resmi Compose supaya benar-benar bisa ditekan
            // (sebelumnya pakai Box+clickable manual yang klik-nya "tertelan" oleh
            // OutlinedTextField).
            if (tipe == TipeOperasional.KELUAR) {
                Spacer(Modifier.height(18.dp))
                Text("Kategori", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedKategori,
                    onExpandedChange = { expandedKategori = it }
                ) {
                    OutlinedTextField(
                        value = kategori,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKategori) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedKategori, onDismissRequest = { expandedKategori = false }) {
                        KategoriOperasional.DAFTAR.forEach { opsi ->
                            DropdownMenuItem(
                                text = { Text(opsi) },
                                onClick = { onKategoriChange(opsi); expandedKategori = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Jumlah (live format Rupiah)
            Text("Jumlah", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = jumlahField,
                onValueChange = { new ->
                    val digitsOnly = new.text.filter { it.isDigit() }
                    val formatted = RupiahFormatter.formatInputDigits(digitsOnly)
                    onJumlahFieldChange(TextFieldValue(formatted, selection = androidx.compose.ui.text.TextRange(formatted.length)))
                },
                leadingIcon = { Text("Rp", color = TextSecondary) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            // Keterangan
            Text("Keterangan", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = keterangan,
                onValueChange = onKeteranganChange,
                shape = RoundedCornerShape(14.dp),
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            // Foto bukti -- SELALU tersedia untuk kedua Tipe, tapi HANYA WAJIB untuk
            // Tipe "Keluar". Untuk "Masuk" sifatnya opsional (boleh dilampirkan kalau
            // mau, tidak menghalangi kirim laporan kalau tidak diisi).
            Text(
                "Foto Bukti (Opsional)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            if (fotoBitmap != null) {
                Image(
                    bitmap = fotoBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(8.dp))
            } else if (isEdit && punyaFotoLama) {
                Text(
                    "Foto lama tetap dipakai kalau tidak diambil ulang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAmbilFotoKamera,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(if (fotoBitmap != null || (isEdit && punyaFotoLama)) "Ambil Ulang" else "Kamera")
                }
                OutlinedButton(
                    onClick = onPilihDariGaleri,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Galeri")
                }
            }
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onSubmit,
                enabled = jumlahAngka > 0.0 && keterangan.isNotBlank() && fotoWajibTerpenuhi,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (isEdit) "Simpan Perubahan" else "Kirim Laporan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showKonfirmasiHapus && onHapus != null) {
        AlertDialog(
            onDismissRequest = { showKonfirmasiHapus = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Hapus Laporan?", color = RedAccent) },
            text = { Text("Laporan ini akan dihapus permanen dan tidak bisa dibatalkan.", color = NavyDark) },
            confirmButton = {
                TextButton(onClick = { showKonfirmasiHapus = false; onHapus() }) {
                    Text("Hapus", color = RedAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showKonfirmasiHapus = false }) { Text("Batal") } }
        )
    }
}

/**
 * Peringatan sebelum kirim/edit kalau transaksi ini akan membuat Saldo orang tsb
 * menjadi minus -- pengguna tetap boleh melanjutkan kalau memang disengaja.
 */
@Composable
fun SaldoMinusWarningDialog(
    perkiraanSaldo: Double,
    onLanjutkan: () -> Unit,
    onBatal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onBatal,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Saldo Akan Minus", color = RedAccent, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                "Setelah laporan ini, saldo Anda menjadi ${RupiahFormatter.format(perkiraanSaldo)}. Tetap lanjutkan?",
                color = NavyDark
            )
        },
        confirmButton = { TextButton(onClick = onLanjutkan) { Text("Lanjutkan", color = RedAccent, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } }
    )
}

/**
 * Popup hasil submit/edit/hapus -- HANYA muncul kalau pengguna masih berada di menu
 * Laporan Operasional saat prosesnya selesai (lihat logika di MainActivity). Kalau
 * sudah pindah layar, status cukup berubah di daftar (lihat OperasionalMenuScreen).
 */
@Composable
fun OperasionalResultDialog(
    isSukses: Boolean,
    tersimpanOffline: Boolean,
    pesan: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                when {
                    tersimpanOffline -> "Tersimpan Offline"
                    isSukses -> "Berhasil"
                    else -> "Gagal"
                },
                color = when {
                    tersimpanOffline -> OrangeWarning
                    isSukses -> GreenSuccess
                    else -> RedAccent
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = { Text(pesan, color = NavyDark) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

/**
 * Riwayat SELURUH Laporan Operasional milik SATU orang (bukan cuma hari ini) --
 * read-only, disinkron lewat Sinkronisasi Data (tetap offline). Foto TIDAK ikut
 * disinkron (cuma link), jadi ketuk link untuk buka foto (butuh internet saat itu).
 */
@Composable
fun RiwayatOperasionalScreen(
    daftar: List<RiwayatOperasionalEntry>,
    onBukaFoto: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Riwayat Laporan", style = MaterialTheme.typography.headlineLarge, color = NavyDark)
            Spacer(Modifier.height(4.dp))
            Text(
                "Seluruh laporan milik Anda, dari yang terbaru. Foto tidak otomatis diunduh -- ketuk \"Lihat Foto\" untuk membukanya.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
        }

        if (daftar.isEmpty()) {
            Text(
                "Belum ada riwayat, atau lakukan Sinkronisasi Data dulu.",
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(daftar) { r ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    Text(
                                        r.tipe,
                                        color = if (r.tipe == TipeOperasional.MASUK) GreenSuccess else RedAccent,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    if (r.kategori.isNotEmpty()) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(r.kategori, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text(r.tanggal, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(RupiahFormatter.format(r.jumlah), color = NavyDark, fontWeight = FontWeight.Bold)
                            if (r.keterangan.isNotEmpty()) {
                                Text(r.keterangan, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Saldo setelah ini: ${RupiahFormatter.format(r.saldo)}",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (r.urlBukti.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Lihat Foto ›",
                                    color = BlueGray,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.clickable { onBukaFoto(r.urlBukti) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

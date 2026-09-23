// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/components/RincianKehadiranDialog.kt
package com.sirlasirliputeri.absenssp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirlasirliputeri.absenssp.model.AbsensiHariIniEntry
import com.sirlasirliputeri.absenssp.model.AppConstants
import com.sirlasirliputeri.absenssp.ui.theme.BlueGray
import com.sirlasirliputeri.absenssp.ui.theme.NavyDark
import com.sirlasirliputeri.absenssp.ui.theme.TextSecondary

/**
 * Rincian "Total Karyawan Masuk Hari Ini" dengan 2 tingkat:
 *  1) Jumlah karyawan masuk per proyek (mis. Pulling Cable: 3, Offshore: 2).
 *  2) Ketuk salah satu proyek -> tampil daftar nama karyawan pada proyek tersebut.
 * Seluruh navigasi antar tingkat dikelola lokal di komponen ini (tidak perlu state
 * tambahan di MainActivity), datanya diambil dari cache absensi hari ini yang sudah ada.
 */
@Composable
fun RincianKehadiranDialog(
    absensiHariIni: List<AbsensiHariIniEntry>,
    onDismiss: () -> Unit
) {
    var proyekTerpilih by remember { mutableStateOf<String?>(null) }

    val masukHariIni = remember(absensiHariIni) {
        absensiHariIni.filter { it.jenis == AppConstants.JENIS_BERANGKAT }.distinctBy { it.uid }
    }

    val perProyek = remember(masukHariIni) {
        masukHariIni
            .groupBy { it.proyek.ifBlank { "Tanpa Proyek" } }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            if (proyekTerpilih == null) {
                Text("Rincian Kehadiran Hari Ini", color = NavyDark, style = MaterialTheme.typography.titleLarge)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Kembali",
                        tint = BlueGray,
                        modifier = Modifier
                            .clickable { proyekTerpilih = null }
                            .padding(end = 4.dp)
                    )
                    Text(proyekTerpilih ?: "", color = NavyDark, style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        text = {
            if (proyekTerpilih == null) {
                if (perProyek.isEmpty()) {
                    Text(
                        "Belum ada karyawan yang absen masuk hari ini.",
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(perProyek) { (proyek, jumlah) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { proyekTerpilih = proyek }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(proyek, color = NavyDark, fontWeight = FontWeight.Medium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$jumlah Orang", color = TextSecondary)
                                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            } else {
                val namaList = masukHariIni
                    .filter { it.proyek.ifBlank { "Tanpa Proyek" } == proyekTerpilih }
                    .map { it.nama }

                if (namaList.isEmpty()) {
                    Text("Tidak ada data.", color = TextSecondary)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(namaList) { nama ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(BlueGray.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        nama.take(1).uppercase(),
                                        color = BlueGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(nama, color = NavyDark)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

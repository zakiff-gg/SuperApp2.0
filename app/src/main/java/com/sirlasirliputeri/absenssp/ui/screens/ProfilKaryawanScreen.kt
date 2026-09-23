// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/screens/ProfilKaryawanScreen.kt
package com.sirlasirliputeri.absenssp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirlasirliputeri.absenssp.model.ProfilKaryawan
import com.sirlasirliputeri.absenssp.ui.components.SspCard
import com.sirlasirliputeri.absenssp.ui.components.SspPrimaryButton
import com.sirlasirliputeri.absenssp.ui.components.SspTopBar
import com.sirlasirliputeri.absenssp.ui.theme.*
import com.sirlasirliputeri.absenssp.util.RupiahFormatter

/**
 * Menampilkan profil karyawan (Nama, Jumlah Hari Masuk, Upah Harian, Upah Lembur,
 * Jumlah Jam Lembur, Bon Diterima) beserta tombol untuk membuka Pendaftaran Bon.
 * Identitas dipilih sekali dari daftar nama (bukan tap kartu), bisa diganti lewat
 * tautan "Ganti" di pojok atas.
 */
@Composable
fun ProfilKaryawanScreen(
    profil: ProfilKaryawan,
    onAjukanBon: () -> Unit,
    onGantiIdentitas: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        SspTopBar(
            title = profil.nama,
            subtitle = "Profil Karyawan",
            onBack = onBack,
            actions = {
                TextButton(onClick = onGantiIdentitas) { Text("Ganti", color = TextSecondary, fontSize = 12.sp) }
            }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            SspCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ProfilRow("Jumlah Hari Masuk", "${formatHari(profil.jumlahHariMasuk)} Hari")
                    ProfilDivider()
                    ProfilRow("Upah Harian", RupiahFormatter.format(profil.upahHarian))
                    ProfilDivider()
                    ProfilRow("Upah Lembur", RupiahFormatter.format(profil.upahLembur) + "/jam")
                    ProfilDivider()
                    ProfilRow("Jumlah Jam Lembur", "${profil.jumlahJamLembur} Jam")
                    ProfilDivider()
                    ProfilRow("Bon Diterima", RupiahFormatter.format(profil.bonDiterima))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Absen Berangkat = 0,5 hari, Absen Pulang = 0,5 hari.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(Modifier.height(20.dp))

            SspPrimaryButton(
                text = "Pendaftaran Bon",
                enabled = !profil.adaPengajuanMenunggu,
                onClick = onAjukanBon
            )
            if (profil.adaPengajuanMenunggu) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Masih ada pengajuan bon yang menunggu pencairan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Format angka hari supaya rapi: "12 Hari" kalau bulat, "12,5 Hari" kalau ada pecahan. */
private fun formatHari(nilai: Double): String {
    return if (nilai % 1.0 == 0.0) nilai.toInt().toString() else nilai.toString().replace(".", ",")
}

@Composable
private fun ProfilRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.labelLarge, color = NavyDark, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfilDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(1.dp)
            .background(DividerGray)
    )
}

/**
 * Submenu Pendaftaran Bon -- menampilkan batas maksimal dalam format Rupiah
 * ("Maksimal Bon Rp12.500.000"), input jumlah diformat OTOMATIS jadi "##.###.###"
 * saat diketik supaya tidak bingung menghitung nol. Status pengajuan SELALU
 * "Menunggu Pencairan" (bukan langsung cair).
 */
@Composable
fun PendaftaranBonDialog(
    maksimalBon: Double,
    isSubmitting: Boolean,
    onAjukan: (jumlah: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var inputField by remember { mutableStateOf(TextFieldValue("")) }
    val jumlahAngka = inputField.text.replace(".", "").toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Pendaftaran Bon", color = NavyDark, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "Maksimal Bon ${RupiahFormatter.format(maksimalBon)}",
                    color = NavyDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pengajuan akan tercatat dengan status \"Menunggu Pencairan\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputField,
                    onValueChange = { new ->
                        val digitsOnly = new.text.filter { it.isDigit() }
                        val formatted = com.sirlasirliputeri.absenssp.util.RupiahFormatter.formatInputDigits(digitsOnly)
                        inputField = TextFieldValue(formatted, selection = androidx.compose.ui.text.TextRange(formatted.length))
                    },
                    label = { Text("Jumlah Pengajuan (Rp)") },
                    leadingIcon = { Text("Rp", color = TextSecondary) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (jumlahAngka > maksimalBon) {
                    Spacer(Modifier.height(6.dp))
                    Text("Melebihi batas maksimal bon.", color = RedAccent, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAjukan(jumlahAngka) },
                enabled = !isSubmitting && jumlahAngka > 0.0 && jumlahAngka <= maksimalBon
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                else Text("Ajukan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Batal") } }
    )
}

/**
 * Popup hasil pengajuan bon (berhasil / gagal) -- selalu disertai bunyi (lihat
 * pemanggilan SoundHelper di MainActivity saat dialog ini dimunculkan).
 */
@Composable
fun BonResultDialog(
    isSukses: Boolean,
    pesan: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                if (isSukses) "Pengajuan Berhasil" else "Pengajuan Gagal",
                color = if (isSukses) GreenSuccess else RedAccent,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = { Text(pesan, color = NavyDark) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

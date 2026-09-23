// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/screens/RegisterScreen.kt
package com.sirlasirliputeri.absenssp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sirlasirliputeri.absenssp.ui.components.SspCard
import com.sirlasirliputeri.absenssp.ui.components.SspPrimaryButton
import com.sirlasirliputeri.absenssp.ui.components.SspTopBar
import com.sirlasirliputeri.absenssp.ui.theme.*

/**
 * Menu "Rekam Kartu Baru" -- form modern Material 3: top bar, kartu field terkelompok,
 * tombol aksi penuh lebar yang menonjol.
 */
@Composable
fun RegisterScreen(
    uidTerbaca: String,
    isSubmitting: Boolean,
    onSubmit: (nama: String, tempatKerja: String, posisi: String) -> Unit,
    onBack: () -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var tempatKerja by remember { mutableStateOf("") }
    var posisi by remember { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = DividerGray,
        focusedBorderColor = NavyDark
    )

    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        SspTopBar(title = "Rekam Kartu Baru", subtitle = "Tempelkan kartu untuk isi UID otomatis", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SspCard {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = NavyDark)
                        Spacer(Modifier.width(8.dp))
                        Text("Data Karyawan Baru", style = MaterialTheme.typography.titleMedium, color = NavyDark)
                    }
                    OutlinedTextField(
                        value = uidTerbaca,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("UID Kartu") },
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nama, onValueChange = { nama = it },
                        label = { Text("Nama Karyawan") },
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempatKerja, onValueChange = { tempatKerja = it },
                        label = { Text("Tempat Kerja") },
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = posisi, onValueChange = { posisi = it },
                        label = { Text("Posisi / Jabatan") },
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            SspPrimaryButton(
                text = if (isSubmitting) "Menyimpan…" else "Simpan Data Karyawan",
                enabled = !isSubmitting && uidTerbaca.isNotEmpty() && nama.isNotBlank(),
                onClick = { onSubmit(nama, tempatKerja, posisi) }
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "Jika UID sudah terdaftar, data lama otomatis dipindahkan ke Riwayat Karyawan sebelum data baru disimpan.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Popup hasil pendaftaran kartu (berhasil / gagal) -- selalu disertai bunyi (lihat
 * pemanggilan SoundHelper di MainActivity saat dialog ini dimunculkan).
 */
@Composable
fun RegisterResultDialog(
    isSukses: Boolean,
    pesan: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                if (isSukses) "Berhasil Didaftarkan" else "Gagal Mendaftarkan",
                color = if (isSukses) GreenSuccess else RedAccent,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = { Text(pesan, color = NavyDark) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

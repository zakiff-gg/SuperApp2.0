// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/components/ProjectPickerDialog.kt
package com.sirlasirliputeri.absenssp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sirlasirliputeri.absenssp.ui.theme.NavyDark
import com.sirlasirliputeri.absenssp.ui.theme.TextSecondary

/**
 * Pop-up pilihan proyek yang muncul HANYA saat Absen Berangkat. Daftar proyek diambil
 * dari cache lokal HP (sudah instan, tidak menunggu server) -- lihat MainActivity.
 */
@Composable
fun ProjectPickerDialog(
    daftarProyek: List<String>,
    onProyekDipilih: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Pilih Proyek Hari Ini", style = MaterialTheme.typography.titleLarge, color = NavyDark) },
        text = {
            if (daftarProyek.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "Belum ada data proyek di HP ini.",
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        "Minta admin melakukan Sinkronisasi Data.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(daftarProyek) { proyek ->
                        Button(
                            onClick = { onProyekDipilih(proyek) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(proyek, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

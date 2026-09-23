// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/ui/screens/AdminDialog.kt
package com.sirlasirliputeri.absenssp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sirlasirliputeri.absenssp.ui.theme.CardWhite
import com.sirlasirliputeri.absenssp.ui.theme.NavyDark
import com.sirlasirliputeri.absenssp.ui.theme.TextSecondary

/**
 * Menu Rahasia Admin -- dipicu dengan mengetuk logo perusahaan 5x berturut-turut.
 */
@Composable
fun AdminPasswordDialog(
    onPasswordBenar: () -> Unit,
    onDismiss: () -> Unit,
    validatePassword: (String) -> Boolean
) {
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Masuk Sebagai Admin", color = NavyDark) },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = null },
                    label = { Text("Password Admin") },
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = PasswordVisualTransformation()
                )
                errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (validatePassword(password)) onPasswordBenar()
                else errorText = "Password salah"
            }) { Text("Masuk") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun AdminMenuDialog(
    isDownloading: Boolean,
    statusMessage: String?,
    onClearCache: () -> Unit,
    onDownloadPdf: (bulan: Int, tahun: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = java.util.Calendar.getInstance()
    var bulan by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH) + 1) }
    var tahun by remember { mutableStateOf(calendar.get(java.util.Calendar.YEAR)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Menu Admin", color = NavyDark) },
        text = {
            Column {
                OutlinedButton(
                    onClick = onClearCache,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Clear Cache & Reset Antrean")
                }

                Spacer(Modifier.height(20.dp))
                Text("Periode Rekap", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (bulan > 1) bulan-- else { bulan = 12; tahun-- } }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Bulan sebelumnya", tint = NavyDark)
                    }
                    Text("$bulan / $tahun", color = NavyDark, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (bulan < 12) bulan++ else { bulan = 1; tahun++ } }) {
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Bulan berikutnya", tint = NavyDark)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onDownloadPdf(bulan, tahun) },
                    enabled = !isDownloading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isDownloading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CardWhite)
                    else Text("Download PDF Rekap Bulanan")
                }
                statusMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = NavyDark, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}

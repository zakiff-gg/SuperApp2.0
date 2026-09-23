// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/InvoiceDetailScreen.kt
package com.sirlasirliputeri.absenssp.invoice

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sirlasirliputeri.absenssp.ui.theme.*
import com.sirlasirliputeri.absenssp.util.RupiahFormatter

@Composable
fun InvoiceDetailScreen(
    invoice: Invoice?,
    pembayaranTertunda: Double,
    riwayatPembayaran: List<Payment>,
    isLoading: Boolean,
    isSubmittingPayment: Boolean,
    tanggalHariIni: String,
    onBack: () -> Unit,
    onLihatPdf: () -> Unit,
    onPilihTanggal: (tanggalSaatIni: String, onDipilih: (String) -> Unit) -> Unit,
    onTambahPembayaran: (nominal: Double, tanggal: String, catatan: String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
            Spacer(Modifier.weight(1f))
            Text("Detail Invoice", style = MaterialTheme.typography.titleLarge, color = NavyDark)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(72.dp))
        }

        if (isLoading || invoice == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val sisaTampil = (invoice.sisaTagihan() - pembayaranTertunda).coerceAtLeast(0.0)
        val sudahLunasTampil = sisaTampil <= 0.0

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(invoice.tujuan, style = MaterialTheme.typography.headlineMedium, color = NavyDark)
                    if (invoice.nomorInvoice.isNotBlank()) {
                        Text("No. Invoice: ${invoice.nomorInvoice}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(RupiahFormatter.format(invoice.nominal), style = MaterialTheme.typography.bodyLarge, color = NavyDark, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Status: ${if (sudahLunasTampil && pembayaranTertunda > 0) "Lunas (menunggu sinkron)" else invoice.status}  ·  Sisa " + RupiahFormatter.format(sisaTampil),
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    val jatuhTempoText = when {
                        invoice.status == "Lunas" -> "Lunas"
                        invoice.selisihJatuhTempo() < 0 -> "Telat ${-invoice.selisihJatuhTempo()} hari"
                        invoice.selisihJatuhTempo() == 0L -> "Jatuh tempo hari ini"
                        else -> "Jatuh tempo ${invoice.selisihJatuhTempo()} hari lagi"
                    }
                    Text("Umur ${invoice.umurHari()} hari · $jatuhTempoText", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onLihatPdf,
                            enabled = invoice.linkPdf.isNotBlank(),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) { Text("Lihat PDF") }
                        Button(
                            onClick = { showDialog = true },
                            enabled = !sudahLunasTampil,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) { Text("Tambah Bayar") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Riwayat Pembayaran", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
        }

        if (riwayatPembayaran.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp), contentAlignment = Alignment.TopCenter) {
                Text("Belum ada pembayaran tercatat", color = TextSecondary, modifier = Modifier.padding(top = 20.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(riwayatPembayaran.sortedByDescending { it.tanggalBayar }) { p ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(RupiahFormatter.format(p.nominalDibayar), style = MaterialTheme.typography.bodyLarge, color = NavyDark, fontWeight = FontWeight.SemiBold)
                                Text(p.tanggalBayar, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                if (p.catatan.isNotBlank()) {
                                    Text(p.catatan, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TambahPembayaranDialog(
            sisaTagihan = sisaTinggiAman(invoice, pembayaranTertunda),
            tanggalHariIni = tanggalHariIni,
            isSubmitting = isSubmittingPayment,
            onDismiss = { showDialog = false },
            onPilihTanggal = onPilihTanggal,
            onSimpan = { nominal, tanggal, catatan ->
                onTambahPembayaran(nominal, tanggal, catatan)
                showDialog = false
            }
        )
    }
}

private fun sisaTinggiAman(invoice: Invoice?, pembayaranTertunda: Double): Double =
    ((invoice?.sisaTagihan() ?: 0.0) - pembayaranTertunda).coerceAtLeast(0.0)

@Composable
private fun TambahPembayaranDialog(
    sisaTagihan: Double,
    tanggalHariIni: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onPilihTanggal: (tanggalSaatIni: String, onDipilih: (String) -> Unit) -> Unit,
    onSimpan: (nominal: Double, tanggal: String, catatan: String) -> Unit
) {
    var nominalDigits by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(tanggalHariIni) }
    var catatan by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Tambah Pembayaran", style = MaterialTheme.typography.titleLarge, color = NavyDark)
                Spacer(Modifier.height(4.dp))
                Text("Sisa tagihan: " + RupiahFormatter.format(sisaTagihan), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(14.dp))

                NominalInputField(
                    digits = nominalDigits,
                    onDigitsChange = { raw ->
                        nominalDigits = clampDigitsTo(raw, sisaTagihan)
                        error = null
                    },
                    isError = error != null,
                    supportingText = error ?: "Maksimal " + RupiahFormatter.format(sisaTagihan),
                    colors = fieldColorsLocal(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                Box {
                    OutlinedTextField(
                        value = tanggal,
                        onValueChange = {},
                        readOnly = true,
                        colors = fieldColorsLocal(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier.matchParentSize().clickable(
                            indication = null, interactionSource = remember { MutableInteractionSource() }
                        ) { onPilihTanggal(tanggal) { tanggal = it } }
                    )
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    placeholder = { Text("Catatan (opsional)") },
                    colors = fieldColorsLocal(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, enabled = !isSubmitting, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val nominal = nominalDigits.toDoubleOrNull()
                            when {
                                nominal == null || nominal <= 0.0 -> error = "Nominal tidak valid"
                                nominal > sisaTagihan -> error = "Melebihi sisa tagihan"
                                else -> onSimpan(nominal, tanggal, catatan.trim())
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CreamBg)
                        else Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColorsLocal() = TextFieldDefaults.colors(
    unfocusedContainerColor = CreamBg,
    focusedContainerColor = CreamBg,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent
)

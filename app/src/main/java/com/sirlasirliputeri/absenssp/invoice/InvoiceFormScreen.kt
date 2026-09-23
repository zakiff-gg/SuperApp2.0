// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/InvoiceFormScreen.kt
package com.sirlasirliputeri.absenssp.invoice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sirlasirliputeri.absenssp.ui.theme.*

data class InvoiceFormData(
    val nomorInvoice: String,
    val tujuan: String,
    val nominal: Double,
    val tanggalInvoice: String,
    val jatuhTempo: String,
    val keterangan: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormScreen(
    daftarVendor: List<String>,
    daftarNomorInvoiceTerpakai: List<String>,
    tanggalHariIni: String,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onPilihTanggal: (tanggalSaatIni: String, onDipilih: (String) -> Unit) -> Unit,
    onLanjutScan: (InvoiceFormData) -> Unit
) {
    var tujuan by remember { mutableStateOf("") }
    var vendorMenuExpanded by remember { mutableStateOf(false) }
    var nomorInvoice by remember { mutableStateOf("") }
    var nominalDigits by remember { mutableStateOf("") }
    var tanggalInvoice by remember { mutableStateOf(tanggalHariIni) }
    var jatuhTempo by remember { mutableStateOf("") }
    var keterangan by remember { mutableStateOf("") }

    var errorNomorInvoice by remember { mutableStateOf<String?>(null) }
    var errorTujuan by remember { mutableStateOf<String?>(null) }
    var errorNominal by remember { mutableStateOf<String?>(null) }
    var errorJatuhTempo by remember { mutableStateOf<String?>(null) }

    var showKonfirmasiKeluar by remember { mutableStateOf(false) }
    var showPeringatanDuplikat by remember { mutableStateOf(false) }
    var dataSiapDikirim by remember { mutableStateOf<InvoiceFormData?>(null) }

    val isDirty = nomorInvoice.isNotBlank() || tujuan.isNotBlank() || nominalDigits.isNotBlank() ||
        jatuhTempo.isNotBlank() || keterangan.isNotBlank()

    fun mintaKeluar() {
        if (isDirty && !isSubmitting) showKonfirmasiKeluar = true else onBack()
    }

    // Tombol back sistem (gesture/tombol fisik) ikut ditangkap, bukan cuma tombol "Batal" di layar.
    BackHandler(enabled = isDirty && !isSubmitting) { showKonfirmasiKeluar = true }

    val vendorTersaring = remember(tujuan, daftarVendor) {
        if (tujuan.isBlank()) daftarVendor else daftarVendor.filter { it.contains(tujuan, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { mintaKeluar() }, enabled = !isSubmitting) { Text("‹ Batal", color = BlueGray) }
            Spacer(Modifier.weight(1f))
            Text("Invoice Baru", style = MaterialTheme.typography.titleLarge, color = NavyDark)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(56.dp)) // seimbangkan judul di tengah
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            FieldLabel("Nomor Invoice")
            OutlinedTextField(
                value = nomorInvoice,
                onValueChange = { nomorInvoice = it; errorNomorInvoice = null },
                placeholder = { Text("Contoh: INV/2026/09/001") },
                isError = errorNomorInvoice != null,
                supportingText = errorNomorInvoice?.let { { Text(it, color = RedAccent) } },
                singleLine = true,
                colors = fieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Ditujukan Kepada (Vendor)")
            ExposedDropdownMenuBox(
                expanded = vendorMenuExpanded && vendorTersaring.isNotEmpty(),
                onExpandedChange = { vendorMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = tujuan,
                    onValueChange = {
                        tujuan = it
                        errorTujuan = null
                        vendorMenuExpanded = true
                    },
                    placeholder = { Text("Pilih atau ketik nama vendor baru") },
                    isError = errorTujuan != null,
                    supportingText = errorTujuan?.let { { Text(it, color = RedAccent) } },
                    singleLine = true,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = vendorMenuExpanded && vendorTersaring.isNotEmpty(),
                    onDismissRequest = { vendorMenuExpanded = false }
                ) {
                    vendorTersaring.forEach { nama ->
                        DropdownMenuItem(
                            text = { Text(nama) },
                            onClick = { tujuan = nama; vendorMenuExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            FieldLabel("Nominal Invoice")
            NominalInputField(
                digits = nominalDigits,
                onDigitsChange = { nominalDigits = it; errorNominal = null },
                isError = errorNominal != null,
                supportingText = errorNominal,
                colors = fieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Tanggal Invoice")
            DateField(value = tanggalInvoice, onClick = {
                onPilihTanggal(tanggalInvoice) { tanggalInvoice = it }
            })

            Spacer(Modifier.height(16.dp))

            FieldLabel("Jatuh Tempo")
            DateField(value = jatuhTempo, placeholder = "Pilih tanggal jatuh tempo", isError = errorJatuhTempo != null, onClick = {
                onPilihTanggal(jatuhTempo.ifBlank { tanggalHariIni }) { jatuhTempo = it; errorJatuhTempo = null }
            })
            errorJatuhTempo?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = RedAccent, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))

            FieldLabel("Keterangan (opsional)")
            OutlinedTextField(
                value = keterangan,
                onValueChange = { keterangan = it },
                placeholder = { Text("Catatan tambahan") },
                minLines = 2,
                colors = fieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }

        // Tombol lanjut, fixed di bawah
        Column(modifier = Modifier.fillMaxWidth().background(CreamBg).padding(20.dp)) {
            Button(
                onClick = {
                    var valid = true
                    if (nomorInvoice.isBlank()) { errorNomorInvoice = "Wajib diisi"; valid = false }
                    if (tujuan.isBlank()) { errorTujuan = "Wajib diisi"; valid = false }
                    val nominal = nominalDigits.toDoubleOrNull() ?: 0.0
                    if (nominalDigits.isBlank() || nominal <= 0.0) { errorNominal = "Nominal tidak valid"; valid = false }
                    if (jatuhTempo.isBlank()) { errorJatuhTempo = "Pilih tanggal jatuh tempo dulu"; valid = false }
                    if (valid) {
                        val data = InvoiceFormData(nomorInvoice.trim(), tujuan.trim(), nominal, tanggalInvoice, jatuhTempo, keterangan.trim())
                        if (daftarNomorInvoiceTerpakai.any { it.equals(data.nomorInvoice, ignoreCase = true) }) {
                            dataSiapDikirim = data
                            showPeringatanDuplikat = true
                        } else {
                            onLanjutScan(data)
                        }
                    }
                },
                enabled = !isSubmitting,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CreamBg)
                } else {
                    Text("Lanjut Pindai Berkas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showKonfirmasiKeluar) {
        KonfirmasiDialog(
            judul = "Buang Invoice Ini?",
            pesan = "Data yang sudah diisi belum disimpan dan akan hilang kalau kamu keluar sekarang.",
            labelKonfirmasi = "Ya, Keluar",
            warnaKonfirmasi = RedAccent,
            onKonfirmasi = { showKonfirmasiKeluar = false; onBack() },
            onBatal = { showKonfirmasiKeluar = false }
        )
    }

    if (showPeringatanDuplikat) {
        KonfirmasiDialog(
            judul = "Nomor Invoice Sudah Pernah Dipakai",
            pesan = "Nomor \"${dataSiapDikirim?.nomorInvoice.orEmpty()}\" sudah tercatat sebelumnya. Lanjutkan simpan invoice ini juga?",
            labelKonfirmasi = "Lanjutkan",
            warnaKonfirmasi = OrangeWarning,
            onKonfirmasi = {
                showPeringatanDuplikat = false
                dataSiapDikirim?.let { onLanjutScan(it) }
            },
            onBatal = { showPeringatanDuplikat = false }
        )
    }
}

@Composable
private fun KonfirmasiDialog(
    judul: String,
    pesan: String,
    labelKonfirmasi: String,
    warnaKonfirmasi: Color,
    onKonfirmasi: () -> Unit,
    onBatal: () -> Unit
) {
    Dialog(onDismissRequest = onBatal) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(judul, style = MaterialTheme.typography.titleLarge, color = NavyDark, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(pesan, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBatal, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = onKonfirmasi,
                        colors = ButtonDefaults.buttonColors(containerColor = warnaKonfirmasi),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(labelKonfirmasi)
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun DateField(value: String, placeholder: String = "Pilih tanggal", isError: Boolean = false, onClick: () -> Unit) {
    // OutlinedTextField dibuat readOnly (bukan disabled) supaya tampil normal (tidak pudar),
    // lalu ditumpuk Box transparan yang menangkap klik untuk membuka DatePickerDialog.
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            isError = isError,
            colors = fieldColors(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
        )
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    unfocusedContainerColor = CardWhite,
    focusedContainerColor = CardWhite,
    disabledContainerColor = CardWhite,
    unfocusedIndicatorColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)

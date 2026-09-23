// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/InvoiceListScreen.kt
package com.sirlasirliputeri.absenssp.invoice

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.sirlasirliputeri.absenssp.ui.theme.*
import com.sirlasirliputeri.absenssp.util.RupiahFormatter

private enum class FilterInvoice(val label: String) {
    SEMUA("Semua"), BELUM_BAYAR("Belum Bayar"), SEBAGIAN("Sebagian"), LUNAS("Lunas")
}

@Composable
fun InvoiceListScreen(
    items: List<InvoiceUiItem>,
    isSyncing: Boolean,
    jumlahAntrean: Int,
    waktuSinkronTerakhir: String,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onTambahBaru: () -> Unit,
    onSync: () -> Unit
) {
    var filter by remember { mutableStateOf(FilterInvoice.SEMUA) }
    var kataKunci by remember { mutableStateOf("") }

    val filtered = remember(items, filter, kataKunci) {
        val hasilStatus = when (filter) {
            FilterInvoice.SEMUA -> items
            FilterInvoice.BELUM_BAYAR -> items.filter { it.invoice.status == "Belum Bayar" && !it.isLocalDraft }
            FilterInvoice.SEBAGIAN -> items.filter { it.invoice.status == "Sebagian" || (it.pembayaranTertunda > 0 && it.sisaTampil > 0) }
            FilterInvoice.LUNAS -> items.filter { it.invoice.status == "Lunas" || (it.pembayaranTertunda > 0 && it.sisaTampil <= 0.0) }
        }
        if (kataKunci.isBlank()) hasilStatus else hasilStatus.filter {
            it.invoice.tujuan.contains(kataKunci, ignoreCase = true) || it.invoice.nomorInvoice.contains(kataKunci, ignoreCase = true)
        }
    }

    // Ringkasan: total sisa tagihan yang belum lunas (belum bayar + sebagian), dari SEMUA data
    // (bukan hasil filter/pencarian), supaya selalu jadi angka acuan yang stabil.
    val totalBelumLunas = remember(items) {
        items.filter { it.sisaTampil > 0.0 }.sumOf { it.sisaTampil }
    }
    val jumlahBelumLunas = remember(items) { items.count { it.sisaTampil > 0.0 } }

    Box(modifier = Modifier.fillMaxSize().background(CreamBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali", tint = NavyDark) }
                Spacer(Modifier.weight(1f))
                Text("Monitoring Invoice", style = MaterialTheme.typography.titleLarge, color = NavyDark)
                Spacer(Modifier.weight(1f))
                SyncIconButton(isSyncing = isSyncing, jumlahAntrean = jumlahAntrean, onClick = onSync)
            }

            if (jumlahAntrean > 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OrangeWarning.copy(alpha = 0.12f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "$jumlahAntrean data menunggu dikirim · ketuk ikon sinkron di pojok atas",
                        color = OrangeWarning, fontWeight = FontWeight.Medium, fontSize = 13.sp
                    )
                }
            } else if (waktuSinkronTerakhir.isNotBlank()) {
                Text(
                    "Terakhir sinkron: $waktuSinkronTerakhir",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Ringkasan total belum lunas -- langsung kelihatan tanpa scroll satu-satu
            if (items.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavyDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Total Belum Lunas", style = MaterialTheme.typography.labelMedium, color = CreamBg.copy(alpha = 0.7f))
                            Text(RupiahFormatter.format(totalBelumLunas), style = MaterialTheme.typography.titleLarge, color = CreamBg, fontWeight = FontWeight.Bold)
                        }
                        Text("$jumlahBelumLunas invoice", style = MaterialTheme.typography.bodySmall, color = CreamBg.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Pencarian nomor invoice / nama vendor
            OutlinedTextField(
                value = kataKunci,
                onValueChange = { kataKunci = it },
                placeholder = { Text("Cari nomor invoice atau nama vendor...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = CardWhite,
                    focusedContainerColor = CardWhite,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterInvoice.entries.forEach { f ->
                    val selected = f == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) NavyDark else CardWhite)
                            .clickable { filter = f }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            f.label,
                            color = if (selected) CreamBg else NavyDark,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                EmptyStateInvoice(adaPencarianAtauFilter = kataKunci.isNotBlank() || filter != FilterInvoice.SEMUA, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filtered, key = { it.invoice.id }) { item ->
                        InvoiceCard(item = item, onClick = { if (!item.isLocalDraft) onOpenDetail(item.invoice.id) })
                    }
                }
            }
        }

        // FAB tambah invoice baru
        FloatingActionButton(
            onClick = onTambahBaru,
            containerColor = NavyDark,
            contentColor = CreamBg,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah Invoice")
        }
    }
}

/** Empty state yang lebih ramah: pesan beda antara "belum ada data sama sekali" vs "tidak ketemu hasil filter/cari". */
@Composable
private fun EmptyStateInvoice(adaPencarianAtauFilter: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(NavyDark.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            val warnaIkon = NavyDark.copy(alpha = 0.5f)
            Canvas(modifier = Modifier.size(30.dp)) {
                val w = size.width; val h = size.height
                drawRoundRect(
                    color = warnaIkon,
                    topLeft = Offset(w * 0.14f, h * 0.08f),
                    size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.84f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f),
                    style = Stroke(width = w * 0.08f)
                )
                drawLine(color = warnaIkon, start = Offset(w * 0.30f, h * 0.40f), end = Offset(w * 0.70f, h * 0.40f), strokeWidth = w * 0.07f)
                drawLine(color = warnaIkon, start = Offset(w * 0.30f, h * 0.60f), end = Offset(w * 0.70f, h * 0.60f), strokeWidth = w * 0.07f)
            }
        }
        Spacer(Modifier.height(14.dp))
        if (adaPencarianAtauFilter) {
            Text("Tidak ada invoice yang cocok", style = MaterialTheme.typography.titleLarge, color = NavyDark, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("Coba kata kunci lain atau ganti filter status.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        } else {
            Text("Belum Ada Invoice", style = MaterialTheme.typography.titleLarge, color = NavyDark, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("Ketuk tombol + di pojok bawah untuk membuat invoice pertama.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InvoiceCard(item: InvoiceUiItem, onClick: () -> Unit) {
    val inv = item.invoice
    val warnaStatus = when {
        item.isLocalDraft -> OrangeWarning
        inv.status == "Lunas" && item.pembayaranTertunda <= 0.0 -> GreenSuccess
        inv.isTerlambat() -> RedAccent
        inv.status == "Sebagian" -> OrangeWarning
        else -> BlueGray
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !item.isLocalDraft) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(inv.tujuan.ifBlank { "(Tanpa nama vendor)" }, style = MaterialTheme.typography.titleLarge, color = NavyDark)
                    if (inv.nomorInvoice.isNotBlank()) {
                        Text("No. ${inv.nomorInvoice}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(warnaStatus.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(item.statusTampil, color = warnaStatus, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(RupiahFormatter.format(inv.nominal), style = MaterialTheme.typography.bodyLarge, color = NavyDark, fontWeight = FontWeight.SemiBold)
            if (!item.isLocalDraft) {
                Spacer(Modifier.height(2.dp))
                Text("Sisa " + RupiahFormatter.format(item.sisaTampil), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                val jatuhTempoText = when {
                    inv.status == "Lunas" && item.pembayaranTertunda <= 0.0 -> "Lunas"
                    inv.selisihJatuhTempo() < 0 -> "Telat ${-inv.selisihJatuhTempo()} hari"
                    inv.selisihJatuhTempo() == 0L -> "Jatuh tempo hari ini"
                    else -> "Jatuh tempo ${inv.selisihJatuhTempo()} hari lagi"
                }
                Text("Umur ${inv.umurHari()} hari · $jatuhTempoText", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                Spacer(Modifier.height(4.dp))
                Text("Berkas tersimpan di HP, akan terkirim saat Sinkron", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun SyncIconButton(isSyncing: Boolean, jumlahAntrean: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (jumlahAntrean > 0) OrangeWarning.copy(alpha = 0.14f) else NavyDark.copy(alpha = 0.08f))
            .clickable(enabled = !isSyncing) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSyncing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            val warna = if (jumlahAntrean > 0) OrangeWarning else NavyDark
            Canvas(modifier = Modifier.size(18.dp)) {
                val w = size.width; val h = size.height
                val stroke = Stroke(width = w * 0.11f)
                drawArc(color = warna, startAngle = -160f, sweepAngle = 250f, useCenter = false,
                    topLeft = Offset(w * 0.10f, h * 0.10f), size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.80f), style = stroke)
                drawArc(color = warna, startAngle = 20f, sweepAngle = 250f, useCenter = false,
                    topLeft = Offset(w * 0.10f, h * 0.10f), size = androidx.compose.ui.geometry.Size(w * 0.80f, h * 0.80f), style = stroke)
            }
        }
    }
}

// Helper kecil supaya penulisan ukuran sp konsisten tanpa import berulang di tiap pemanggilan

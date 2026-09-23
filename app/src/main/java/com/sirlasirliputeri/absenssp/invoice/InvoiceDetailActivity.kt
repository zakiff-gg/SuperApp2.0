// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/InvoiceDetailActivity.kt
package com.sirlasirliputeri.absenssp.invoice

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sirlasirliputeri.absenssp.ui.theme.AbsenSSPTheme
import com.sirlasirliputeri.absenssp.ui.theme.resolveDarkTheme
import com.sirlasirliputeri.absenssp.util.PrefsHelper
import java.util.Calendar

class InvoiceDetailActivity : ComponentActivity() {

    private lateinit var repo: InvoiceRepository
    private lateinit var idInvoice: String
    private val isSubmittingPayment = mutableStateOf(false)
    private val resumeTrigger = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = InvoiceRepository(applicationContext)

        idInvoice = intent.getStringExtra("idInvoice") ?: run {
            finish(); return
        }

        setContent {
            val darkPref = remember { PrefsHelper.getDarkModePreference(applicationContext) }
            AbsenSSPTheme(darkTheme = resolveDarkTheme(darkPref)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val trigger by resumeTrigger
                    var invoice by remember { mutableStateOf<Invoice?>(null) }
                    var pembayaran by remember { mutableStateOf<List<Payment>>(emptyList()) }
                    var pembayaranTertunda by remember { mutableStateOf(0.0) }
                    val submitting by isSubmittingPayment

                    LaunchedEffect(trigger) {
                        val (inv, draftPembayaran) = repo.detailInvoice(idInvoice)
                        invoice = inv
                        pembayaranTertunda = draftPembayaran.sumOf { it.nominalDibayar }
                        // riwayat pembayaran server disimpan bersama cache invoice? Tidak -- server
                        // tidak menyimpan detail riwayat di cache list, jadi gabungkan draft lokal saja
                        // di sini; riwayat lengkap dari server didapat lewat detailInvoice() saat online.
                        pembayaran = draftPembayaran
                        if (inv == null) {
                            // Belum ada di cache (mis. baru buka app) -- coba ambil dari server kalau online
                            InvoiceApiService.detailInvoice(idInvoice, object : InvoiceApiService.Callback<Pair<Invoice, List<Payment>>> {
                                override fun onSuccess(result: Pair<Invoice, List<Payment>>) {
                                    invoice = result.first
                                    pembayaran = result.second + draftPembayaran
                                }
                                override fun onError(message: String) {
                                    Toast.makeText(this@InvoiceDetailActivity, message, Toast.LENGTH_LONG).show()
                                }
                            })
                        } else {
                            // Ada di cache -- tetap coba refresh riwayat pembayaran lengkap dari server kalau online,
                            // tapi TIDAK memblokir tampilan (sudah tampil duluan dari cache di atas)
                            InvoiceApiService.detailInvoice(idInvoice, object : InvoiceApiService.Callback<Pair<Invoice, List<Payment>>> {
                                override fun onSuccess(result: Pair<Invoice, List<Payment>>) {
                                    invoice = result.first
                                    pembayaran = result.second + draftPembayaran
                                }
                                override fun onError(message: String) { /* diamkan, tetap pakai data cache */ }
                            })
                        }
                    }

                    InvoiceDetailScreen(
                        invoice = invoice,
                        pembayaranTertunda = pembayaranTertunda,
                        riwayatPembayaran = pembayaran,
                        isLoading = false,
                        isSubmittingPayment = submitting,
                        tanggalHariIni = DateUtil.today(),
                        onBack = { finish() },
                        onLihatPdf = {
                            val link = invoice?.linkPdf.orEmpty()
                            if (link.isNotBlank()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        },
                        onPilihTanggal = { tanggalSaatIni, onDipilih -> pilihTanggal(tanggalSaatIni, onDipilih) },
                        onTambahPembayaran = { nominal, tanggal, catatan ->
                            isSubmittingPayment.value = true
                            repo.tambahPembayaran(idInvoice, tanggal, nominal, catatan) { result ->
                                isSubmittingPayment.value = false
                                val pesan = when (result) {
                                    is SubmitResult.Terkirim -> "Pembayaran tersimpan"
                                    is SubmitResult.Diantrekan -> result.alasan
                                    is SubmitResult.Gagal -> result.pesan
                                }
                                Toast.makeText(this@InvoiceDetailActivity, pesan, Toast.LENGTH_LONG).show()
                                resumeTrigger.value += 1
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTrigger.value += 1
    }

    private fun pilihTanggal(tanggalSaatIni: String, onDipilih: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day -> onDipilih(String.format("%04d-%02d-%02d", year, month + 1, day)) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}

// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/InvoiceFormActivity.kt
package com.sirlasirliputeri.absenssp.invoice

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sirlasirliputeri.absenssp.ui.theme.AbsenSSPTheme
import com.sirlasirliputeri.absenssp.ui.theme.resolveDarkTheme
import com.sirlasirliputeri.absenssp.util.PrefsHelper
import java.io.ByteArrayOutputStream
import java.util.Calendar

/**
 * Layar pertama alur Invoice: isi form dulu (tujuan/vendor, nominal, tanggal invoice,
 * jatuh tempo, keterangan), baru lanjut ke pemindaian berkas. Form-nya sekarang Compose,
 * vendor jadi dropdown otomatis dari riwayat, nominal live-format ##.###.###.
 */
class InvoiceFormActivity : ComponentActivity() {

    private lateinit var repo: InvoiceRepository
    private var pendingFormData: InvoiceFormData? = null
    private val isSubmitting = mutableStateOf(false)

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pdfUri = DocumentScanHelper.extractPdfUri(result.resultCode, result.data)
        val data = pendingFormData
        if (pdfUri != null && data != null) {
            uploadInvoice(data, pdfUri)
        } else {
            isSubmitting.value = false
            Toast.makeText(this, "Pemindaian dibatalkan atau gagal", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = InvoiceRepository(applicationContext)

        setContent {
            val darkPref = remember { PrefsHelper.getDarkModePreference(applicationContext) }
            AbsenSSPTheme(darkTheme = resolveDarkTheme(darkPref)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val submitting by isSubmitting
                    InvoiceFormScreen(
                        daftarVendor = repo.daftarVendor(),
                        daftarNomorInvoiceTerpakai = repo.daftarNomorInvoiceTerpakai(),
                        tanggalHariIni = DateUtil.today(),
                        isSubmitting = submitting,
                        onBack = { finish() },
                        onPilihTanggal = { tanggalSaatIni, onDipilih -> pilihTanggal(tanggalSaatIni, onDipilih) },
                        onLanjutScan = { data ->
                            pendingFormData = data
                            isSubmitting.value = true
                            DocumentScanHelper.start(this@InvoiceFormActivity, scanLauncher)
                        }
                    )
                }
            }
        }
    }

    private fun pilihTanggal(tanggalSaatIni: String, onDipilih: (String) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day -> onDipilih(String.format("%04d-%02d-%02d", year, month + 1, day)) },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun uploadInvoice(data: InvoiceFormData, pdfUri: android.net.Uri) {
        Thread {
            try {
                val base64Bytes = uriToBytes(pdfUri)
                val namaFile = "Invoice_${data.tujuan}_${data.tanggalInvoice}.pdf"

                runOnUiThread {
                    repo.simpanInvoiceBaru(
                        nomorInvoice = data.nomorInvoice,
                        tujuan = data.tujuan,
                        nominal = data.nominal,
                        tanggalInvoice = data.tanggalInvoice,
                        jatuhTempo = data.jatuhTempo,
                        keterangan = data.keterangan,
                        pdfBytes = base64Bytes,
                        namaFile = namaFile
                    ) { result ->
                        isSubmitting.value = false
                        when (result) {
                            is SubmitResult.Terkirim -> {
                                Toast.makeText(this, "Invoice tersimpan (ID: ${result.idInvoice})", Toast.LENGTH_LONG).show()
                                finish()
                            }
                            is SubmitResult.Diantrekan -> {
                                Toast.makeText(this, result.alasan, Toast.LENGTH_LONG).show()
                                finish()
                            }
                            is SubmitResult.Gagal -> {
                                Toast.makeText(this, result.pesan, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    isSubmitting.value = false
                    Toast.makeText(this, "Gagal membaca berkas PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun uriToBytes(uri: android.net.Uri): ByteArray {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Tidak bisa membaca berkas hasil scan")
        val buffer = ByteArrayOutputStream()
        inputStream.use { it.copyTo(buffer) }
        return buffer.toByteArray()
    }
}

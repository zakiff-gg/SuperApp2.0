// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/InvoiceMonitoringActivity.kt
package com.sirlasirliputeri.absenssp.invoice

import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Locale

class InvoiceMonitoringActivity : ComponentActivity() {

    private lateinit var repo: InvoiceRepository
    private val resumeTrigger = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = InvoiceRepository(applicationContext)

        setContent {
            val darkPref = remember { PrefsHelper.getDarkModePreference(applicationContext) }
            AbsenSSPTheme(darkTheme = resolveDarkTheme(darkPref)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var items by remember { mutableStateOf(repo.daftarInvoiceUi()) }
                    var jumlahAntrean by remember { mutableStateOf(repo.jumlahAntreanTertunda()) }
                    var isSyncing by remember { mutableStateOf(false) }
                    var waktuSinkron by remember { mutableStateOf(formatWaktu(repo.waktuSinkronTerakhir())) }
                    val trigger by resumeTrigger

                    // Muat ulang dari CACHE LOKAL setiap kembali ke layar ini (instan, TIDAK kontak server)
                    LaunchedEffect(trigger) {
                        items = repo.daftarInvoiceUi()
                        jumlahAntrean = repo.jumlahAntreanTertunda()
                        waktuSinkron = formatWaktu(repo.waktuSinkronTerakhir())
                    }

                    InvoiceListScreen(
                        items = items,
                        isSyncing = isSyncing,
                        jumlahAntrean = jumlahAntrean,
                        waktuSinkronTerakhir = waktuSinkron,
                        onBack = { finish() },
                        onOpenDetail = { idInvoice ->
                            val intent = Intent(this@InvoiceMonitoringActivity, InvoiceDetailActivity::class.java)
                            intent.putExtra("idInvoice", idInvoice)
                            startActivity(intent)
                        },
                        onTambahBaru = {
                            startActivity(Intent(this@InvoiceMonitoringActivity, InvoiceFormActivity::class.java))
                        },
                        onSync = {
                            isSyncing = true
                            repo.sync { result ->
                                isSyncing = false
                                items = repo.daftarInvoiceUi()
                                jumlahAntrean = repo.jumlahAntreanTertunda()
                                waktuSinkron = formatWaktu(repo.waktuSinkronTerakhir())
                                when (result) {
                                    is SyncResult.TidakAdaInternet ->
                                        Toast.makeText(this@InvoiceMonitoringActivity, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
                                    is SyncResult.Selesai -> {
                                        val pesan = if (result.gagal == 0) "Sinkron selesai (${result.berhasil} terkirim)"
                                        else "Sinkron selesai: ${result.berhasil} terkirim, ${result.gagal} masih gagal"
                                        Toast.makeText(this@InvoiceMonitoringActivity, pesan, Toast.LENGTH_LONG).show()
                                    }
                                }
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

    private fun formatWaktu(millis: Long): String {
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return sdf.format(millis)
    }
}

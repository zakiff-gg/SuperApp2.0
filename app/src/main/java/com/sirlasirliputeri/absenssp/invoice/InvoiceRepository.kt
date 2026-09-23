// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/InvoiceRepository.kt
package com.sirlasirliputeri.absenssp.invoice

import android.content.Context
import android.util.Base64
import com.sirlasirliputeri.absenssp.network.NetworkStatusHelper
import com.sirlasirliputeri.absenssp.util.PrefsHelper

/** Satu invoice untuk ditampilkan di layar, ditambah penanda "belum ke-sync". */
data class InvoiceUiItem(
    val invoice: Invoice,
    val isLocalDraft: Boolean,      // invoice baru, ID-nya masih sementara (LOCAL-...), belum ada di server
    val pembayaranTertunda: Double  // total cicilan yang sudah dicatat di HP tapi belum terkirim ke server
) {
    val sisaTampil: Double get() = (invoice.sisaTagihan() - pembayaranTertunda).coerceAtLeast(0.0)
    val statusTampil: String
        get() = when {
            isLocalDraft -> "Menunggu Sinkron"
            pembayaranTertunda > 0.0 && sisaTampil <= 0.0 -> "Lunas (menunggu sinkron)"
            pembayaranTertunda > 0.0 -> "${invoice.status} (ada bayar tertunda)"
            else -> invoice.status
        }
}

sealed class SubmitResult {
    data class Terkirim(val idInvoice: String) : SubmitResult()
    data class Diantrekan(val alasan: String) : SubmitResult()
    data class Gagal(val pesan: String) : SubmitResult()
}

sealed class SyncResult {
    object TidakAdaInternet : SyncResult()
    data class Selesai(val berhasil: Int, val gagal: Int) : SyncResult()
}

/**
 * Titik akses tunggal data invoice. Layar Compose TIDAK PERNAH memanggil
 * InvoiceApiService langsung saat dibuka -- selalu baca dari cache lokal
 * (PrefsHelper + InvoiceQueueManager) dulu, instan tanpa nunggu server.
 * Server hanya dikontak lewat sync() (tombol ikon Sinkron) atau saat submit
 * form/pembayaran ketika HP kebetulan online.
 */
class InvoiceRepository(private val context: Context) {

    val queue = InvoiceQueueManager(context)

    /** Daftar gabungan (cache server + draft lokal) untuk ditampilkan, sudah terurut. */
    fun daftarInvoiceUi(): List<InvoiceUiItem> {
        val serverCache = PrefsHelper.getInvoiceCache(context)
        val draftPembayaran = queue.getDaftarPembayaranDraft()
        val draftInvoice = queue.getDaftarInvoiceDraft()

        val dariServer = serverCache.map { inv ->
            val tertunda = draftPembayaran.filter { it.idInvoice == inv.id }.sumOf { it.nominalDibayar }
            InvoiceUiItem(inv, isLocalDraft = false, pembayaranTertunda = tertunda)
        }
        val dariDraft = draftInvoice.map { d ->
            InvoiceUiItem(
                invoice = Invoice(
                    id = d.localId,
                    nomorInvoice = d.nomorInvoice,
                    tanggalInvoice = d.tanggalInvoice,
                    tujuan = d.tujuan,
                    nominal = d.nominal,
                    jatuhTempo = d.jatuhTempo,
                    keterangan = d.keterangan,
                    status = "Belum Bayar",
                    totalDibayar = 0.0,
                    linkPdf = ""
                ),
                isLocalDraft = true,
                pembayaranTertunda = 0.0
            )
        }
        return (dariDraft + dariServer).sortedWith(
            compareBy<InvoiceUiItem> { it.invoice.status == "Lunas" && !it.isLocalDraft }
                .thenByDescending { it.isLocalDraft }
                .thenByDescending { it.invoice.umurHari() }
        )
    }

    /** Nama vendor yang pernah dipakai, buat dropdown -- otomatis dari riwayat, tidak perlu daftar master terpisah. */
    fun daftarVendor(): List<String> {
        val dariServer = PrefsHelper.getInvoiceCache(context).map { it.tujuan }
        val dariDraft = queue.getDaftarInvoiceDraft().map { it.tujuan }
        return (dariServer + dariDraft).filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() }
    }

    /** Nomor invoice yang sudah pernah dipakai (cache server + draft lokal) -- dipakai untuk
     *  memperingatkan pengguna kalau mereka mengetik nomor yang sama dua kali. */
    fun daftarNomorInvoiceTerpakai(): List<String> {
        val dariServer = PrefsHelper.getInvoiceCache(context).map { it.nomorInvoice }
        val dariDraft = queue.getDaftarInvoiceDraft().map { it.nomorInvoice }
        return (dariServer + dariDraft).filter { it.isNotBlank() }.distinct()
    }

    fun waktuSinkronTerakhir(): Long = PrefsHelper.getInvoiceLastSync(context)

    fun jumlahAntreanTertunda(): Int = queue.jumlahAntrean()

    /** Ambil satu invoice (dari cache) plus riwayat pembayarannya (server + draft belum sync). */
    fun detailInvoice(idInvoice: String): Pair<Invoice?, List<Payment>> {
        val inv = PrefsHelper.getInvoiceCache(context).find { it.id == idInvoice }
        val draftPembayaran = queue.getDaftarPembayaranDraft(idInvoice).map {
            Payment(idInvoice = it.idInvoice, tanggalBayar = it.tanggalBayar, nominalDibayar = it.nominalDibayar, catatan = it.catatan + "  (menunggu sinkron)")
        }
        return Pair(inv, draftPembayaran)
    }

    // ---------------- Refresh dari server (dipanggil oleh sync()) ----------------

    private fun refreshDariServer(onDone: (berhasil: Boolean) -> Unit) {
        InvoiceApiService.listInvoice(object : InvoiceApiService.Callback<List<Invoice>> {
            override fun onSuccess(result: List<Invoice>) {
                PrefsHelper.saveInvoiceCache(context, result)
                onDone(true)
            }
            override fun onError(message: String) {
                onDone(false)
            }
        })
    }

    // ---------------- Submit Invoice Baru ----------------

    fun simpanInvoiceBaru(
        nomorInvoice: String,
        tujuan: String,
        nominal: Double,
        tanggalInvoice: String,
        jatuhTempo: String,
        keterangan: String,
        pdfBytes: ByteArray,
        namaFile: String,
        callback: (SubmitResult) -> Unit
    ) {
        if (!NetworkStatusHelper.isOnline(context)) {
            queue.antreInvoiceBaru(nomorInvoice, tujuan, nominal, tanggalInvoice, jatuhTempo, keterangan, pdfBytes)
            callback(SubmitResult.Diantrekan("Sedang offline, invoice disimpan di HP dan akan terkirim otomatis saat Sinkron"))
            return
        }
        val base64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
        InvoiceApiService.simpanInvoice(
            nomorInvoice, tujuan, nominal, tanggalInvoice, jatuhTempo, keterangan, base64, namaFile,
            object : InvoiceApiService.Callback<String> {
                override fun onSuccess(result: String) {
                    refreshDariServer { }
                    callback(SubmitResult.Terkirim(result))
                }
                override fun onError(message: String) {
                    queue.antreInvoiceBaru(nomorInvoice, tujuan, nominal, tanggalInvoice, jatuhTempo, keterangan, pdfBytes)
                    callback(SubmitResult.Diantrekan("Gagal kirim ($message), invoice disimpan di HP dan akan dicoba lagi saat Sinkron"))
                }
            }
        )
    }

    // ---------------- Submit Pembayaran ----------------

    fun tambahPembayaran(
        idInvoice: String,
        tanggalBayar: String,
        nominalDibayar: Double,
        catatan: String,
        callback: (SubmitResult) -> Unit
    ) {
        if (!NetworkStatusHelper.isOnline(context)) {
            queue.antrePembayaranBaru(idInvoice, tanggalBayar, nominalDibayar, catatan)
            callback(SubmitResult.Diantrekan("Sedang offline, pembayaran disimpan di HP dan akan terkirim otomatis saat Sinkron"))
            return
        }
        InvoiceApiService.tambahPembayaran(
            idInvoice, tanggalBayar, nominalDibayar, catatan,
            object : InvoiceApiService.Callback<Unit> {
                override fun onSuccess(result: Unit) {
                    refreshDariServer { }
                    callback(SubmitResult.Terkirim(idInvoice))
                }
                override fun onError(message: String) {
                    queue.antrePembayaranBaru(idInvoice, tanggalBayar, nominalDibayar, catatan)
                    callback(SubmitResult.Diantrekan("Gagal kirim ($message), pembayaran disimpan di HP dan akan dicoba lagi saat Sinkron"))
                }
            }
        )
    }

    // ---------------- Sinkronisasi Manual ----------------

    /** Kirim semua draft tertunda satu-satu, lalu refresh cache dari server. */
    fun sync(callback: (SyncResult) -> Unit) {
        if (!NetworkStatusHelper.isOnline(context)) {
            callback(SyncResult.TidakAdaInternet)
            return
        }
        kirimSemuaInvoiceDraft(berhasil = 0, gagal = 0) { berhasilInv, gagalInv ->
            kirimSemuaPembayaranDraft(berhasil = berhasilInv, gagal = gagalInv) { berhasilTotal, gagalTotal ->
                refreshDariServer {
                    callback(SyncResult.Selesai(berhasilTotal, gagalTotal))
                }
            }
        }
    }

    private fun kirimSemuaInvoiceDraft(berhasil: Int, gagal: Int, onDone: (Int, Int) -> Unit) {
        val sisa = queue.getDaftarInvoiceDraft()
        if (sisa.isEmpty()) { onDone(berhasil, gagal); return }
        val d = sisa.first()
        val pdfBytes = queue.bacaPdfBytesDraft(d.pdfFileName)
        if (pdfBytes == null) {
            // berkas hilang -- buang draft rusak ini supaya tidak mengganjal antrean selamanya
            queue.hapusInvoiceDraft(d.localId)
            kirimSemuaInvoiceDraft(berhasil, gagal + 1, onDone)
            return
        }
        val base64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
        val namaFile = "Invoice_${d.tujuan}_${d.tanggalInvoice}.pdf"
        InvoiceApiService.simpanInvoice(
            d.nomorInvoice, d.tujuan, d.nominal, d.tanggalInvoice, d.jatuhTempo, d.keterangan, base64, namaFile,
            object : InvoiceApiService.Callback<String> {
                override fun onSuccess(result: String) {
                    queue.hapusInvoiceDraft(d.localId)
                    kirimSemuaInvoiceDraft(berhasil + 1, gagal, onDone)
                }
                override fun onError(message: String) {
                    // biarkan tetap di antrean, coba lagi lain kali; lanjut ke draft berikutnya
                    kirimSemuaInvoiceDraft(berhasil, gagal + 1, onDone)
                }
            }
        )
    }

    private fun kirimSemuaPembayaranDraft(berhasil: Int, gagal: Int, onDone: (Int, Int) -> Unit) {
        val sisa = queue.getDaftarPembayaranDraft()
        if (sisa.isEmpty()) { onDone(berhasil, gagal); return }
        val d = sisa.first()
        InvoiceApiService.tambahPembayaran(
            d.idInvoice, d.tanggalBayar, d.nominalDibayar, d.catatan,
            object : InvoiceApiService.Callback<Unit> {
                override fun onSuccess(result: Unit) {
                    queue.hapusPembayaranDraft(d.localId)
                    kirimSemuaPembayaranDraft(berhasil + 1, gagal, onDone)
                }
                override fun onError(message: String) {
                    kirimSemuaPembayaranDraft(berhasil, gagal + 1, onDone)
                }
            }
        )
    }
}

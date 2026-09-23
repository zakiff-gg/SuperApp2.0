// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/invoice/InvoiceQueueManager.kt
package com.sirlasirliputeri.absenssp.invoice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Draft invoice baru yang BELUM berhasil terkirim ke server (dibuat saat offline,
 * atau saat online tapi request gagal). PDF hasil scan disimpan sebagai file
 * terpisah di storage internal (sama seperti pola OperasionalQueueManager),
 * bukan di JSON, karena ukurannya besar.
 */
data class InvoiceDraft(
    val localId: String,       // contoh: LOCAL-a1b2c3d4
    val nomorInvoice: String,
    val tujuan: String,
    val nominal: Double,
    val tanggalInvoice: String,
    val jatuhTempo: String,
    val keterangan: String,
    val pdfFileName: String,
    val timestamp: Long
)

/** Draft pembayaran (cicilan) untuk invoice tertentu yang belum sempat terkirim. */
data class PaymentDraft(
    val localId: String,
    val idInvoice: String,     // ID invoice ASLI dari server (invoice ini pasti sudah ada di server)
    val tanggalBayar: String,
    val nominalDibayar: Double,
    val catatan: String,
    val timestamp: Long
)

/**
 * Antrean lokal modul Invoice. Dipakai supaya "Tambah Invoice" dan "Tambah
 * Pembayaran" tetap bisa dipakai walau HP sedang tidak ada sinyal -- datanya
 * ditahan di HP dulu, lalu dikirim satu-satu saat tombol Sinkron ditekan
 * (atau otomatis dicoba saat submit dan ternyata sudah online lagi).
 */
class InvoiceQueueManager(private val context: Context) {

    private val pdfDir: File
        get() = File(context.filesDir, "invoice_queue_pdf").apply { mkdirs() }

    private fun invoiceMetaFile(): File = File(context.filesDir, "invoice_queue_meta.json")
    private fun paymentMetaFile(): File = File(context.filesDir, "invoice_payment_queue_meta.json")

    // ---------------- Draft Invoice Baru ----------------

    @Synchronized
    fun antreInvoiceBaru(
        nomorInvoice: String,
        tujuan: String,
        nominal: Double,
        tanggalInvoice: String,
        jatuhTempo: String,
        keterangan: String,
        pdfBytes: ByteArray
    ): InvoiceDraft {
        val localId = "LOCAL-" + UUID.randomUUID().toString().take(8)
        val fileName = "$localId.pdf"
        File(pdfDir, fileName).writeBytes(pdfBytes)
        val draft = InvoiceDraft(
            localId = localId,
            nomorInvoice = nomorInvoice,
            tujuan = tujuan,
            nominal = nominal,
            tanggalInvoice = tanggalInvoice,
            jatuhTempo = jatuhTempo,
            keterangan = keterangan,
            pdfFileName = fileName,
            timestamp = System.currentTimeMillis()
        )
        val list = getDaftarInvoiceDraft().toMutableList()
        list.add(draft)
        simpanInvoiceMeta(list)
        return draft
    }

    @Synchronized
    fun getDaftarInvoiceDraft(): List<InvoiceDraft> {
        val file = invoiceMetaFile()
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                InvoiceDraft(
                    localId = o.optString("localId"),
                    nomorInvoice = o.optString("nomorInvoice"),
                    tujuan = o.optString("tujuan"),
                    nominal = o.optDouble("nominal", 0.0),
                    tanggalInvoice = o.optString("tanggalInvoice"),
                    jatuhTempo = o.optString("jatuhTempo"),
                    keterangan = o.optString("keterangan"),
                    pdfFileName = o.optString("pdfFileName"),
                    timestamp = o.optLong("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun bacaPdfBytesDraft(pdfFileName: String): ByteArray? {
        val file = File(pdfDir, pdfFileName)
        return if (file.exists()) file.readBytes() else null
    }

    @Synchronized
    fun hapusInvoiceDraft(localId: String) {
        val list = getDaftarInvoiceDraft()
        val draft = list.find { it.localId == localId }
        draft?.let { File(pdfDir, it.pdfFileName).delete() }
        simpanInvoiceMeta(list.filterNot { it.localId == localId })
    }

    private fun simpanInvoiceMeta(list: List<InvoiceDraft>) {
        val arr = JSONArray()
        list.forEach { d ->
            val o = JSONObject()
            o.put("localId", d.localId)
            o.put("nomorInvoice", d.nomorInvoice)
            o.put("tujuan", d.tujuan)
            o.put("nominal", d.nominal)
            o.put("tanggalInvoice", d.tanggalInvoice)
            o.put("jatuhTempo", d.jatuhTempo)
            o.put("keterangan", d.keterangan)
            o.put("pdfFileName", d.pdfFileName)
            o.put("timestamp", d.timestamp)
            arr.put(o)
        }
        invoiceMetaFile().writeText(arr.toString())
    }

    // ---------------- Draft Pembayaran ----------------

    @Synchronized
    fun antrePembayaranBaru(
        idInvoice: String,
        tanggalBayar: String,
        nominalDibayar: Double,
        catatan: String
    ): PaymentDraft {
        val draft = PaymentDraft(
            localId = "LOCALBYR-" + UUID.randomUUID().toString().take(8),
            idInvoice = idInvoice,
            tanggalBayar = tanggalBayar,
            nominalDibayar = nominalDibayar,
            catatan = catatan,
            timestamp = System.currentTimeMillis()
        )
        val list = getDaftarPembayaranDraft().toMutableList()
        list.add(draft)
        simpanPembayaranMeta(list)
        return draft
    }

    @Synchronized
    fun getDaftarPembayaranDraft(): List<PaymentDraft> {
        val file = paymentMetaFile()
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                PaymentDraft(
                    localId = o.optString("localId"),
                    idInvoice = o.optString("idInvoice"),
                    tanggalBayar = o.optString("tanggalBayar"),
                    nominalDibayar = o.optDouble("nominalDibayar", 0.0),
                    catatan = o.optString("catatan"),
                    timestamp = o.optLong("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDaftarPembayaranDraft(idInvoice: String): List<PaymentDraft> =
        getDaftarPembayaranDraft().filter { it.idInvoice == idInvoice }

    @Synchronized
    fun hapusPembayaranDraft(localId: String) {
        simpanPembayaranMeta(getDaftarPembayaranDraft().filterNot { it.localId == localId })
    }

    private fun simpanPembayaranMeta(list: List<PaymentDraft>) {
        val arr = JSONArray()
        list.forEach { d ->
            val o = JSONObject()
            o.put("localId", d.localId)
            o.put("idInvoice", d.idInvoice)
            o.put("tanggalBayar", d.tanggalBayar)
            o.put("nominalDibayar", d.nominalDibayar)
            o.put("catatan", d.catatan)
            o.put("timestamp", d.timestamp)
            arr.put(o)
        }
        paymentMetaFile().writeText(arr.toString())
    }

    // ---------------- Ringkasan ----------------

    fun hasQueuedData(): Boolean =
        getDaftarInvoiceDraft().isNotEmpty() || getDaftarPembayaranDraft().isNotEmpty()

    fun jumlahAntrean(): Int =
        getDaftarInvoiceDraft().size + getDaftarPembayaranDraft().size
}

package com.sirlasirliputeri.absenssp.invoice

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Semua request untuk modul invoice mengarah ke InvoiceConfig.INVOICE_SCRIPT_URL,
 * TERPISAH dari ApiService modul tool management yang mengarah ke spreadsheet lain.
 *
 * Pola & gaya sengaja disamakan dengan ApiService yang sudah ada di modul tool
 * management (HttpURLConnection manual, callback di main thread) supaya konsisten.
 */
object InvoiceApiService {

    private val mainHandler = Handler(Looper.getMainLooper())

    interface Callback<T> {
        fun onSuccess(result: T)
        fun onError(message: String)
    }

    // ---------- SIMPAN INVOICE BARU (form + PDF hasil scan) ----------
    fun simpanInvoice(
        nomorInvoice: String,
        tujuan: String,
        nominal: Double,
        tanggalInvoice: String,
        jatuhTempo: String,
        keterangan: String,
        pdfBase64: String,
        namaFile: String,
        callback: Callback<String> // mengembalikan ID invoice yang baru dibuat
    ) {
        Thread {
            try {
                val body = JSONObject().apply {
                    put("action", "simpanInvoice")
                    put("nomorInvoice", nomorInvoice)
                    put("tujuan", tujuan)
                    put("nominal", nominal)
                    put("tanggalInvoice", tanggalInvoice)
                    put("jatuhTempo", jatuhTempo)
                    put("keterangan", keterangan)
                    put("pdfBase64", pdfBase64)
                    put("namaFile", namaFile)
                }
                val response = postJson(body)
                if (response.optBoolean("success", false)) {
                    postSuccess(callback, response.optString("idInvoice"))
                } else {
                    postError(callback, response.optString("message", "Gagal menyimpan invoice"))
                }
            } catch (e: Exception) {
                postError(callback, "Kesalahan jaringan: ${e.message}")
            }
        }.start()
    }

    // ---------- TAMBAH PEMBAYARAN (cicilan) UNTUK INVOICE TERTENTU ----------
    fun tambahPembayaran(
        idInvoice: String,
        tanggalBayar: String,
        nominalDibayar: Double,
        catatan: String,
        callback: Callback<Unit>
    ) {
        Thread {
            try {
                val body = JSONObject().apply {
                    put("action", "tambahPembayaran")
                    put("idInvoice", idInvoice)
                    put("tanggalBayar", tanggalBayar)
                    put("nominalDibayar", nominalDibayar)
                    put("catatan", catatan)
                }
                val response = postJson(body)
                if (response.optBoolean("success", false)) {
                    postSuccess(callback, Unit)
                } else {
                    postError(callback, response.optString("message", "Gagal menyimpan pembayaran"))
                }
            } catch (e: Exception) {
                postError(callback, "Kesalahan jaringan: ${e.message}")
            }
        }.start()
    }

    // ---------- LIST SEMUA INVOICE (untuk layar monitoring) ----------
    fun listInvoice(callback: Callback<List<Invoice>>) {
        Thread {
            try {
                val json = getJson("listInvoice", emptyMap())
                val arr = json.optJSONArray("data") ?: JSONArray()
                val result = mutableListOf<Invoice>()
                for (i in 0 until arr.length()) {
                    result.add(parseInvoice(arr.getJSONObject(i)))
                }
                postSuccess(callback, result)
            } catch (e: Exception) {
                postError(callback, "Kesalahan jaringan: ${e.message}")
            }
        }.start()
    }

    // ---------- DETAIL 1 INVOICE + RIWAYAT PEMBAYARANNYA ----------
    fun detailInvoice(idInvoice: String, callback: Callback<Pair<Invoice, List<Payment>>>) {
        Thread {
            try {
                val json = getJson("detailInvoice", mapOf("id" to idInvoice))
                if (!json.optBoolean("success", false)) {
                    postError(callback, json.optString("message", "Invoice tidak ditemukan"))
                    return@Thread
                }
                val invoice = parseInvoice(json.getJSONObject("invoice"))
                val arr = json.optJSONArray("pembayaran") ?: JSONArray()
                val payments = mutableListOf<Payment>()
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    payments.add(
                        Payment(
                            idInvoice = idInvoice,
                            tanggalBayar = p.optString("tanggalBayar"),
                            nominalDibayar = p.optDouble("nominalDibayar", 0.0),
                            catatan = p.optString("catatan")
                        )
                    )
                }
                postSuccess(callback, Pair(invoice, payments))
            } catch (e: Exception) {
                postError(callback, "Kesalahan jaringan: ${e.message}")
            }
        }.start()
    }

    // ------------------------------------------------------------------
    // Helper internal
    // ------------------------------------------------------------------

    private fun parseInvoice(o: JSONObject): Invoice = Invoice(
        id = o.optString("id"),
        nomorInvoice = o.optString("nomorInvoice"),
        tanggalInvoice = o.optString("tanggalInvoice"),
        tujuan = o.optString("tujuan"),
        nominal = o.optDouble("nominal", 0.0),
        jatuhTempo = o.optString("jatuhTempo"),
        keterangan = o.optString("keterangan"),
        status = o.optString("status"),
        totalDibayar = o.optDouble("totalDibayar", 0.0),
        linkPdf = o.optString("linkPdf")
    )

    private fun postJson(body: JSONObject): JSONObject {
        val url = URL(InvoiceConfig.INVOICE_SCRIPT_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        return JSONObject(text)
    }

    private fun getJson(action: String, params: Map<String, String>): JSONObject {
        val query = StringBuilder("?action=${URLEncoder.encode(action, "UTF-8")}")
        for ((k, v) in params) {
            query.append("&$k=${URLEncoder.encode(v, "UTF-8")}")
        }
        val url = URL(InvoiceConfig.INVOICE_SCRIPT_URL + query.toString())
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        return JSONObject(text)
    }

    private fun <T> postSuccess(callback: Callback<T>, result: T) {
        mainHandler.post { callback.onSuccess(result) }
    }

    private fun <T> postError(callback: Callback<T>, message: String) {
        mainHandler.post { callback.onError(message) }
    }
}

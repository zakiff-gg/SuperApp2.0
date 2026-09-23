// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/network/ApiClient.kt
package com.sirlasirliputeri.absenssp.network

import com.sirlasirliputeri.absenssp.model.AppConstants
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Client jaringan murni menggunakan HttpURLConnection + org.json (tanpa OkHttp/Retrofit)
 * sesuai spesifikasi efisiensi ukuran APK. Semua pemanggilan bersifat blocking sehingga
 * WAJIB dipanggil dari coroutine/background thread (lihat pemanggilnya di layer UI).
 *
 * CATATAN PENTING: Google Apps Script Web App SELALU membalas dengan HTTP 302 yang
 * mengarah ke URL script.googleusercontent.com berisi hasil eksekusi sebenarnya.
 * Untuk request GET, HttpURLConnection otomatis mengikuti redirect ini tanpa masalah.
 * Untuk request POST, mengikuti redirect otomatis akan GAGAL karena body POST yang
 * sudah terkirim tidak bisa dikirim ulang oleh JVM secara otomatis (unresolved retry
 * of request body) -- inilah penyebab error "Gagal menyimpan data" walau internet lancar.
 * Solusinya: matikan auto-redirect khusus untuk POST, lalu ikuti header "Location"
 * secara manual dengan request GET biasa (karena kontennya sudah jadi, tidak perlu
 * mengirim ulang body).
 */
object ApiClient {

    private const val TIMEOUT_MS = 15000
    // Upload foto (Laporan Operasional) butuh waktu lebih lama dari request biasa,
    // terutama di lokasi lapangan dengan koneksi lambat.
    const val TIMEOUT_UPLOAD_MS = 45000

    /**
     * Kirim POST request berformat application/x-www-form-urlencoded ke Web App GAS.
     * Mengembalikan JSONObject hasil parsing response, atau melempar Exception jika gagal.
     */
    @Throws(Exception::class)
    fun post(params: Map<String, String>, timeoutMs: Int = TIMEOUT_MS): JSONObject {
        val url = URL(AppConstants.WEB_APP_URL)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.instanceFollowRedirects = false // redirect ditangani manual, lihat catatan di atas
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")

            val body = params.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val responseCode = conn.responseCode

            if (isRedirect_(responseCode)) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrEmpty()) {
                    throw Exception("Redirect dari server tidak berisi URL tujuan.")
                }
                return fetchGet_(location, timeoutMs)
            }

            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            return JSONObject(readStream_(stream))
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Kirim GET request (dipakai untuk aksi ringan seperti getProjects / getTodayCount,
     * dan juga dipakai internal untuk mengikuti redirect hasil POST).
     */
    @Throws(Exception::class)
    fun get(action: String, extraParams: Map<String, String> = emptyMap(), timeoutMs: Int = TIMEOUT_MS): JSONObject {
        val query = StringBuilder("action=${URLEncoder.encode(action, "UTF-8")}")
        for ((k, v) in extraParams) {
            query.append("&${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}")
        }
        return fetchGet_("${AppConstants.WEB_APP_URL}?$query", timeoutMs)
    }

    private fun isRedirect_(responseCode: Int): Boolean {
        return responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == HttpURLConnection.HTTP_SEE_OTHER
    }

    private fun fetchGet_(urlString: String, timeoutMs: Int = TIMEOUT_MS): JSONObject {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.instanceFollowRedirects = true
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            return JSONObject(readStream_(stream))
        } finally {
            conn.disconnect()
        }
    }

    private fun readStream_(stream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream))
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
        reader.close()
        return sb.toString()
    }
}

// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/offline/OfflineQueueManager.kt
package com.sirlasirliputeri.absenssp.offline

import android.content.Context
import com.sirlasirliputeri.absenssp.model.AppConstants
import com.sirlasirliputeri.absenssp.network.ApiClient
import java.io.File

/**
 * Antrean CSV lokal untuk absensi saat internet mati (Offline Fallback).
 * Format baris: uid,jenis,proyek,timestampEpochMillis
 * Setiap baris menyimpan timestamp ASLI saat tap terjadi, bukan saat data terkirim,
 * sehingga jam & tanggal absensi tetap akurat walau baru disinkronkan belakangan.
 */
class OfflineQueueManager(private val context: Context) {

    private fun queueFile(): File = File(context.filesDir, AppConstants.OFFLINE_QUEUE_FILE)

    @Synchronized
    fun enqueue(uid: String, jenis: String, proyek: String, timestampMillis: Long) {
        val file = queueFile()
        val line = "$uid,$jenis,$proyek,$timestampMillis\n"
        file.appendText(line)
    }

    @Synchronized
    fun hasQueuedData(): Boolean = queueFile().exists() && queueFile().length() > 0

    /**
     * Coba dorong semua data offline ke server. Baris yang gagal terkirim akan
     * dipertahankan di antrean untuk dicoba ulang pada kesempatan berikutnya.
     * Dipanggil dari onResume() atau saat konektivitas kembali online.
     */
    @Synchronized
    fun flushQueue(): Int {
        val file = queueFile()
        if (!file.exists()) return 0

        val lines = file.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return 0

        val stillFailed = mutableListOf<String>()
        var successCount = 0

        for (line in lines) {
            val parts = line.split(",")
            if (parts.size < 4) continue
            val (uid, jenis, proyek, timestamp) = parts
            try {
                val result = ApiClient.post(
                    mapOf(
                        "action" to "absen",
                        "uid" to uid,
                        "jenis" to jenis,
                        "proyek" to proyek,
                        "timestamp" to timestamp,
                        "offlineSync" to "true"
                    )
                )
                if (result.optString("status") == "ERROR_UNKNOWN") {
                    stillFailed.add(line)
                } else {
                    successCount++
                }
            } catch (e: Exception) {
                // Masih offline / gagal jaringan, simpan lagi untuk dicoba nanti.
                stillFailed.add(line)
            }
        }

        if (stillFailed.isEmpty()) {
            file.delete()
        } else {
            file.writeText(stillFailed.joinToString("\n") + "\n")
        }
        return successCount
    }
}

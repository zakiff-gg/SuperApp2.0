// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/util/TimeValidator.kt
package com.sirlasirliputeri.absenssp.util

import android.content.Context
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*

/**
 * Proteksi Jam Perangkat (Anti-Tampering Clock).
 * Mendeteksi apakah "Automatic Time & Date" pada HP dinonaktifkan secara manual.
 * Jika nonaktif, fungsi tap HARUS dibekukan di sisi UI (lihat StandbyScreen).
 */
object TimeValidator {

    fun isAutoTimeEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME) == 1
        } catch (e: Exception) {
            // Jika setting tidak ditemukan (perangkat non-standar), anggap valid agar
            // tidak memblokir operasional secara tidak sengaja.
            true
        }
    }

    fun currentDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    fun currentTimeString(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    fun currentTimeHHmm(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    fun isJamBerangkat(): Boolean {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour in 6..15
    }
}

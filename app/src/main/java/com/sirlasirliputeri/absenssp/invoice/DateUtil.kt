package com.sirlasirliputeri.absenssp.invoice

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtil {

    private const val PATTERN = "yyyy-MM-dd"

    fun today(): String {
        val sdf = SimpleDateFormat(PATTERN, Locale("id", "ID"))
        return sdf.format(Calendar.getInstance().time)
    }

    /** Selisih hari dari [tanggal] (yyyy-MM-dd) sampai hari ini. Selalu >= 0 kalau tanggal di masa lalu. */
    fun selisihHariDariHariIni(tanggal: String): Long {
        val t = parse(tanggal) ?: return 0
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0); now.set(Calendar.MILLISECOND, 0)
        val diffMs = now.timeInMillis - t.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMs)
    }

    /** Selisih hari dari hari ini ke [tanggal]. Positif = tanggal masih di masa depan. Negatif = sudah lewat. */
    fun selisihHariKeTanggal(tanggal: String): Long {
        return -selisihHariDariHariIni(tanggal)
    }

    private fun parse(tanggal: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat(PATTERN, Locale("id", "ID"))
            val date = sdf.parse(tanggal) ?: return null
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal
        } catch (e: Exception) {
            null
        }
    }
}

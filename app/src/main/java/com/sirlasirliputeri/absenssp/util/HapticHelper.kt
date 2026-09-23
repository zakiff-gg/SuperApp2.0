// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/util/HapticHelper.kt
package com.sirlasirliputeri.absenssp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Getaran singkat sebagai konfirmasi taktil tap kartu -- berguna karena HP sering
 * ditaruh di kantong/mounting dan tidak selalu langsung dilihat layarnya.
 * Aman untuk minSdk 24 (VibrationEffect butuh API 26+, di bawah itu fallback ke
 * vibrate(long) versi lama).
 */
object HapticHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Sukses: satu getar pendek & tegas. */
    fun sukses(context: Context) = getarkan(context, longArrayOf(0, 40))

    /** Gagal / tidak dikenal / sudah absen: dua getar pendek beruntun, terasa berbeda dari sukses. */
    fun gagal(context: Context) = getarkan(context, longArrayOf(0, 35, 60, 35))

    private fun getarkan(context: Context, pattern: LongArray) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Getaran gagal (mis. izin dicabut manual) tidak boleh mengganggu proses absen
        }
    }
}

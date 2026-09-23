// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/util/SoundHelper.kt
package com.sirlasirliputeri.absenssp.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Audio Feedback System berbasis ToneGenerator (tanpa file audio eksternal, hemat ukuran
 * APK). Memakai kombinasi 2 nada DTMF pendek untuk menghasilkan bunyi "chime" modern --
 * mirip nada konfirmasi pembayaran contactless -- dan bukan lagi bunyi "beep" mesin lama.
 */
object SoundHelper {

    private val handler = Handler(Looper.getMainLooper())

    /** Sukses: 2 nada naik cepat, ceria dan singkat. */
    fun playSukses() {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tg.startTone(ToneGenerator.TONE_DTMF_6, 70)
        handler.postDelayed({
            tg.startTone(ToneGenerator.TONE_DTMF_9, 110)
            handler.postDelayed({ tg.release() }, 160)
        }, 80)
    }

    /** Sudah absen: 1 nada datar, netral -- bukan lagi beep-beep kasar. */
    fun playSudahAbsen() {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
        tg.startTone(ToneGenerator.TONE_DTMF_7, 150)
        handler.postDelayed({ tg.release() }, 230)
    }

    /** Error / tidak dikenal: 2 nada turun, mirip bunyi "ditolak" pada pembayaran modern. */
    fun playError() {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
        tg.startTone(ToneGenerator.TONE_DTMF_4, 110)
        handler.postDelayed({
            tg.startTone(ToneGenerator.TONE_DTMF_1, 160)
            handler.postDelayed({ tg.release() }, 240)
        }, 120)
    }
}

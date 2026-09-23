// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/util/AbsensiRules.kt
package com.sirlasirliputeri.absenssp.util

import com.sirlasirliputeri.absenssp.model.AppConstants

/**
 * Aturan bisnis absensi (Telat / Tepat Waktu / Lembur / Setengah Hari) dihitung LANGSUNG
 * DI HP supaya feedback ke petugas instan tanpa menunggu server. Logika ini SENGAJA dibuat
 * identik dengan hitungKeteranganBerangkat_/hitungKeteranganPulang_ di Code.gs. Kalau
 * salah satu aturan diubah, ubah juga yang satunya supaya keterangan yang tampil di HP
 * dan yang tersimpan di Google Sheets tidak pernah berbeda.
 */
object AbsensiRules {
    fun keteranganBerangkat(jamHHmm: String): String {
        return if (jamHHmm < AppConstants.JAM_MASUK_NORMAL_MULAI || jamHHmm > AppConstants.JAM_MASUK_NORMAL_SELESAI) {
            "Telat (Masuk)"
        } else {
            "Tepat Waktu"
        }
    }

    fun keteranganPulang(jamHHmm: String): String {
        return when {
            jamHHmm < AppConstants.JAM_PULANG_NORMAL_MULAI -> "Pulang - Setengah Hari"
            jamHHmm > AppConstants.JAM_PULANG_NORMAL_SELESAI -> "Lembur (Pulang)"
            else -> "Tepat Waktu"
        }
    }
}

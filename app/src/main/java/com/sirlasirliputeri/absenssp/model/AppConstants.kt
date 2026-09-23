// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/model/AppConstants.kt
package com.sirlasirliputeri.absenssp.model

/**
 * Konfigurasi pusat aplikasi. GANTI nilai WEB_APP_URL dan ADMIN_PASSWORD
 * sesuai deployment Google Apps Script dan kebijakan internal perusahaan.
 */
object AppConstants {
    // Ganti dengan URL Web App hasil "Deploy > New deployment" pada Google Apps Script.
    const val WEB_APP_URL = "https://script.google.com/macros/s/AKfycbzkEykXX4YXGZgAWPS_71M60_j8WaXbK5av6lyp6KAV_9BN9QxehV-jZn3xNbh5Jci9SQ/exec"

    // Password default menu rahasia admin. Simpan dengan aman, sebaiknya diubah berkala.
    const val DEFAULT_ADMIN_PASSWORD = "ssp2026admin"

    const val PREFS_NAME = "absen_ssp_prefs"
    const val PREF_ADMIN_PASSWORD = "admin_password"

    const val OFFLINE_QUEUE_FILE = "antrean_absen_offline.csv"

    // Debounce anti-dobel-baca kartu NFC (dalam milidetik)
    const val NFC_DEBOUNCE_MS = 3500L

    // Durasi tampilan hasil pop-up sebelum kembali ke standby (dalam milidetik)
    const val RESULT_DISPLAY_MS = 2500L

    // Jam batas absen berangkat vs pulang
    const val JAM_BATAS_BERANGKAT_MULAI = "06:00"
    const val JAM_BATAS_BERANGKAT_SELESAI = "15:59"
    const val JAM_MULAI_PULANG = "16:00"

    const val JENIS_BERANGKAT = "Berangkat"
    const val JENIS_PULANG = "Pulang"

    // Aturan jam normal masuk/pulang -- HARUS SAMA PERSIS dengan konstanta di Code.gs
    // (JAM_MASUK_NORMAL_MULAI dkk) karena keterangan "Telat/Lembur/dst" sekarang dihitung
    // di HP (lihat AbsensiRules.kt), bukan menunggu jawaban server.
    const val JAM_MASUK_NORMAL_MULAI = "06:00"
    const val JAM_MASUK_NORMAL_SELESAI = "08:00"
    const val JAM_PULANG_NORMAL_MULAI = "16:00"
    const val JAM_PULANG_NORMAL_SELESAI = "17:00"
}

// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/model/Karyawan.kt
package com.sirlasirliputeri.absenssp.model

data class Karyawan(
    val uid: String,
    val nama: String,
    val tempatKerja: String,
    val posisi: String,
    val tanggalDaftar: String,
    val upahHarian: Double = 0.0,
    val upahLembur: Double = 0.0,
    // Agregat profil (jumlah hari masuk, jam lembur, bon) ikut disinkronkan bersama
    // data karyawan supaya Menu Profil Karyawan bisa dibuka INSTAN & OFFLINE, tanpa
    // perlu pilih ulang tiap buka menu. jumlahHariMasuk berupa Double karena Absen
    // Berangkat = 0.5 hari, Absen Pulang = 0.5 hari (lengkap keduanya = 1 hari penuh).
    val jumlahHariMasuk: Double = 0.0,
    val jumlahJamLembur: Double = 0.0,
    val bonDiterima: Double = 0.0,
    val maksimalBon: Double = 0.0,
    // true kalau masih ada pengajuan bon berstatus "Menunggu Pencairan" -- dipakai
    // untuk mencegah pengajuan dobel sebelum bon sebelumnya benar-benar dicairkan.
    val adaPengajuanMenunggu: Boolean = false,
    // Akses menu Laporan Operasional HANYA untuk karyawan tertentu (admin lapangan),
    // ditandai admin lewat kolom "Akses Operasional" di sheet Karyawan.
    val aksesOperasional: Boolean = false,
    // Saldo berjalan Laporan Operasional milik orang ini (masing-masing admin
    // lapangan punya saldo sendiri-sendiri).
    val saldoOperasional: Double = 0.0
)

data class AbsensiRecord(
    val uid: String,
    val nama: String,
    val tanggal: String,
    val jam: String,
    val jenis: String,
    val keterangan: String,
    val proyek: String
)

data class TapResult(
    val status: String,      // "SUKSES", "SUDAH_ABSEN", "ERROR", "TIDAK_DIKENAL"
    val pesan: String,
    val nama: String = "",
    val jenis: String = "",
    val keterangan: String = ""
)

/**
 * Satu baris absen hari ini, disimpan di cache lokal HP supaya pengecekan "sudah absen"
 * dan pencocokan proyek untuk absen Pulang bisa instan tanpa nunggu server.
 */
data class AbsensiHariIniEntry(
    val uid: String,
    val nama: String,
    val jenis: String,
    val proyek: String,
    val keterangan: String,
    val jam: String
)

/**
 * Ringkasan profil karyawan untuk ditampilkan di menu Profil Karyawan. Dibangun
 * langsung dari cache lokal Karyawan -- sepenuhnya OFFLINE & INSTAN, diperbarui
 * tiap kali Sinkronisasi Data dijalankan. Identitas dipilih sekali dari daftar nama
 * (bukan tap kartu lagi), tersimpan di HP (lihat PrefsHelper.saveProfilIdentity).
 */
data class ProfilKaryawan(
    val uid: String,
    val nama: String,
    val jumlahHariMasuk: Double,
    val upahHarian: Double,
    val upahLembur: Double,
    val jumlahJamLembur: Double,
    val bonDiterima: Double,
    val maksimalBon: Double,
    val adaPengajuanMenunggu: Boolean = false
)

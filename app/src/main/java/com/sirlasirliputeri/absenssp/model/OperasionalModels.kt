// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/model/OperasionalModels.kt
package com.sirlasirliputeri.absenssp.model

object KategoriOperasional {
    const val OPERASIONAL = "Operasional"
    const val PEMBELIAN_MATERIAL = "Pembelian Material"
    const val LAIN_LAIN = "Lain-lain"

    val DAFTAR = listOf(OPERASIONAL, PEMBELIAN_MATERIAL, LAIN_LAIN)
}

object TipeOperasional {
    const val MASUK = "Masuk"
    const val KELUAR = "Keluar"
}

/**
 * Satu baris Laporan Operasional yang SUDAH TERSIMPAN di server (dari sheet
 * "Operasional" hari ini). Dicache lokal lewat getTodayOperasional supaya menu
 * "Laporan Hari Ini milik saya" bisa tampil offline & tahu mana yang boleh diedit.
 */
data class OperasionalEntry(
    val id: String,
    val uid: String,
    val tanggal: String,
    val tipe: String,
    val kategori: String,
    val jumlah: Double,
    val keterangan: String,
    val saldo: Double,
    val namaFileBukti: String,
    val urlBukti: String,
    val diinputOleh: String
)

/**
 * Draft laporan yang BELUM berhasil terkirim ke server (submit gagal / offline saat
 * itu). Foto disimpan sebagai file terpisah di storage internal (lihat
 * OperasionalQueueManager), bukan di dalam objek ini, supaya metadata tetap ringan.
 */
data class OperasionalDraft(
    val localId: String,
    val uid: String,
    val namaPengirim: String,
    val editId: String? = null, // null = laporan baru, terisi = draft utk edit laporan existing
    val tipe: String,
    val kategori: String,
    val jumlah: Double,
    val keterangan: String,
    val fotoFileName: String,
    val timestamp: Long
)

/**
 * Satu baris riwayat Laporan Operasional (BUKAN cuma hari ini) milik SATU orang --
 * dipakai di menu "Riwayat Laporan". SENGAJA TIDAK menyimpan namaFileBukti, cuma
 * urlBukti (link teks) -- foto tidak ikut disinkron supaya hemat kuota, baru
 * diambil kalau link-nya benar-benar diketuk.
 */
data class RiwayatOperasionalEntry(
    val id: String,
    val tanggal: String,
    val tipe: String,
    val kategori: String,
    val jumlah: Double,
    val keterangan: String,
    val saldo: Double,
    val urlBukti: String
)

/**
 * Status pengiriman laporan yang sedang berjalan di BACKGROUND (lihat MainActivity).
 * Ditampilkan sebagai badge "Mengirim..." / "Gagal, ketuk untuk coba lagi" di daftar
 * Laporan Hari Ini -- supaya petugas bisa langsung isi laporan berikutnya tanpa
 * menunggu proses kirim yang sedang berjalan selesai.
 */
data class PendingOperasionalSubmission(
    val localId: String,
    val editId: String?,
    val tipe: String,
    val kategori: String,
    val jumlah: Double,
    val keterangan: String,
    val status: String // "MENGIRIM" atau "GAGAL"
)

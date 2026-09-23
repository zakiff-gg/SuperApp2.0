// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/model/GudangModels.kt
package com.sirlasirliputeri.absenssp.model

/**
 * Satu unit alat yang SEDANG DIPINJAM (belum dikembalikan), dari sheet "Gudang".
 * Karena belum ada opname/data induk barang, nama alat adalah TEKS BEBAS yang diketik
 * manual oleh peminjam saat "Ambil Alat" -- bukan referensi ke daftar alat baku.
 *
 * jumlahHari dihitung DI SERVER (Code.gs) berdasarkan tanggalAmbil vs tanggal hari ini,
 * supaya tidak perlu urus zona waktu/parsing tanggal manual di Android.
 */
data class AlatDipinjam(
    val id: String,
    val uid: String,
    val nama: String,
    val namaAlat: String,
    val tanggalAmbil: String,
    val jumlahHari: Int
)

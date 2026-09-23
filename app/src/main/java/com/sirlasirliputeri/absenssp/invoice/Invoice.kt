package com.sirlasirliputeri.absenssp.invoice

/**
 * Model data untuk satu Invoice.
 * status dihitung otomatis oleh backend (Apps Script) berdasarkan
 * total pembayaran yang sudah masuk vs nominal invoice.
 */
data class Invoice(
    val id: String,               // ID unik invoice, digenerate saat simpan (misal: INV-20260904-0001)
    val nomorInvoice: String,     // nomor invoice yang diketik manual oleh pengguna (nomor dari dokumen fisik)
    val tanggalInvoice: String,   // format yyyy-MM-dd
    val tujuan: String,           // ditujukan kepada / nama vendor
    val nominal: Double,          // nominal total invoice
    val jatuhTempo: String,       // format yyyy-MM-dd
    val keterangan: String,       // catatan opsional
    val status: String,           // "Belum Bayar" | "Sebagian" | "Lunas"
    val totalDibayar: Double,     // dihitung dari akumulasi tab PembayaranInvoice
    val linkPdf: String           // link Google Drive hasil scan
) {
    /** Sisa tagihan yang belum dibayar. */
    fun sisaTagihan(): Double = (nominal - totalDibayar).coerceAtLeast(0.0)

    /**
     * Umur invoice dalam hari, dihitung dari tanggalInvoice sampai hari ini.
     * Dihitung on-the-fly di app, tidak disimpan di sheet.
     */
    fun umurHari(): Long = DateUtil.selisihHariDariHariIni(tanggalInvoice)

    /**
     * Selisih hari terhadap jatuh tempo.
     * Positif = masih ada sisa hari sampai jatuh tempo.
     * Negatif = sudah telat sekian hari (dan berguna hanya jika status belum Lunas).
     */
    fun selisihJatuhTempo(): Long = DateUtil.selisihHariKeTanggal(jatuhTempo)

    fun isTerlambat(): Boolean = status != "Lunas" && selisihJatuhTempo() < 0
}

package com.sirlasirliputeri.absenssp.invoice

/**
 * Satu baris riwayat pembayaran untuk sebuah invoice.
 * Satu invoice bisa punya banyak Payment (dibayar bertahap/dicicil).
 */
data class Payment(
    val idInvoice: String,
    val tanggalBayar: String, // format yyyy-MM-dd
    val nominalDibayar: Double,
    val catatan: String
)

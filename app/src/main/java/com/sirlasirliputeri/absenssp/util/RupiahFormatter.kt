// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/util/RupiahFormatter.kt
package com.sirlasirliputeri.absenssp.util

/**
 * Format angka menjadi teks Rupiah dengan pemisah ribuan titik, tanpa desimal
 * (Rupiah tidak memakai satuan sen), contoh: 12500000.0 -> "Rp12.500.000".
 */
object RupiahFormatter {
    fun format(value: Double): String {
        val bulat = Math.round(value)
        val negatif = bulat < 0
        val angkaStr = Math.abs(bulat).toString()
        val hasil = groupWithDots(angkaStr)
        return (if (negatif) "-Rp" else "Rp") + hasil
    }

    /**
     * Format teks input mentah (hanya digit) menjadi tampilan berkelompok titik
     * ribuan, contoh: "12500000" -> "12.500.000". Dipakai untuk live-format
     * kolom input Pendaftaran Bon supaya pengguna tidak bingung menghitung nol.
     */
    fun formatInputDigits(digitsOnly: String): String {
        val bersih = digitsOnly.trimStart('0').ifEmpty { "" }
        if (bersih.isEmpty()) return ""
        return groupWithDots(bersih)
    }

    private fun groupWithDots(angkaStr: String): String {
        val sb = StringBuilder()
        for ((index, c) in angkaStr.reversed().withIndex()) {
            if (index != 0 && index % 3 == 0) sb.append('.')
            sb.append(c)
        }
        return sb.reverse().toString()
    }
}

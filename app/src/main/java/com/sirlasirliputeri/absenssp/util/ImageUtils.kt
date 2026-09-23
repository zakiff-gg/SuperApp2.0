// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/util/ImageUtils.kt
package com.sirlasirliputeri.absenssp.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Kompresi & encoding foto bukti Laporan Operasional. JPEG kualitas ~70% sudah cukup
 * jelas untuk bukti transaksi tapi jauh lebih hemat data dibanding kualitas penuh --
 * penting karena ini dikirim dari lokasi lapangan yang koneksinya belum tentu bagus.
 */
object ImageUtils {
    /**
     * Perkecil resolusi foto SEBELUM dikompres -- ini yang paling berpengaruh menghemat
     * kuota, karena foto dari kamera modern bisa 4000x3000px+ yang tetap besar walau
     * sudah dikompres JPEG 70% kalau resolusinya tidak diperkecil dulu.
     */
    fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1280): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun compressToJpeg(bitmap: Bitmap, quality: Int = 70): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    fun toBase64NoWrap(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

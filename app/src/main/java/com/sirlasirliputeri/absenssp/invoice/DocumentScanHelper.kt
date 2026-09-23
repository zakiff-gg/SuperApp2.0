package com.sirlasirliputeri.absenssp.invoice

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Bungkus pemakaian ML Kit Document Scanner API supaya Activity tinggal panggil
 * DocumentScanHelper.start(...) tanpa perlu tahu detail setup GmsDocumentScannerOptions.
 *
 * ML Kit Document Scanner menangani sendiri: kamera, deteksi tepi kertas otomatis,
 * crop otomatis maupun manual, reorder halaman, dan export ke PDF multi-halaman.
 * Tidak perlu izin kamera terpisah karena kamera ditangani oleh Google Play services.
 */
object DocumentScanHelper {

    private const val MAX_HALAMAN = 10 // batas jumlah lembar invoice per sesi scan

    /**
     * Panggil ini dari Activity untuk membuka UI scanner.
     * Hasilnya (PDF Uri) akan diterima lewat [launcher] yang sudah didaftarkan
     * dengan ActivityResultContracts.StartIntentSenderForResult().
     */
    fun start(context: Context, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true) // izinkan pilih dari galeri, sekaligus jadi jalur "upload file"
            .setPageLimit(MAX_HALAMAN)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL) // auto crop + manual crop + cleanup
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(context as Activity)
            .addOnSuccessListener { intentSender ->
                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                // Biarkan Activity pemanggil yang menampilkan pesan error ke user,
                // simpan pesannya lewat lastError agar bisa diambil setelah kegagalan.
                lastError = it.message ?: "Gagal membuka pemindai dokumen"
            }
    }

    var lastError: String? = null
        private set

    /** Ambil Uri PDF hasil scan dari data Intent yang diterima di callback ActivityResult. */
    fun extractPdfUri(resultCode: Int, data: android.content.Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK || data == null) return null
        val result = GmsDocumentScanningResult.fromActivityResultIntent(data)
        return result?.pdf?.uri
    }
}

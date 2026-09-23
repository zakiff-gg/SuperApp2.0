// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/nfc/NfcHelper.kt
package com.sirlasirliputeri.absenssp.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle

/**
 * Membaca UID mentah kartu NFC (e-KTP, kartu akses RFID, e-money, dll) TANPA mewajibkan
 * format NDEF. Memakai ReaderMode API bawaan Android sehingga kompatibel dengan hampir
 * semua jenis kartu ISO14443 (NfcA/NfcB) yang beredar di Indonesia.
 *
 * Termasuk sistem NFC Debounce (anti-dobel-baca) berbasis waktu di level pemanggil
 * (lihat MainActivity: pengecekan AppConstants.NFC_DEBOUNCE_MS).
 */
class NfcHelper(private val activity: Activity, private val onTagRead: (uid: String) -> Unit) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun isNfcAvailable(): Boolean = nfcAdapter != null
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun enableReaderMode() {
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        val options = Bundle()
        // Hindari delay tambahan antar pembacaan bawaan sistem; debounce kita atur sendiri.
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

        nfcAdapter?.enableReaderMode(activity, { tag: Tag ->
            val uidHex = bytesToHex(tag.id)
            onTagRead(uidHex)
        }, flags, options)
    }

    fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
}

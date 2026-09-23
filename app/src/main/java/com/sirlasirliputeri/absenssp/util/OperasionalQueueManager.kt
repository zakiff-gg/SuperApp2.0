// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/util/OperasionalQueueManager.kt
package com.sirlasirliputeri.absenssp.util

import android.content.Context
import com.sirlasirliputeri.absenssp.model.OperasionalDraft
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Antrean lokal untuk Laporan Operasional yang GAGAL terkirim (submit gagal / HP
 * sedang offline saat itu). Foto disimpan sebagai FILE TERPISAH di storage internal
 * (bukan disimpan di dalam SharedPreferences seperti absensi, karena ukurannya jauh
 * lebih besar) -- metadata laporan disimpan ringkas dalam bentuk daftar JSON yang
 * merujuk ke nama file foto tersebut.
 */
class OperasionalQueueManager(private val context: Context) {

    private val queueDir: File
        get() = File(context.filesDir, "operasional_queue").apply { mkdirs() }

    private fun metaFile(): File = File(context.filesDir, "operasional_queue_meta.json")

    @Synchronized
    fun simpanDraft(draft: OperasionalDraft, fotoBytes: ByteArray) {
        File(queueDir, draft.fotoFileName).writeBytes(fotoBytes)
        val list = getDaftarDraft().toMutableList()
        list.add(draft)
        simpanMeta(list)
    }

    @Synchronized
    fun getDaftarDraft(): List<OperasionalDraft> {
        val file = metaFile()
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                OperasionalDraft(
                    localId = obj.optString("localId"),
                    uid = obj.optString("uid"),
                    namaPengirim = obj.optString("namaPengirim"),
                    editId = obj.optString("editId", "").ifEmpty { null },
                    tipe = obj.optString("tipe"),
                    kategori = obj.optString("kategori"),
                    jumlah = obj.optDouble("jumlah", 0.0),
                    keterangan = obj.optString("keterangan"),
                    fotoFileName = obj.optString("fotoFileName"),
                    timestamp = obj.optLong("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun bacaFotoBytes(fotoFileName: String): ByteArray? {
        val file = File(queueDir, fotoFileName)
        return if (file.exists()) file.readBytes() else null
    }

    @Synchronized
    fun hapusDraft(localId: String) {
        val list = getDaftarDraft()
        val draft = list.find { it.localId == localId }
        draft?.let { File(queueDir, it.fotoFileName).delete() }
        simpanMeta(list.filterNot { it.localId == localId })
    }

    fun hasQueuedData(): Boolean = getDaftarDraft().isNotEmpty()

    private fun simpanMeta(list: List<OperasionalDraft>) {
        val arr = JSONArray()
        list.forEach { d ->
            val obj = JSONObject()
            obj.put("localId", d.localId)
            obj.put("uid", d.uid)
            obj.put("namaPengirim", d.namaPengirim)
            obj.put("editId", d.editId ?: "")
            obj.put("tipe", d.tipe)
            obj.put("kategori", d.kategori)
            obj.put("jumlah", d.jumlah)
            obj.put("keterangan", d.keterangan)
            obj.put("fotoFileName", d.fotoFileName)
            obj.put("timestamp", d.timestamp)
            arr.put(obj)
        }
        metaFile().writeText(arr.toString())
    }
}

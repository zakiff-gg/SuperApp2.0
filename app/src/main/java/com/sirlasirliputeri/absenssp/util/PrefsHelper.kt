// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/util/PrefsHelper.kt
package com.sirlasirliputeri.absenssp.util

import android.content.Context
import com.sirlasirliputeri.absenssp.invoice.Invoice
import com.sirlasirliputeri.absenssp.model.AbsensiHariIniEntry
import com.sirlasirliputeri.absenssp.model.AppConstants
import com.sirlasirliputeri.absenssp.model.Karyawan
import com.sirlasirliputeri.absenssp.model.OperasionalEntry
import com.sirlasirliputeri.absenssp.model.RiwayatOperasionalEntry
import org.json.JSONArray
import org.json.JSONObject

/**
 * Semua data yang dibutuhkan agar tap kartu bisa diproses TANPA menunggu server
 * (nama karyawan, daftar proyek, dan siapa saja yang sudah absen hari ini) disimpan
 * di sini. Server hanya dipakai untuk (1) mengisi cache ini lewat tombol Sinkronisasi
 * Data, dan (2) menerima kiriman hasil absen di belakang layar (fire-and-forget).
 */
object PrefsHelper {

    private const val PREF_DAFTAR_PROYEK = "daftar_proyek_cache"
    private const val PREF_DAFTAR_KARYAWAN = "daftar_karyawan_cache"
    private const val PREF_ABSENSI_HARI_INI = "absensi_hari_ini_cache"
    private const val PREF_ABSENSI_HARI_INI_TANGGAL = "absensi_hari_ini_tanggal"
    private const val PREF_OPERASIONAL_HARI_INI = "operasional_hari_ini_cache"
    private const val PREF_OPERASIONAL_HARI_INI_TANGGAL = "operasional_hari_ini_tanggal"
    private const val PREF_OPERASIONAL_IDENTITAS_UID = "operasional_identitas_uid"
    private const val PREF_PROFIL_IDENTITAS_UID = "profil_identitas_uid"
    private const val PREF_RIWAYAT_OPERASIONAL = "riwayat_operasional_cache"
    private const val PREF_RIWAYAT_OPERASIONAL_UID = "riwayat_operasional_cache_uid"
    private const val PREF_INVOICE_CACHE = "invoice_cache"
    private const val PREF_INVOICE_LAST_SYNC = "invoice_last_sync"
    private const val PREF_DARK_MODE = "dark_mode_pref" // "system" | "light" | "dark"

    fun getAdminPassword(context: Context): String {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(AppConstants.PREF_ADMIN_PASSWORD, AppConstants.DEFAULT_ADMIN_PASSWORD)
            ?: AppConstants.DEFAULT_ADMIN_PASSWORD
    }

    fun setAdminPassword(context: Context, password: String) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(AppConstants.PREF_ADMIN_PASSWORD, password).apply()
    }

    // ---------------- Daftar Proyek ----------------

    fun saveDaftarProyek(context: Context, daftarProyek: List<String>) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_DAFTAR_PROYEK, JSONArray(daftarProyek).toString()).apply()
    }

    fun getDaftarProyek(context: Context): List<String> {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_DAFTAR_PROYEK, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------------- Daftar Karyawan (untuk kenali kartu instan tanpa server) ----------------

    fun saveDaftarKaryawan(context: Context, daftarKaryawan: List<Karyawan>) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        daftarKaryawan.forEach { k ->
            val obj = JSONObject()
            obj.put("uid", k.uid)
            obj.put("nama", k.nama)
            obj.put("tempatKerja", k.tempatKerja)
            obj.put("posisi", k.posisi)
            obj.put("tanggalDaftar", k.tanggalDaftar)
            obj.put("upahHarian", k.upahHarian)
            obj.put("upahLembur", k.upahLembur)
            obj.put("jumlahHariMasuk", k.jumlahHariMasuk)
            obj.put("jumlahJamLembur", k.jumlahJamLembur)
            obj.put("bonDiterima", k.bonDiterima)
            obj.put("maksimalBon", k.maksimalBon)
            obj.put("adaPengajuanMenunggu", k.adaPengajuanMenunggu)
            obj.put("aksesOperasional", k.aksesOperasional)
            obj.put("saldoOperasional", k.saldoOperasional)
            arr.put(obj)
        }
        prefs.edit().putString(PREF_DAFTAR_KARYAWAN, arr.toString()).apply()
    }

    fun getDaftarKaryawan(context: Context): List<Karyawan> {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_DAFTAR_KARYAWAN, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Karyawan(
                    uid = obj.optString("uid"),
                    nama = obj.optString("nama"),
                    tempatKerja = obj.optString("tempatKerja"),
                    posisi = obj.optString("posisi"),
                    tanggalDaftar = obj.optString("tanggalDaftar"),
                    upahHarian = obj.optDouble("upahHarian", 0.0),
                    upahLembur = obj.optDouble("upahLembur", 0.0),
                    jumlahHariMasuk = obj.optDouble("jumlahHariMasuk", 0.0),
                    jumlahJamLembur = obj.optDouble("jumlahJamLembur", 0.0),
                    bonDiterima = obj.optDouble("bonDiterima", 0.0),
                    maksimalBon = obj.optDouble("maksimalBon", 0.0),
                    adaPengajuanMenunggu = obj.optBoolean("adaPengajuanMenunggu", false),
                    aksesOperasional = obj.optBoolean("aksesOperasional", false),
                    saldoOperasional = obj.optDouble("saldoOperasional", 0.0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------------- Absensi Hari Ini (untuk cek "sudah absen" & cocokkan proyek pulang) ----------------

    /**
     * Menyimpan seluruh daftar absen hari ini beserta tanggalnya. Kalau tanggal yang
     * tersimpan berbeda dari hari ini saat dibaca kembali (lihat getAbsensiHariIni),
     * cache otomatis dianggap kosong -- ini membuat counter otomatis "reset" tiap
     * hari baru tanpa perlu mengontak server.
     */
    fun saveAbsensiHariIni(context: Context, tanggal: String, daftar: List<AbsensiHariIniEntry>) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        daftar.forEach { e ->
            val obj = JSONObject()
            obj.put("uid", e.uid)
            obj.put("nama", e.nama)
            obj.put("jenis", e.jenis)
            obj.put("proyek", e.proyek)
            obj.put("keterangan", e.keterangan)
            obj.put("jam", e.jam)
            arr.put(obj)
        }
        prefs.edit()
            .putString(PREF_ABSENSI_HARI_INI, arr.toString())
            .putString(PREF_ABSENSI_HARI_INI_TANGGAL, tanggal)
            .apply()
    }

    fun getAbsensiHariIni(context: Context, tanggalHariIni: String): List<AbsensiHariIniEntry> {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val tanggalTersimpan = prefs.getString(PREF_ABSENSI_HARI_INI_TANGGAL, null)
        if (tanggalTersimpan != tanggalHariIni) return emptyList() // data lama / hari sudah berganti

        val json = prefs.getString(PREF_ABSENSI_HARI_INI, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                AbsensiHariIniEntry(
                    uid = obj.optString("uid"),
                    nama = obj.optString("nama"),
                    jenis = obj.optString("jenis"),
                    proyek = obj.optString("proyek"),
                    keterangan = obj.optString("keterangan"),
                    jam = obj.optString("jam")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------------- Operasional Hari Ini (untuk daftar "laporan saya hari ini" & cek boleh-edit) ----------------

    fun saveOperasionalHariIni(context: Context, tanggal: String, daftar: List<OperasionalEntry>) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        daftar.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("uid", e.uid)
            obj.put("tanggal", e.tanggal)
            obj.put("tipe", e.tipe)
            obj.put("kategori", e.kategori)
            obj.put("jumlah", e.jumlah)
            obj.put("keterangan", e.keterangan)
            obj.put("saldo", e.saldo)
            obj.put("namaFileBukti", e.namaFileBukti)
            obj.put("urlBukti", e.urlBukti)
            obj.put("diinputOleh", e.diinputOleh)
            arr.put(obj)
        }
        prefs.edit()
            .putString(PREF_OPERASIONAL_HARI_INI, arr.toString())
            .putString(PREF_OPERASIONAL_HARI_INI_TANGGAL, tanggal)
            .apply()
    }

    fun getOperasionalHariIni(context: Context, tanggalHariIni: String): List<OperasionalEntry> {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val tanggalTersimpan = prefs.getString(PREF_OPERASIONAL_HARI_INI_TANGGAL, null)
        if (tanggalTersimpan != tanggalHariIni) return emptyList()

        val json = prefs.getString(PREF_OPERASIONAL_HARI_INI, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                OperasionalEntry(
                    id = obj.optString("id"),
                    uid = obj.optString("uid"),
                    tanggal = obj.optString("tanggal"),
                    tipe = obj.optString("tipe"),
                    kategori = obj.optString("kategori"),
                    jumlah = obj.optDouble("jumlah", 0.0),
                    keterangan = obj.optString("keterangan"),
                    saldo = obj.optDouble("saldo", 0.0),
                    namaFileBukti = obj.optString("namaFileBukti"),
                    urlBukti = obj.optString("urlBukti"),
                    diinputOleh = obj.optString("diinputOleh")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------------- Identitas Tersimpan untuk Laporan Operasional (tanpa tap kartu) ----------------
    // HP Laporan Operasional dipegang pribadi oleh satu admin lapangan, jadi identitasnya
    // cukup dipilih SEKALI lalu disimpan di HP -- tidak perlu tap kartu tiap buka menu.

    fun saveOperasionalIdentity(context: Context, uid: String) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_OPERASIONAL_IDENTITAS_UID, uid).apply()
    }

    fun getOperasionalIdentityUid(context: Context): String? {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_OPERASIONAL_IDENTITAS_UID, null)
    }

    fun clearOperasionalIdentity(context: Context) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(PREF_OPERASIONAL_IDENTITAS_UID).apply()
    }

    // ---------------- Identitas Tersimpan untuk Profil Karyawan (tanpa tap kartu) ----------------
    // Sama seperti Laporan Operasional -- cukup pilih nama sekali dari daftar, tersimpan
    // di HP, tidak perlu tap kartu tiap buka menu. Disimpan TERPISAH dari identitas
    // Operasional karena keduanya bisa dipakai orang yang berbeda (semua karyawan boleh
    // cek Profil miliknya sendiri, tapi Laporan Operasional cuma admin lapangan tertentu).

    fun saveProfilIdentity(context: Context, uid: String) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_PROFIL_IDENTITAS_UID, uid).apply()
    }

    fun getProfilIdentityUid(context: Context): String? {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_PROFIL_IDENTITAS_UID, null)
    }

    fun clearProfilIdentity(context: Context) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(PREF_PROFIL_IDENTITAS_UID).apply()
    }

    // ---------------- Riwayat Laporan Operasional (seluruh riwayat milik SATU orang) ----------------
    // SENGAJA cuma menyimpan link foto (urlBukti), BUKAN filenya -- supaya hemat kuota,
    // foto baru diambil kalau link-nya benar-benar diketuk pengguna.

    fun saveRiwayatOperasional(context: Context, uid: String, daftar: List<RiwayatOperasionalEntry>) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        daftar.forEach { r ->
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("tanggal", r.tanggal)
            obj.put("tipe", r.tipe)
            obj.put("kategori", r.kategori)
            obj.put("jumlah", r.jumlah)
            obj.put("keterangan", r.keterangan)
            obj.put("saldo", r.saldo)
            obj.put("urlBukti", r.urlBukti)
            arr.put(obj)
        }
        prefs.edit()
            .putString(PREF_RIWAYAT_OPERASIONAL, arr.toString())
            .putString(PREF_RIWAYAT_OPERASIONAL_UID, uid)
            .apply()
    }

    /**
     * Kalau UID yang diminta beda dari pemilik cache tersimpan (mis. identitas baru
     * saja diganti tapi belum sempat Sinkronisasi Data ulang), kembalikan kosong --
     * bukan riwayat milik orang lain.
     */
    fun getRiwayatOperasional(context: Context, uid: String): List<RiwayatOperasionalEntry> {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val pemilikTersimpan = prefs.getString(PREF_RIWAYAT_OPERASIONAL_UID, null)
        if (pemilikTersimpan != uid) return emptyList()

        val json = prefs.getString(PREF_RIWAYAT_OPERASIONAL, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                RiwayatOperasionalEntry(
                    id = obj.optString("id"),
                    tanggal = obj.optString("tanggal"),
                    tipe = obj.optString("tipe"),
                    kategori = obj.optString("kategori"),
                    jumlah = obj.optDouble("jumlah", 0.0),
                    keterangan = obj.optString("keterangan"),
                    saldo = obj.optDouble("saldo", 0.0),
                    urlBukti = obj.optString("urlBukti")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------------- Cache Invoice (dipakai supaya menu Invoice bisa dibuka OFFLINE) ----------------
    // Data terakhir hasil listInvoice() dari server disimpan di sini. Layar Compose
    // invoice SELALU baca dari sini duluan (instan, tanpa nunggu server), server cuma
    // dikontak lewat tombol Sinkron atau saat submit form/pembayaran ketika online.

    fun saveInvoiceCache(context: Context, daftar: List<Invoice>) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        daftar.forEach { inv ->
            val obj = JSONObject()
            obj.put("id", inv.id)
            obj.put("nomorInvoice", inv.nomorInvoice)
            obj.put("tanggalInvoice", inv.tanggalInvoice)
            obj.put("tujuan", inv.tujuan)
            obj.put("nominal", inv.nominal)
            obj.put("jatuhTempo", inv.jatuhTempo)
            obj.put("keterangan", inv.keterangan)
            obj.put("status", inv.status)
            obj.put("totalDibayar", inv.totalDibayar)
            obj.put("linkPdf", inv.linkPdf)
            arr.put(obj)
        }
        prefs.edit()
            .putString(PREF_INVOICE_CACHE, arr.toString())
            .putLong(PREF_INVOICE_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun getInvoiceCache(context: Context): List<Invoice> {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(PREF_INVOICE_CACHE, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Invoice(
                    id = obj.optString("id"),
                    nomorInvoice = obj.optString("nomorInvoice"),
                    tanggalInvoice = obj.optString("tanggalInvoice"),
                    tujuan = obj.optString("tujuan"),
                    nominal = obj.optDouble("nominal", 0.0),
                    jatuhTempo = obj.optString("jatuhTempo"),
                    keterangan = obj.optString("keterangan"),
                    status = obj.optString("status"),
                    totalDibayar = obj.optDouble("totalDibayar", 0.0),
                    linkPdf = obj.optString("linkPdf")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getInvoiceLastSync(context: Context): Long {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(PREF_INVOICE_LAST_SYNC, 0L)
    }

    // ---------------- Preferensi Mode Gelap ----------------
    // "system" (ikut pengaturan HP, default), "light" (dipaksa terang), "dark" (dipaksa gelap).

    fun setDarkModePreference(context: Context, value: String) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_DARK_MODE, value).apply()
    }

    fun getDarkModePreference(context: Context): String {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_DARK_MODE, "system") ?: "system"
    }
}

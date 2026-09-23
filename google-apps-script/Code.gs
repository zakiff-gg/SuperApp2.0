// File Path: google-apps-script/Code.gs
// ============================================================================
// ABSEN SSP - Backend Google Apps Script Web App
// PT Sirla Sirli Puteri
//
// CARA PASANG:
// 1. Buat Google Spreadsheet baru, buka Extensions > Apps Script.
// 2. Salin seluruh isi berkas ini ke Code.gs pada editor Apps Script.
// 3. Buat 6 sheet berikut PERSIS namanya (huruf besar/kecil harus sama):
//      - "Karyawan"          : UID | Nama | Tempat Kerja | Posisi | Tanggal Daftar | Upah Harian | Upah Lembur | Akses Operasional
//      - "Absensi"           : UID | Nama | Tanggal | Jam | Jenis | Keterangan | Proyek | Jam Lembur
//      - "Proyek"            : Nama Proyek
//      - "Riwayat Karyawan"  : UID | Nama | Tempat Kerja | Posisi | Tanggal Daftar | Tanggal Diarsipkan
//      - "Bon"               : UID | Nama | Tanggal Pengajuan | Jumlah | Status | Tanggal Cair
//      - "Operasional"       : ID | UID | Tanggal | Tipe | Kategori | Jumlah | Keterangan | Saldo | NamaFileBukti | URLBukti | DiinputOleh
//      - "Gudang"            : ID | UID | Nama | NamaAlat | TanggalAmbil | TanggalKembali | Status
// 4. Format kolom UID pada sheet Karyawan, Absensi, Riwayat Karyawan, Operasional, dan
//    Gudang sebagai "Plain text" (Format > Number > Plain text) agar angka nol di depan
//    tidak hilang.
// 5. Kolom "Jam Lembur" pada sheet Absensi diisi MANUAL oleh admin (bukan otomatis oleh
//    sistem) -- sistem hanya menjumlahkan nilai yang sudah diisi untuk kebutuhan Profil
//    Karyawan & perhitungan batas maksimal bon.
// 6. Kolom "Status" pada sheet Bon diisi otomatis "Menunggu Pencairan" saat karyawan
//    mendaftar bon. Admin WAJIB mengubah manual menjadi "Cair" + mengisi Tanggal Cair
//    saat bon benar-benar dicairkan -- karena hanya bon berstatus "Cair" yang dihitung
//    sebagai pengurang batas maksimal bon berikutnya.
// 7. Kolom "Akses Operasional" pada sheet Karyawan diisi manual "Ya" oleh admin untuk
//    karyawan (admin lapangan) yang berhak membuka menu Laporan Operasional di HP.
// 8. Sheet "Operasional" TIDAK perlu diisi manual -- otomatis terisi dari aplikasi.
//    Kolom Saldo dihitung per-UID (masing-masing admin lapangan punya saldo berjalan
//    sendiri), dan foto bukti otomatis tersimpan ke folder Google Drive "Bukti Operasional".
// 9. Deploy > New deployment > Web app. Execute as: Me. Who has access: Anyone.
// 10. Salin URL Web App yang dihasilkan ke AppConstants.WEB_APP_URL pada aplikasi Android.
// 11. Sheet "Gudang" TIDAK perlu diisi manual -- otomatis terisi dari menu Gudang Alat
//     di aplikasi. Karena belum ada opname/data induk alat, kolom NamaAlat berisi TEKS
//     BEBAS yang diketik peminjam sendiri saat mengambil alat (bukan referensi ke daftar
//     alat baku). Kolom Status otomatis "Dipinjam" saat diambil, berubah "Dikembalikan"
//     + TanggalKembali terisi saat dikembalikan lewat aplikasi.
// ============================================================================

const SHEET_KARYAWAN = "Karyawan";
const SHEET_ABSENSI = "Absensi";
const SHEET_PROYEK = "Proyek";
const SHEET_RIWAYAT = "Riwayat Karyawan";
const SHEET_BON = "Bon";
const SHEET_OPERASIONAL = "Operasional";
const SHEET_GUDANG = "Gudang";

const STATUS_ALAT_DIPINJAM = "Dipinjam";
const STATUS_ALAT_DIKEMBALIKAN = "Dikembalikan";

const STATUS_BON_MENUNGGU = "Menunggu Pencairan";
const STATUS_BON_CAIR = "Cair";

const JAM_MASUK_NORMAL_MULAI = "06:00";
const JAM_MASUK_NORMAL_SELESAI = "08:00";
const JAM_PULANG_NORMAL_MULAI = "16:00";
const JAM_PULANG_NORMAL_SELESAI = "17:00";

// ------------------------------ ENTRY POINTS ------------------------------

function doGet(e) {
  try {
    const action = e.parameter.action;

    // Diakses langsung dari browser (bukan dari aplikasi Android) -- sajikan
    // halaman web pencarian riwayat karyawan. Gratis & tanpa hosting terpisah,
    // karena memakai infrastruktur Apps Script Web App yang sudah ada.
    if (!action) {
      return HtmlService.createHtmlOutputFromFile("Index")
        .setTitle("Absen SSP - Cek Riwayat Karyawan")
        .addMetaTag("viewport", "width=device-width, initial-scale=1");
    }

    let result;
    switch (action) {
      case "getProjects":
        result = getProjects_();
        break;
      case "getKaryawan":
        result = getDaftarKaryawan_();
        break;
      case "getTodayAbsensi":
        result = getTodayAbsensi_();
        break;
      case "getTodayOperasional":
        result = getTodayOperasional_();
        break;
      case "getRiwayatOperasional":
        result = getRiwayatOperasional_(e.parameter.uid);
        break;
      case "getGudangAktif":
        result = getGudangAktif_(e.parameter.uid);
        break;
      case "getTodayCount":
        result = getTodayCount_();
        break;
      case "getProfilKaryawan":
        result = getProfilKaryawan_(e.parameter.uid);
        break;
      case "searchHistory":
        result = searchHistory_(e.parameter.nama, e.parameter.tanggalMulai, e.parameter.tanggalSelesai);
        break;
      case "exportPdf":
        result = exportRekapPdf_(e.parameter.bulan, e.parameter.tahun);
        break;
      default:
        result = { status: "ERROR", pesan: "Aksi tidak dikenali: " + action };
    }
    return jsonResponse_(result);
  } catch (err) {
    return jsonResponse_({ status: "ERROR", pesan: "Kesalahan server: " + err.message });
  }
}

function doPost(e) {
  try {
    const action = e.parameter.action;
    let result;
    switch (action) {
      case "registerCard":
        result = registerCard_(e.parameter.uid, e.parameter.nama, e.parameter.tempatKerja, e.parameter.posisi);
        break;
      case "absen":
        result = prosesAbsen_(
          e.parameter.uid,
          e.parameter.jenis,
          e.parameter.proyek,
          e.parameter.timestamp,
          e.parameter.offlineSync === "true"
        );
        break;
      case "ajukanBon":
        result = ajukanBon_(e.parameter.uid, e.parameter.jumlah);
        break;
      case "operasional":
        result = prosesOperasional_(
          e.parameter.uid,
          e.parameter.tipe,
          e.parameter.kategori,
          e.parameter.jumlah,
          e.parameter.keterangan,
          e.parameter.fotoBase64,
          e.parameter.timestamp
        );
        break;
      case "editOperasional":
        result = editOperasional_(
          e.parameter.id,
          e.parameter.uid,
          e.parameter.tipe,
          e.parameter.kategori,
          e.parameter.jumlah,
          e.parameter.keterangan,
          e.parameter.fotoBase64,
          e.parameter.timestamp
        );
        break;
      case "hapusOperasional":
        result = hapusOperasional_(e.parameter.id, e.parameter.uid);
        break;
      case "ambilAlat":
        result = ambilAlat_(e.parameter.uid, e.parameter.namaAlatList);
        break;
      case "kembalikanAlat":
        result = kembalikanAlat_(e.parameter.uid, e.parameter.ids);
        break;
      default:
        result = { status: "ERROR", pesan: "Aksi tidak dikenali: " + action };
    }
    return jsonResponse_(result);
  } catch (err) {
    return jsonResponse_({ status: "ERROR", pesan: "Kesalahan server: " + err.message });
  }
}

function jsonResponse_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

// ------------------------------ HELPER SHEET ------------------------------

function getSheet_(name) {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = ss.getSheetByName(name);
  if (!sheet) throw new Error("Sheet '" + name + "' tidak ditemukan.");
  return sheet;
}

// Normalisasi UID selalu sebagai String murni untuk mencegah galat tipe data.
function normalizeUid_(uid) {
  return String(uid).trim();
}

function formatTanggal_(date) {
  return Utilities.formatDate(date, Session.getScriptTimeZone(), "yyyy-MM-dd");
}
function formatJam_(date) {
  return Utilities.formatDate(date, Session.getScriptTimeZone(), "HH:mm:ss");
}

// Google Sheets kadang otomatis mengubah sel yang berisi teks tanggal (mis. "2026-07-04")
// menjadi tipe Date asli saat dibaca kembali lewat getValues(). Kalau ini dibiarkan,
// perbandingan string biasa (dataTanggal === "2026-07-04") akan SELALU gagal walau
// tanggalnya sama, karena membandingkan objek Date dengan String. Fungsi ini menormalkan
// nilai apa pun (Date ataupun String) menjadi format "yyyy-MM-dd" yang konsisten.
function normalizeTanggal_(value) {
  if (value instanceof Date) {
    return formatTanggal_(value);
  }
  return String(value || "").trim();
}

// ------------------------------ REKAM KARTU BARU ------------------------------

function registerCard_(uidRaw, nama, tempatKerja, posisi) {
  const uid = normalizeUid_(uidRaw);
  if (!uid) return { status: "ERROR", pesan: "UID kosong." };

  const sheet = getSheet_(SHEET_KARYAWAN);
  const data = sheet.getDataRange().getValues();
  const now = new Date();
  const tanggalDaftar = formatTanggal_(now);

  let existingRowIndex = -1;
  for (let i = 1; i < data.length; i++) {
    if (normalizeUid_(data[i][0]) === uid) {
      existingRowIndex = i + 1; // 1-indexed row number pada sheet
      break;
    }
  }

  if (existingRowIndex > 0) {
    // Pindahkan data lama ke sheet Riwayat Karyawan sebelum di-update (update mechanism).
    const oldRow = sheet.getRange(existingRowIndex, 1, 1, 5).getValues()[0];
    const riwayatSheet = getSheet_(SHEET_RIWAYAT);
    riwayatSheet.appendRow([oldRow[0], oldRow[1], oldRow[2], oldRow[3], oldRow[4], tanggalDaftar]);

    sheet.getRange(existingRowIndex, 1, 1, 5).setValues([[uid, nama, tempatKerja, posisi, tanggalDaftar]]);
    return { status: "SUKSES", pesan: "Data karyawan lama diperbarui (riwayat sebelumnya diarsipkan)." };
  } else {
    sheet.appendRow([uid, nama, tempatKerja, posisi, tanggalDaftar]);
    return { status: "SUKSES", pesan: "Karyawan baru berhasil didaftarkan." };
  }
}

function cariKaryawan_(uid) {
  const sheet = getSheet_(SHEET_KARYAWAN);
  const data = sheet.getDataRange().getValues();
  for (let i = 1; i < data.length; i++) {
    if (normalizeUid_(data[i][0]) === uid) {
      return { uid: uid, nama: data[i][1], tempatKerja: data[i][2], posisi: data[i][3] };
    }
  }
  return null;
}

// ------------------------------ PROSES ABSEN ------------------------------

function prosesAbsen_(uidRaw, jenisDiminta, proyekDiminta, timestampMillis, isOfflineSync) {
  const uid = normalizeUid_(uidRaw);
  const karyawan = cariKaryawan_(uid);
  if (!karyawan) {
    return { status: "TIDAK_DIKENAL", pesan: "Kartu tidak terdaftar. Silakan hubungi admin." };
  }

  const waktuAbsen = timestampMillis ? new Date(Number(timestampMillis)) : new Date();
  const tanggal = formatTanggal_(waktuAbsen);
  const jam = formatJam_(waktuAbsen);
  const jamHHMM = Utilities.formatDate(waktuAbsen, Session.getScriptTimeZone(), "HH:mm");

  const sheet = getSheet_(SHEET_ABSENSI);
  const data = sheet.getDataRange().getValues();

  // Cek duplikasi: satu jenis absen (Berangkat/Pulang) hanya boleh sekali per hari per UID,
  // kecuali ini adalah proses sinkronisasi data offline yang memang perlu dicatat historis.
  if (!isOfflineSync) {
    for (let i = 1; i < data.length; i++) {
      if (normalizeUid_(data[i][0]) === uid && normalizeTanggal_(data[i][2]) === tanggal && data[i][4] === jenisDiminta) {
        return {
          status: "SUDAH_ABSEN",
          pesan: "Anda sudah melakukan absen " + jenisDiminta + " hari ini.",
          nama: karyawan.nama,
          jenis: jenisDiminta
        };
      }
    }
  }

  let jenis = jenisDiminta;
  let proyek = proyekDiminta || "";
  let keterangan = "";

  if (jenis === "Berangkat") {
    keterangan = hitungKeteranganBerangkat_(jamHHMM);
  } else if (jenis === "Pulang") {
    // Server mencari data "Berangkat" milik karyawan pada tanggal yang sama untuk
    // mencocokkan nama proyek secara mandiri (sesuai spesifikasi).
    proyek = cariProyekBerangkatHariIni_(uid, tanggal, data) || "Tidak Diketahui";
    keterangan = hitungKeteranganPulang_(jamHHMM);
  }

  // Kolom terakhir "Jam Lembur" sengaja dikosongkan -- diisi manual oleh admin
  // di Google Sheets, bukan otomatis oleh sistem.
  sheet.appendRow([uid, karyawan.nama, tanggal, jam, jenis, keterangan, proyek, ""]);

  return {
    status: "SUKSES",
    pesan: "Absen " + jenis + " berhasil dicatat.",
    nama: karyawan.nama,
    jenis: jenis,
    keterangan: keterangan
  };
}

function cariProyekBerangkatHariIni_(uid, tanggal, dataCache) {
  const data = dataCache || getSheet_(SHEET_ABSENSI).getDataRange().getValues();
  for (let i = 1; i < data.length; i++) {
    if (normalizeUid_(data[i][0]) === uid && normalizeTanggal_(data[i][2]) === tanggal && data[i][4] === "Berangkat") {
      return data[i][6];
    }
  }
  return null;
}

// Otomatisasi Status Keterangan sesuai aturan bisnis.
function hitungKeteranganBerangkat_(jamHHMM) {
  if (jamHHMM < JAM_MASUK_NORMAL_MULAI || jamHHMM > JAM_MASUK_NORMAL_SELESAI) {
    return "Telat (Masuk)";
  }
  return "Tepat Waktu";
}

function hitungKeteranganPulang_(jamHHMM) {
  if (jamHHMM < JAM_PULANG_NORMAL_MULAI) {
    return "Pulang - Setengah Hari";
  }
  if (jamHHMM > JAM_PULANG_NORMAL_SELESAI) {
    return "Lembur (Pulang)";
  }
  return "Tepat Waktu";
}

// ------------------------------ DAFTAR PROYEK ------------------------------

function getProjects_() {
  const sheet = getSheet_(SHEET_PROYEK);
  const data = sheet.getDataRange().getValues();
  const list = [];
  for (let i = 1; i < data.length; i++) {
    if (data[i][0]) list.push(String(data[i][0]));
  }
  return { status: "SUKSES", proyek: list };
}

// ------------------------------ SINKRONISASI KARYAWAN & ABSENSI HARI INI (UNTUK CACHE HP) ------------------------------

/**
 * Mengirim SELURUH daftar karyawan (UID, Nama, Tempat Kerja, Posisi, Upah) BESERTA
 * agregat profil (jumlah hari masuk, jam lembur, bon diterima, batas maksimal bon,
 * status pengajuan menunggu, akses & saldo Operasional) ke HP -- supaya Menu Profil
 * Karyawan & Menu Laporan Operasional bisa dibuka INSTAN & OFFLINE tanpa kontak server
 * tiap kartu ditempelkan. Dipanggil lewat tombol Sinkronisasi Data / otomatis saat
 * aplikasi pertama dibuka.
 */
/**
 * Hitung "Jumlah Hari Masuk" per-UID dengan logika: Absen Berangkat = 0.5, Absen
 * Pulang = 0.5 (kalau lengkap keduanya di tanggal yang sama = 1 hari penuh, kalau
 * cuma salah satu = 0.5 hari). Ini penting untuk karyawan yang berangkatnya
 * setengah hari. Dipakai KONSISTEN di getDaftarKaryawan_ (sinkronisasi ke HP) DAN
 * hitungProfilKaryawan_ (rumus Maksimal Bon) supaya angkanya selalu sama.
 */
function hitungHariMasukMap_(dataAbsensi) {
  const perOrangTanggal = {}; // { uid: { "2026-07-13": {berangkat, pulang} } }
  for (let i = 1; i < dataAbsensi.length; i++) {
    const uid = normalizeUid_(dataAbsensi[i][0]);
    if (!uid) continue;
    const tanggal = normalizeTanggal_(dataAbsensi[i][2]);
    const jenis = dataAbsensi[i][4];

    if (!perOrangTanggal[uid]) perOrangTanggal[uid] = {};
    if (!perOrangTanggal[uid][tanggal]) perOrangTanggal[uid][tanggal] = { berangkat: false, pulang: false };
    if (jenis === "Berangkat") perOrangTanggal[uid][tanggal].berangkat = true;
    if (jenis === "Pulang") perOrangTanggal[uid][tanggal].pulang = true;
  }

  const hasil = {};
  Object.keys(perOrangTanggal).forEach(function (uid) {
    let total = 0;
    Object.keys(perOrangTanggal[uid]).forEach(function (tgl) {
      if (perOrangTanggal[uid][tgl].berangkat) total += 0.5;
      if (perOrangTanggal[uid][tgl].pulang) total += 0.5;
    });
    hasil[uid] = total;
  });
  return hasil;
}

function getDaftarKaryawan_() {
  const sheetKaryawan = getSheet_(SHEET_KARYAWAN);
  const dataKaryawan = sheetKaryawan.getDataRange().getValues();

  // Precompute agregat dari Absensi & Bon sekali saja (efisien, bukan per-karyawan).
  const jamLemburMap = {};
  const sheetAbsensi = getSheet_(SHEET_ABSENSI);
  const dataAbsensi = sheetAbsensi.getDataRange().getValues();
  for (let i = 1; i < dataAbsensi.length; i++) {
    const uid = normalizeUid_(dataAbsensi[i][0]);
    if (!uid) continue;
    const jamLembur = Number(dataAbsensi[i][7]);
    if (!isNaN(jamLembur)) {
      jamLemburMap[uid] = (jamLemburMap[uid] || 0) + jamLembur;
    }
  }
  const hariMasukMap = hitungHariMasukMap_(dataAbsensi);

  const bonCairMap = {};
  const adaMenungguMap = {};
  const sheetBon = getSheet_(SHEET_BON);
  const dataBon = sheetBon.getDataRange().getValues();
  for (let i = 1; i < dataBon.length; i++) {
    const uid = normalizeUid_(dataBon[i][0]);
    if (!uid) continue;
    const jumlah = Number(dataBon[i][3]);
    if (dataBon[i][4] === STATUS_BON_CAIR) {
      if (!isNaN(jumlah)) bonCairMap[uid] = (bonCairMap[uid] || 0) + jumlah;
    } else if (dataBon[i][4] === STATUS_BON_MENUNGGU) {
      adaMenungguMap[uid] = true;
    }
  }

  // Saldo Operasional per-UID (masing-masing admin lapangan punya saldo berjalan
  // sendiri). Dibungkus try/catch supaya kalau sheet "Operasional" belum dibuat,
  // sinkronisasi data yang lain (karyawan/proyek/absensi) TETAP JALAN NORMAL.
  const saldoOperasionalMap = {};
  try {
    const sheetOperasional = getSheet_(SHEET_OPERASIONAL);
    const dataOperasional = sheetOperasional.getDataRange().getValues();
    for (let i = 1; i < dataOperasional.length; i++) {
      const uidOp = normalizeUid_(dataOperasional[i][1]);
      if (!uidOp) continue;
      // Selalu ditimpa -> nilai terakhir yang tersisa adalah Saldo paling baru,
      // karena baris selalu ditambahkan/diperbarui secara kronologis.
      saldoOperasionalMap[uidOp] = Number(dataOperasional[i][7]) || 0;
    }
  } catch (e) {
    // Sheet Operasional belum ada -- lewati, saldo default 0 untuk semua.
  }

  const list = [];
  for (let i = 1; i < dataKaryawan.length; i++) {
    if (!dataKaryawan[i][0]) continue;
    const uid = normalizeUid_(dataKaryawan[i][0]);
    const upahHarian = Number(dataKaryawan[i][5]) || 0;
    const upahLembur = Number(dataKaryawan[i][6]) || 0;
    const aksesOperasional = String(dataKaryawan[i][7] || "").trim().toLowerCase() === "ya";
    const jumlahHariMasuk = hariMasukMap[uid] || 0;
    const jumlahJamLembur = jamLemburMap[uid] || 0;
    const bonDiterima = bonCairMap[uid] || 0;
    const maksimalBon = (jumlahHariMasuk * upahHarian) + (jumlahJamLembur * upahLembur) - bonDiterima;

    list.push({
      uid: uid,
      nama: dataKaryawan[i][1],
      tempatKerja: dataKaryawan[i][2],
      posisi: dataKaryawan[i][3],
      tanggalDaftar: normalizeTanggal_(dataKaryawan[i][4]),
      upahHarian: upahHarian,
      upahLembur: upahLembur,
      jumlahHariMasuk: jumlahHariMasuk,
      jumlahJamLembur: jumlahJamLembur,
      bonDiterima: bonDiterima,
      maksimalBon: maksimalBon,
      adaPengajuanMenunggu: !!adaMenungguMap[uid],
      aksesOperasional: aksesOperasional,
      saldoOperasional: saldoOperasionalMap[uid] || 0
    });
  }
  return { status: "SUKSES", karyawan: list };
}

/**
 * Mengirim seluruh baris absen HARI INI (dari semua HP/perangkat) supaya HP yang baru
 * disinkronkan tahu siapa saja yang sudah absen -- penting untuk skenario multi-HP,
 * karena pengecekan "sudah absen" sekarang dilakukan di HP, bukan tanya server tiap tap.
 */
function getTodayAbsensi_() {
  const sheet = getSheet_(SHEET_ABSENSI);
  const data = sheet.getDataRange().getValues();
  const today = formatTanggal_(new Date());
  const list = [];
  for (let i = 1; i < data.length; i++) {
    if (normalizeTanggal_(data[i][2]) !== today) continue;
    list.push({
      uid: normalizeUid_(data[i][0]),
      nama: data[i][1],
      jenis: data[i][4],
      proyek: data[i][6],
      keterangan: data[i][5],
      jam: data[i][3]
    });
  }
  return { status: "SUKSES", absensi: list };
}

// ------------------------------ PROFIL KARYAWAN & BON ------------------------------

/**
 * Menghitung seluruh komponen profil karyawan: jumlah hari masuk (Absen Berangkat =
 * 0.5, Absen Pulang = 0.5, jadi karyawan yang cuma berangkat setengah hari tetap
 * terhitung wajar -- lihat hitungHariMasukMap_), total jam lembur (akumulasi kolom
 * Jam Lembur yang diisi manual admin), dan total bon yang SUDAH CAIR (bon berstatus
 * "Menunggu Pencairan" TIDAK dihitung di sini). Fungsi ini dipakai bersama oleh
 * getProfilKaryawan_ (tampilan) dan ajukanBon_ (validasi batas maksimal) supaya
 * perhitungannya selalu konsisten.
 */
function hitungProfilKaryawan_(uid) {
  const sheetKaryawan = getSheet_(SHEET_KARYAWAN);
  const dataKaryawan = sheetKaryawan.getDataRange().getValues();

  let karyawan = null;
  for (let i = 1; i < dataKaryawan.length; i++) {
    if (normalizeUid_(dataKaryawan[i][0]) === uid) {
      karyawan = {
        nama: dataKaryawan[i][1],
        upahHarian: Number(dataKaryawan[i][5]) || 0,
        upahLembur: Number(dataKaryawan[i][6]) || 0
      };
      break;
    }
  }
  if (!karyawan) return null;

  const sheetAbsensi = getSheet_(SHEET_ABSENSI);
  const dataAbsensi = sheetAbsensi.getDataRange().getValues();
  let jumlahJamLembur = 0;
  for (let i = 1; i < dataAbsensi.length; i++) {
    if (normalizeUid_(dataAbsensi[i][0]) !== uid) continue;
    const jamLembur = Number(dataAbsensi[i][7]);
    if (!isNaN(jamLembur)) jumlahJamLembur += jamLembur;
  }
  const jumlahHariMasuk = hitungHariMasukMap_(dataAbsensi)[uid] || 0;

  const sheetBon = getSheet_(SHEET_BON);
  const dataBon = sheetBon.getDataRange().getValues();
  let bonDiterima = 0;
  for (let i = 1; i < dataBon.length; i++) {
    if (normalizeUid_(dataBon[i][0]) !== uid) continue;
    if (dataBon[i][4] !== STATUS_BON_CAIR) continue; // hanya yang sudah cair
    const jumlah = Number(dataBon[i][3]);
    if (!isNaN(jumlah)) bonDiterima += jumlah;
  }

  const maksimalBon = (jumlahHariMasuk * karyawan.upahHarian) + (jumlahJamLembur * karyawan.upahLembur) - bonDiterima;

  return {
    nama: karyawan.nama,
    upahHarian: karyawan.upahHarian,
    upahLembur: karyawan.upahLembur,
    jumlahHariMasuk: jumlahHariMasuk,
    jumlahJamLembur: jumlahJamLembur,
    bonDiterima: bonDiterima,
    maksimalBon: maksimalBon
  };
}

function getProfilKaryawan_(uidRaw) {
  const uid = normalizeUid_(uidRaw);
  const profil = hitungProfilKaryawan_(uid);
  if (!profil) {
    return { status: "TIDAK_DIKENAL", pesan: "Kartu tidak terdaftar. Silakan hubungi admin." };
  }
  return {
    status: "SUKSES",
    uid: uid,
    nama: profil.nama,
    jumlahHariMasuk: profil.jumlahHariMasuk,
    upahHarian: profil.upahHarian,
    upahLembur: profil.upahLembur,
    jumlahJamLembur: profil.jumlahJamLembur,
    bonDiterima: profil.bonDiterima,
    maksimalBon: profil.maksimalBon
  };
}

/**
 * Pendaftaran Bon (BUKAN pencairan langsung) -- status awal selalu "Menunggu
 * Pencairan" karena bon yang diajukan hari ini baru cair keesokan harinya.
 * Admin WAJIB mengubah status ini manual di sheet Bon saat bon benar-benar dicairkan.
 */
function ajukanBon_(uidRaw, jumlahRaw) {
  const uid = normalizeUid_(uidRaw);
  const jumlah = Number(jumlahRaw);

  if (isNaN(jumlah) || jumlah <= 0) {
    return { status: "ERROR", pesan: "Jumlah bon tidak valid." };
  }

  const profil = hitungProfilKaryawan_(uid);
  if (!profil) {
    return { status: "TIDAK_DIKENAL", pesan: "Kartu tidak terdaftar. Silakan hubungi admin." };
  }

  // Cegah pengajuan dobel: tidak boleh ada pengajuan baru selama masih ada yang
  // berstatus "Menunggu Pencairan" untuk karyawan yang sama.
  const sheetBonCek = getSheet_(SHEET_BON);
  const dataBonCek = sheetBonCek.getDataRange().getValues();
  for (let i = 1; i < dataBonCek.length; i++) {
    if (normalizeUid_(dataBonCek[i][0]) === uid && dataBonCek[i][4] === STATUS_BON_MENUNGGU) {
      return {
        status: "ERROR",
        pesan: "Masih ada pengajuan bon yang menunggu pencairan. Tunggu hingga dicairkan admin sebelum mengajukan lagi."
      };
    }
  }

  if (jumlah > profil.maksimalBon) {
    return {
      status: "ERROR",
      pesan: "Pengajuan melebihi batas maksimal bon (" + Math.round(profil.maksimalBon) + ").",
      maksimalBon: profil.maksimalBon
    };
  }

  const sheetBon = getSheet_(SHEET_BON);
  const tanggalPengajuan = formatTanggal_(new Date());
  sheetBon.appendRow([uid, profil.nama, tanggalPengajuan, jumlah, STATUS_BON_MENUNGGU, ""]);

  return {
    status: "SUKSES",
    pesan: "Pengajuan bon berhasil dicatat, menunggu pencairan oleh admin.",
    maksimalBon: profil.maksimalBon
  };
}

// ------------------------------ LAPORAN OPERASIONAL ------------------------------

function getOrCreateBuktiOperasionalFolder_() {
  const folderName = "Bukti Operasional";
  const folders = DriveApp.getFoldersByName(folderName);
  if (folders.hasNext()) return folders.next();
  return DriveApp.createFolder(folderName);
}

/**
 * Submit Laporan Operasional baru. Memakai LockService supaya kalau dua petugas
 * submit hampir bersamaan, nomor urut bukti & saldo berjalan tidak rebutan/salah
 * hitung (race condition). Foto WAJIB dikirim sebagai base64 (tanpa prefix
 * "data:image/jpeg;base64,").
 */
function prosesOperasional_(uidRaw, tipe, kategori, jumlahRaw, keterangan, fotoBase64, timestampMillis) {
  const uid = normalizeUid_(uidRaw);
  const jumlah = Number(jumlahRaw);

  if (isNaN(jumlah) || jumlah <= 0) {
    return { status: "ERROR", pesan: "Jumlah tidak valid." };
  }
  if (tipe !== "Masuk" && tipe !== "Keluar") {
    return { status: "ERROR", pesan: "Tipe tidak valid." };
  }
  // Foto bukti bersifat OPSIONAL untuk kedua Tipe (Masuk maupun Keluar) -- tidak
  // pernah menghalangi laporan tersimpan.
  // Kategori hanya berlaku untuk Tipe "Keluar" -- kosongkan kalau Masuk.
  if (tipe === "Masuk") kategori = "";

  const karyawan = cariKaryawan_(uid);
  if (!karyawan) {
    return { status: "TIDAK_DIKENAL", pesan: "Kartu tidak terdaftar. Silakan hubungi admin." };
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const sheet = getSheet_(SHEET_OPERASIONAL);
    const data = sheet.getDataRange().getValues();

    // Hitung nomor urut bukti & saldo berjalan TERAKHIR milik UID ini (di dalam lock,
    // supaya submit yang hampir bersamaan tidak dapat nomor/saldo yang sama).
    let nomorUrutBukti = 0;
    let saldoSebelumnya = 0;
    for (let i = 1; i < data.length; i++) {
      if (normalizeUid_(data[i][1]) === uid) {
        nomorUrutBukti++;
        saldoSebelumnya = Number(data[i][7]) || 0;
      }
    }
    nomorUrutBukti++;

    const waktu = timestampMillis ? new Date(Number(timestampMillis)) : new Date();
    const tanggal = formatTanggal_(waktu);

    // Foto HANYA diproses & diupload kalau memang dikirim (Tipe "Keluar"). Untuk
    // Tipe "Masuk" tanpa foto, namaFile & urlBukti dikosongkan -- jangan sampai
    // Utilities.newBlob()/base64Decode() dipanggil dengan fotoBase64 kosong/undefined,
    // karena itu yang menyebabkan error "Unexpected error ... newBlob".
    let namaFile = "";
    let urlBukti = "";
    if (fotoBase64) {
      namaFile = uid + "_" + nomorUrutBukti + ".jpg";
      const folder = getOrCreateBuktiOperasionalFolder_();
      const blob = Utilities.newBlob(Utilities.base64Decode(fotoBase64), "image/jpeg", namaFile);
      const file = folder.createFile(blob);
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      urlBukti = file.getUrl();
    }

    const saldoBaru = saldoSebelumnya + (tipe === "Masuk" ? jumlah : -jumlah);
    const newId = "OP" + Utilities.formatString("%05d", sheet.getLastRow());

    sheet.appendRow([newId, uid, tanggal, tipe, kategori, jumlah, keterangan, saldoBaru, namaFile, urlBukti, karyawan.nama]);

    return {
      status: "SUKSES",
      pesan: "Laporan operasional berhasil dikirim.",
      id: newId,
      saldo: saldoBaru,
      namaFileBukti: namaFile,
      urlBukti: urlBukti
    };
  } finally {
    lock.releaseLock();
  }
}

/**
 * Edit Laporan Operasional yang SUDAH terkirim. Hanya boleh oleh UID pengirim asli,
 * dan hanya di hari yang sama saat laporan dikirim (divalidasi di sini juga, jangan
 * cuma percaya validasi Android). Karena Saldo bersifat akumulatif/berjalan per-UID
 * (bukan cuma milik hari itu), semua baris SESUDAH baris yang diedit milik UID yang
 * sama WAJIB dihitung ulang berantai.
 */
function editOperasional_(idRaw, uidRaw, tipe, kategori, jumlahRaw, keterangan, fotoBase64, timestampMillis) {
  const id = String(idRaw).trim();
  const uid = normalizeUid_(uidRaw);
  const jumlah = Number(jumlahRaw);

  if (isNaN(jumlah) || jumlah <= 0) {
    return { status: "ERROR", pesan: "Jumlah tidak valid." };
  }
  if (tipe !== "Masuk" && tipe !== "Keluar") {
    return { status: "ERROR", pesan: "Tipe tidak valid." };
  }

  const karyawan = cariKaryawan_(uid);
  if (!karyawan) {
    return { status: "TIDAK_DIKENAL", pesan: "Kartu tidak terdaftar. Silakan hubungi admin." };
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const sheet = getSheet_(SHEET_OPERASIONAL);
    const data = sheet.getDataRange().getValues();

    let rowIndex = -1; // 0-indexed di dalam array `data`
    for (let i = 1; i < data.length; i++) {
      if (String(data[i][0]).trim() === id) {
        rowIndex = i;
        break;
      }
    }
    if (rowIndex === -1) {
      return { status: "ERROR", pesan: "Laporan tidak ditemukan." };
    }
    if (normalizeUid_(data[rowIndex][1]) !== uid) {
      return { status: "ERROR", pesan: "Anda tidak berhak mengedit laporan ini." };
    }
    const tanggalLaporan = normalizeTanggal_(data[rowIndex][2]);
    const hariIni = formatTanggal_(new Date());
    if (tanggalLaporan !== hariIni) {
      return { status: "ERROR", pesan: "Laporan hanya bisa diedit di hari yang sama saat dikirim." };
    }

    const waktuEdit = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "dd MMM yyyy, HH:mm");
    let namaFile = data[rowIndex][8];
    let urlBukti = data[rowIndex][9];

    // Foto boleh diganti -- foto lama di Drive ditandai trash, diganti foto baru
    // dengan NAMA FILE YANG SAMA (nomor urut bukti tidak berubah saat edit).
    if (fotoBase64) {
      const folder = getOrCreateBuktiOperasionalFolder_();
      try {
        const oldFiles = folder.getFilesByName(namaFile);
        while (oldFiles.hasNext()) oldFiles.next().setTrashed(true);
      } catch (e) {
        // Foto lama tidak ditemukan -- lanjut saja, bukan masalah fatal.
      }
      const blob = Utilities.newBlob(Utilities.base64Decode(fotoBase64), "image/jpeg", namaFile);
      const file = folder.createFile(blob);
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
      urlBukti = file.getUrl();
    }

    const keteranganBaru = keterangan + " (Diedit pada " + waktuEdit + ")";

    // Perbarui field selain Saldo dulu (D:G = Tipe,Kategori,Jumlah,Keterangan;
    // I:J = NamaFileBukti,URLBukti). Kolom Saldo (H) dihitung ulang di bawah.
    sheet.getRange(rowIndex + 1, 4, 1, 4).setValues([[tipe, kategori, jumlah, keteranganBaru]]);
    sheet.getRange(rowIndex + 1, 9, 1, 2).setValues([[namaFile, urlBukti]]);

    // Hitung ulang Saldo berantai untuk baris ini dan semua baris SESUDAHNYA milik
    // UID yang sama (Saldo itu akumulatif sepanjang waktu untuk orang tsb, bukan
    // cuma di hari itu saja).
    let saldoBerjalan = 0;
    for (let i = 1; i < rowIndex; i++) {
      if (normalizeUid_(data[i][1]) === uid) {
        saldoBerjalan = Number(data[i][7]) || 0;
      }
    }

    const dataTerbaru = sheet.getDataRange().getValues(); // re-fetch, sudah termasuk perubahan di atas
    for (let i = rowIndex; i < dataTerbaru.length; i++) {
      if (normalizeUid_(dataTerbaru[i][1]) !== uid) continue;
      const tipeBaris = dataTerbaru[i][3];
      const jumlahBaris = Number(dataTerbaru[i][5]) || 0;
      saldoBerjalan += (tipeBaris === "Masuk" ? jumlahBaris : -jumlahBaris);
      sheet.getRange(i + 1, 8).setValue(saldoBerjalan);
    }

    return {
      status: "SUKSES",
      pesan: "Laporan berhasil diperbarui.",
      id: id,
      saldo: saldoBerjalan,
      namaFileBukti: namaFile,
      urlBukti: urlBukti
    };
  } finally {
    lock.releaseLock();
  }
}

/**
 * Mengirim seluruh baris Operasional HARI INI (dari semua HP/petugas) supaya HP bisa
 * menampilkan "Laporan Hari Ini milik saya" secara offline dan tahu mana yang boleh
 * diedit, tanpa perlu tanya server tiap kali menu dibuka.
 */
function getTodayOperasional_() {
  const sheet = getSheet_(SHEET_OPERASIONAL);
  const data = sheet.getDataRange().getValues();
  const today = formatTanggal_(new Date());
  const list = [];
  for (let i = 1; i < data.length; i++) {
    if (normalizeTanggal_(data[i][2]) !== today) continue;
    list.push({
      id: data[i][0],
      uid: normalizeUid_(data[i][1]),
      tanggal: normalizeTanggal_(data[i][2]),
      tipe: data[i][3],
      kategori: data[i][4],
      jumlah: Number(data[i][5]) || 0,
      keterangan: data[i][6],
      saldo: Number(data[i][7]) || 0,
      namaFileBukti: data[i][8],
      urlBukti: data[i][9],
      diinputOleh: data[i][10]
    });
  }
  return { status: "SUKSES", operasional: list };
}

/**
 * Riwayat SELURUH Laporan Operasional milik SATU UID (bukan cuma hari ini), untuk
 * menu "Riwayat Laporan" -- read-only, hanya laporan milik orang itu sendiri.
 * SENGAJA TIDAK mengirim NamaFileBukti (cuma URLBukti / link) supaya hemat kuota --
 * foto tidak ikut disinkron, baru diambil kalau link-nya benar-benar diketuk.
 */
function getRiwayatOperasional_(uidRaw) {
  const uid = normalizeUid_(uidRaw);
  const sheet = getSheet_(SHEET_OPERASIONAL);
  const data = sheet.getDataRange().getValues();
  const list = [];
  for (let i = 1; i < data.length; i++) {
    if (normalizeUid_(data[i][1]) !== uid) continue;
    list.push({
      id: data[i][0],
      tanggal: normalizeTanggal_(data[i][2]),
      tipe: data[i][3],
      kategori: data[i][4],
      jumlah: Number(data[i][5]) || 0,
      keterangan: data[i][6],
      saldo: Number(data[i][7]) || 0,
      urlBukti: data[i][9] || ""
    });
  }
  list.reverse(); // terbaru dulu (baris tersimpan kronologis menaik)
  return { status: "SUKSES", riwayat: list };
}

/**
 * Hapus Laporan Operasional yang SUDAH terkirim. Aturan sama seperti edit: hanya
 * UID pengirim asli & hanya di hari yang sama. Foto di Drive ikut dihapus (trash),
 * dan Saldo baris-baris SESUDAHNYA milik UID yang sama dihitung ulang berantai
 * (persis seperti editOperasional_, karena Saldo itu akumulatif per-orang).
 */
function hapusOperasional_(idRaw, uidRaw) {
  const id = String(idRaw).trim();
  const uid = normalizeUid_(uidRaw);

  const karyawan = cariKaryawan_(uid);
  if (!karyawan) {
    return { status: "TIDAK_DIKENAL", pesan: "Kartu tidak terdaftar. Silakan hubungi admin." };
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const sheet = getSheet_(SHEET_OPERASIONAL);
    const data = sheet.getDataRange().getValues();

    let rowIndex = -1;
    for (let i = 1; i < data.length; i++) {
      if (String(data[i][0]).trim() === id) {
        rowIndex = i;
        break;
      }
    }
    if (rowIndex === -1) {
      return { status: "ERROR", pesan: "Laporan tidak ditemukan." };
    }
    if (normalizeUid_(data[rowIndex][1]) !== uid) {
      return { status: "ERROR", pesan: "Anda tidak berhak menghapus laporan ini." };
    }
    const tanggalLaporan = normalizeTanggal_(data[rowIndex][2]);
    const hariIni = formatTanggal_(new Date());
    if (tanggalLaporan !== hariIni) {
      return { status: "ERROR", pesan: "Laporan hanya bisa dihapus di hari yang sama saat dikirim." };
    }

    // Hapus foto bukti di Drive (kalau ada).
    const namaFile = data[rowIndex][8];
    if (namaFile) {
      try {
        const folder = getOrCreateBuktiOperasionalFolder_();
        const files = folder.getFilesByName(namaFile);
        while (files.hasNext()) files.next().setTrashed(true);
      } catch (e) {
        // Foto tidak ditemukan -- lanjut saja, bukan masalah fatal.
      }
    }

    sheet.deleteRow(rowIndex + 1);

    // Hitung ulang Saldo berantai untuk semua baris SESUDAH baris yang dihapus,
    // milik UID yang sama (baris-baris tsb otomatis "naik" satu posisi setelah delete).
    const dataBaru = sheet.getDataRange().getValues();
    let saldoBerjalan = 0;
    for (let i = 1; i < rowIndex; i++) {
      if (normalizeUid_(dataBaru[i][1]) === uid) {
        saldoBerjalan = Number(dataBaru[i][7]) || 0;
      }
    }
    for (let i = rowIndex; i < dataBaru.length; i++) {
      if (normalizeUid_(dataBaru[i][1]) !== uid) continue;
      const tipeBaris = dataBaru[i][3];
      const jumlahBaris = Number(dataBaru[i][5]) || 0;
      saldoBerjalan += (tipeBaris === "Masuk" ? jumlahBaris : -jumlahBaris);
      sheet.getRange(i + 1, 8).setValue(saldoBerjalan);
    }

    return { status: "SUKSES", pesan: "Laporan berhasil dihapus.", saldo: saldoBerjalan };
  } finally {
    lock.releaseLock();
  }
}

// ------------------------------ GUDANG ALAT (Ambil/Kembalikan/Histori) ------------------------------
// Kolom sheet "Gudang": ID(0) | UID(1) | Nama(2) | NamaAlat(3) | TanggalAmbil(4) |
// TanggalKembali(5) | Status(6). NamaAlat TEKS BEBAS (belum ada opname/data induk alat),
// diketik sendiri oleh peminjam. SEMUA aksi di sini WAJIB berhasil online (tidak ada
// mekanisme antre offline seperti Absen) supaya status pinjam-kembali selalu akurat.

/**
 * Catat pengambilan SATU ATAU LEBIH alat sekaligus (satu baris per alat) atas nama
 * UID yang tap kartu. namaAlatListJson berupa string JSON array, contoh:
 * ["Bor Listrik","Meteran"].
 */
function ambilAlat_(uidRaw, namaAlatListJson) {
  const uid = normalizeUid_(uidRaw);
  const karyawan = cariKaryawan_(uid);
  if (!karyawan) {
    return { status: "TIDAK_DIKENAL", pesan: "Kartu tidak terdaftar. Silakan hubungi admin." };
  }

  let daftarAlat;
  try {
    daftarAlat = JSON.parse(namaAlatListJson || "[]")
      .map(function (s) { return String(s).trim(); })
      .filter(function (s) { return s.length > 0; });
  } catch (e) {
    return { status: "ERROR", pesan: "Data nama alat tidak valid." };
  }
  if (daftarAlat.length === 0) {
    return { status: "ERROR", pesan: "Belum ada nama alat yang diisi." };
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const sheet = getSheet_(SHEET_GUDANG);
    const tanggal = formatTanggal_(new Date());
    let nomorUrut = sheet.getLastRow(); // baris terakhir SEBELUM menambah baris baru

    const rows = daftarAlat.map(function (namaAlat) {
      nomorUrut++;
      const newId = "GD" + Utilities.formatString("%05d", nomorUrut);
      return [newId, uid, karyawan.nama, namaAlat, tanggal, "", STATUS_ALAT_DIPINJAM];
    });

    sheet.getRange(sheet.getLastRow() + 1, 1, rows.length, 7).setValues(rows);

    return {
      status: "SUKSES",
      pesan: daftarAlat.length + " alat berhasil dicatat diambil oleh " + karyawan.nama + ".",
      jumlah: daftarAlat.length
    };
  } finally {
    lock.releaseLock();
  }
}

/**
 * Catat pengembalian SATU ATAU LEBIH alat sekaligus berdasarkan ID baris. Hanya baris
 * berstatus "Dipinjam" milik UID yang sama yang diperbarui -- ID yang tidak cocok
 * (sudah dikembalikan / bukan milik UID ini) dilewati saja, dihitung di "dilewati".
 */
function kembalikanAlat_(uidRaw, idsJson) {
  const uid = normalizeUid_(uidRaw);
  const karyawan = cariKaryawan_(uid);
  if (!karyawan) {
    return { status: "TIDAK_DIKENAL", pesan: "Kartu tidak terdaftar. Silakan hubungi admin." };
  }

  let ids;
  try {
    ids = JSON.parse(idsJson || "[]").map(function (s) { return String(s).trim(); });
  } catch (e) {
    return { status: "ERROR", pesan: "Data alat yang dipilih tidak valid." };
  }
  if (ids.length === 0) {
    return { status: "ERROR", pesan: "Belum ada alat yang dipilih." };
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const sheet = getSheet_(SHEET_GUDANG);
    const data = sheet.getDataRange().getValues();
    const tanggal = formatTanggal_(new Date());

    let jumlahBerhasil = 0;
    for (let i = 1; i < data.length; i++) {
      const rowId = String(data[i][0]).trim();
      if (ids.indexOf(rowId) === -1) continue;
      if (normalizeUid_(data[i][1]) !== uid) continue;
      if (data[i][6] !== STATUS_ALAT_DIPINJAM) continue;

      sheet.getRange(i + 1, 6, 1, 2).setValues([[tanggal, STATUS_ALAT_DIKEMBALIKAN]]);
      jumlahBerhasil++;
    }

    if (jumlahBerhasil === 0) {
      return { status: "ERROR", pesan: "Alat yang dipilih tidak ditemukan atau sudah dikembalikan." };
    }

    return {
      status: "SUKSES",
      pesan: jumlahBerhasil + " alat berhasil dicatat dikembalikan.",
      jumlah: jumlahBerhasil
    };
  } finally {
    lock.releaseLock();
  }
}

/**
 * Daftar SEMUA alat yang MASIH berstatus "Dipinjam" -- dipakai untuk menu "Kembalikan
 * Alat" (difilter per uidRaw) maupun menu "Alat Sedang Dipinjam" / histori (uidRaw
 * kosong = semua orang). jumlahHari dihitung di sini (server) berdasarkan selisih
 * TanggalAmbil terhadap hari ini, supaya Android tidak perlu urus zona waktu.
 */
function getGudangAktif_(uidRaw) {
  const uidFilter = uidRaw ? normalizeUid_(uidRaw) : null;
  const sheet = getSheet_(SHEET_GUDANG);
  const data = sheet.getDataRange().getValues();
  const hariIni = new Date();
  const list = [];

  for (let i = 1; i < data.length; i++) {
    if (data[i][6] !== STATUS_ALAT_DIPINJAM) continue;
    const uidBaris = normalizeUid_(data[i][1]);
    if (uidFilter && uidBaris !== uidFilter) continue;

    const tanggalAmbil = normalizeTanggal_(data[i][4]);
    let jumlahHari = 0;
    const tglAmbilDate = new Date(tanggalAmbil + "T00:00:00");
    if (!isNaN(tglAmbilDate.getTime())) {
      const selisihMs = hariIni.setHours(0, 0, 0, 0) - tglAmbilDate.getTime();
      jumlahHari = Math.max(0, Math.round(selisihMs / (24 * 60 * 60 * 1000)));
    }

    list.push({
      id: data[i][0],
      uid: uidBaris,
      nama: data[i][2],
      namaAlat: data[i][3],
      tanggalAmbil: tanggalAmbil,
      jumlahHari: jumlahHari
    });
  }

  list.sort(function (a, b) { return b.jumlahHari - a.jumlahHari; });
  return { status: "SUKSES", aktif: list };
}

// ------------------------------ WEB PENCARIAN KARYAWAN (LANDING PAGE, TANPA LOGIN) ------------------------------

/**
 * Cari karyawan berdasarkan potongan nama (dipanggil dari halaman web lewat
 * google.script.run). TANPA LOGIN -- siapa pun yang punya link Web App bisa
 * mencari nama siapa saja (sudah dikonfirmasi ke pemilik sistem).
 */
function webCariNama(query) {
  const q = String(query || "").trim().toLowerCase();
  if (!q) return [];

  const sheet = getSheet_(SHEET_KARYAWAN);
  const data = sheet.getDataRange().getValues();
  const hasil = [];
  for (let i = 1; i < data.length; i++) {
    if (!data[i][0]) continue;
    const nama = String(data[i][1] || "");
    if (nama.toLowerCase().indexOf(q) !== -1) {
      hasil.push({
        uid: normalizeUid_(data[i][0]),
        nama: nama,
        posisi: data[i][3] || ""
      });
    }
  }
  return hasil;
}

/**
 * Riwayat lengkap + total hari kerja seorang karyawan, dengan logika KHUSUS
 * halaman web ini: Berangkat bernilai 0.5 hari, Pulang bernilai 0.5 hari.
 * Kalau dalam satu tanggal ada Berangkat DAN Pulang -> dihitung 1 hari penuh.
 * Kalau cuma Berangkat saja (tanpa Pulang) -> dihitung 0.5 hari.
 * CATATAN: ini TIDAK mengubah rumus "Jumlah Hari Masuk" yang dipakai untuk
 * batas maksimal Bon di aplikasi Android (itu tetap hitungan hari penuh
 * berbasis Berangkat) -- ini murni tampilan tambahan khusus halaman publik ini.
 */
function webRiwayatKaryawan(uidRaw) {
  try {
    const uid = normalizeUid_(uidRaw);
    const karyawan = cariKaryawan_(uid);
    if (!karyawan) {
      return { error: "Karyawan tidak ditemukan." };
    }

    const sheet = getSheet_(SHEET_ABSENSI);
    const data = sheet.getDataRange().getValues();

    const riwayat = [];
    const perTanggal = {}; // { "2026-07-13": { berangkat: true, pulang: false } }

    for (let i = 1; i < data.length; i++) {
      if (normalizeUid_(data[i][0]) !== uid) continue;
      const tanggal = normalizeTanggal_(data[i][2]);
      const jenis = data[i][4];

      if (!perTanggal[tanggal]) perTanggal[tanggal] = { berangkat: false, pulang: false };
      if (jenis === "Berangkat") perTanggal[tanggal].berangkat = true;
      if (jenis === "Pulang") perTanggal[tanggal].pulang = true;

      // Kolom Jam bisa tersimpan sebagai Date/time object di Sheets -- ubah ke teks
      // "HH:mm:ss" murni supaya aman dikirim balik ke halaman web (bukan objek Date).
      let jamTeks = data[i][3];
      if (jamTeks instanceof Date) {
        jamTeks = Utilities.formatDate(jamTeks, Session.getScriptTimeZone(), "HH:mm:ss");
      } else {
        jamTeks = String(jamTeks || "");
      }

      riwayat.push({
        tanggal: tanggal,
        jam: jamTeks,
        jenis: String(jenis || ""),
        keterangan: String(data[i][5] || ""),
        proyek: String(data[i][6] || "")
      });
    }

    let totalHariKerja = 0;
    Object.keys(perTanggal).forEach(function (tgl) {
      if (perTanggal[tgl].berangkat) totalHariKerja += 0.5;
      if (perTanggal[tgl].pulang) totalHariKerja += 0.5;
    });

    riwayat.sort(function (a, b) {
      if (a.tanggal !== b.tanggal) return a.tanggal < b.tanggal ? 1 : -1;
      return a.jam < b.jam ? 1 : -1;
    }); // terbaru dulu

    return {
      nama: karyawan.nama,
      posisi: karyawan.posisi,
      totalHariKerja: totalHariKerja,
      riwayat: riwayat
    };
  } catch (err) {
    // JANGAN biarkan exception tak tertangani -- kalau tidak, halaman web akan
    // "Memuat..." selamanya karena withFailureHandler kadang tidak konsisten
    // menangkap semua jenis error di beberapa versi runtime Apps Script.
    return { error: "Terjadi kesalahan di server: " + err.message };
  }
}

// ------------------------------ COUNTER HARI INI ------------------------------

function getTodayCount_() {
  const sheet = getSheet_(SHEET_ABSENSI);
  const data = sheet.getDataRange().getValues();
  const today = formatTanggal_(new Date());
  const uidSet = {};
  for (let i = 1; i < data.length; i++) {
    if (normalizeTanggal_(data[i][2]) === today && data[i][4] === "Berangkat") {
      uidSet[normalizeUid_(data[i][0])] = true;
    }
  }
  return { status: "SUKSES", total: Object.keys(uidSet).length };
}

// ------------------------------ RIWAYAT & PENCARIAN ------------------------------

function searchHistory_(nama, tanggalMulai, tanggalSelesai) {
  const sheet = getSheet_(SHEET_ABSENSI);
  const data = sheet.getDataRange().getValues();
  const namaLower = (nama || "").toLowerCase().trim();

  const hasil = [];
  let totalMasuk = 0;

  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    const rowNama = String(row[1] || "").toLowerCase();
    const rowTanggal = normalizeTanggal_(row[2]);

    if (namaLower && rowNama.indexOf(namaLower) === -1) continue;
    if (tanggalMulai && rowTanggal < tanggalMulai) continue;
    if (tanggalSelesai && rowTanggal > tanggalSelesai) continue;

    hasil.push({
      uid: normalizeUid_(row[0]),
      nama: row[1],
      tanggal: normalizeTanggal_(row[2]),
      jam: row[3],
      jenis: row[4],
      keterangan: row[5],
      proyek: row[6]
    });

    if (row[4] === "Berangkat") totalMasuk++;
  }

  return { status: "SUKSES", data: hasil, totalMasuk: totalMasuk };
}

// ------------------------------ REKAP BULANAN & EXPORT PDF ------------------------------

/**
 * Mengagregasi data absensi bulanan, dikelompokkan per karyawan sekaligus per proyek,
 * untuk keperluan pemisahan alokasi anggaran proyek.
 */
function buatRekapBulanIni_(bulan, tahun) {
  const sheet = getSheet_(SHEET_ABSENSI);
  const data = sheet.getDataRange().getValues();
  const bulanStr = String(bulan).padStart(2, "0");
  const prefix = tahun + "-" + bulanStr;

  // rekap[nama][proyek] = jumlah hari masuk
  const rekap = {};

  for (let i = 1; i < data.length; i++) {
    const row = data[i];
    const tanggal = normalizeTanggal_(row[2]);
    if (tanggal.indexOf(prefix) !== 0) continue;
    if (row[4] !== "Berangkat") continue;

    const nama = row[1];
    const proyek = row[6] || "Tanpa Proyek";

    if (!rekap[nama]) rekap[nama] = {};
    if (!rekap[nama][proyek]) rekap[nama][proyek] = 0;
    rekap[nama][proyek]++;
  }

  return rekap;
}

/**
 * Mengonversi hasil rekap bulanan menjadi PDF, menyimpan ke Google Drive,
 * dan mengembalikan tautan unduhan langsung (Direct Download Link).
 */
function exportRekapPdf_(bulan, tahun) {
  const rekap = buatRekapBulanIni_(bulan, tahun);

  const ssTemp = SpreadsheetApp.create("TempRekapAbsenSSP_" + bulan + "_" + tahun);
  const sheetTemp = ssTemp.getActiveSheet();
  sheetTemp.appendRow(["Rekap Absensi Absen SSP - PT Sirla Sirli Puteri", "", ""]);
  sheetTemp.appendRow(["Periode: " + bulan + "/" + tahun, "", ""]);
  sheetTemp.appendRow(["", "", ""]);
  sheetTemp.appendRow(["Nama Karyawan", "Proyek", "Total Hari Masuk"]);

  Object.keys(rekap).forEach(function (nama) {
    Object.keys(rekap[nama]).forEach(function (proyek) {
      sheetTemp.appendRow([nama, proyek, rekap[nama][proyek]]);
    });
  });

  SpreadsheetApp.flush();

  const fileId = ssTemp.getId();
  const url = "https://docs.google.com/spreadsheets/d/" + fileId + "/export?format=pdf";
  const token = ScriptApp.getOAuthToken();
  const response = UrlFetchApp.fetch(url, {
    headers: { Authorization: "Bearer " + token }
  });

  const folder = getOrCreateRekapFolder_();
  const pdfFile = folder.createFile(response.getBlob().setName("RekapAbsenSSP_" + bulan + "_" + tahun + ".pdf"));
  pdfFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);

  // Hapus spreadsheet sementara agar Drive tidak penuh dengan file bantu.
  DriveApp.getFileById(fileId).setTrashed(true);

  const directDownloadUrl = "https://drive.google.com/uc?export=download&id=" + pdfFile.getId();

  return { status: "SUKSES", pdfUrl: directDownloadUrl };
}

function getOrCreateRekapFolder_() {
  const folderName = "Rekap Absen SSP";
  const folders = DriveApp.getFoldersByName(folderName);
  if (folders.hasNext()) return folders.next();
  return DriveApp.createFolder(folderName);
}

// ------------------------------ MENU KUSTOM SPREADSHEET ------------------------------

function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu("Absen SSP")
    .addItem("Buat Rekap Bulan Ini", "menuBuatRekapBulanIni")
    .addItem("Export Rekap ke PDF", "menuExportRekapPdf")
    .addToUi();
}

function menuBuatRekapBulanIni() {
  const now = new Date();
  const bulan = now.getMonth() + 1;
  const tahun = now.getFullYear();
  const rekap = buatRekapBulanIni_(bulan, tahun);

  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let sheetRekap = ss.getSheetByName("Rekap Bulanan");
  if (sheetRekap) ss.deleteSheet(sheetRekap);
  sheetRekap = ss.insertSheet("Rekap Bulanan");

  sheetRekap.appendRow(["Nama Karyawan", "Proyek", "Total Hari Masuk"]);
  Object.keys(rekap).forEach(function (nama) {
    Object.keys(rekap[nama]).forEach(function (proyek) {
      sheetRekap.appendRow([nama, proyek, rekap[nama][proyek]]);
    });
  });

  SpreadsheetApp.getUi().alert("Rekap bulan " + bulan + "/" + tahun + " berhasil dibuat pada sheet 'Rekap Bulanan'.");
}

function menuExportRekapPdf() {
  const now = new Date();
  const bulan = now.getMonth() + 1;
  const tahun = now.getFullYear();
  const hasil = exportRekapPdf_(bulan, tahun);
  SpreadsheetApp.getUi().alert("PDF berhasil dibuat. Tautan unduhan:\n" + hasil.pdfUrl);
}

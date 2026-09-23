# Modul Invoice — Absen SSP

Modul ini menambahkan fitur baru di aplikasi Absen SSP: input berkas invoice (foto
multi-lembar dengan auto/manual crop, upload file, jadi PDF), pencatatan tujuan &
nominal, serta monitoring status pembayaran (Belum Bayar / Sebagian / Lunas) dan umur
invoice.

Modul ini **sengaja memakai Google Spreadsheet & Apps Script terpisah** dari sheet
Absen SSP yang sudah ada — jadi status Absen, Gudang Alat, dan Invoice tidak saling
tercampur.

Status: **sudah terpasang di project ini** (package, dependency, menu, dan Activity
sudah didaftarkan). Yang tersisa cuma setup Google Sheet + Apps Script di bawah ini.

---

## 1. Alur Fitur

1. Dari layar utama (Standby), tap menu **"Invoice"** → masuk **Monitoring Invoice**,
   lalu tap tombol **+** untuk buat invoice baru.
2. **Form Invoice**: isi tujuan, nominal, tanggal invoice, jatuh tempo, keterangan.
3. Setelah form valid → otomatis buka **ML Kit Document Scanner**: kamera dengan
   deteksi tepi kertas otomatis, bisa crop manual, bisa foto berkali-kali untuk banyak
   lembar, atau import dari galeri (jalur "upload file"). Hasil akhirnya 1 file PDF
   gabungan semua halaman.
4. PDF (base64) + data form dikirim ke Apps Script → PDF disimpan ke folder Google
   Drive, data invoice ditulis ke tab "Invoice" di Spreadsheet.
5. **Monitoring Invoice**: daftar semua invoice, bisa difilter per status, diurutkan
   dari yang paling lama/mendesak. Ada indikator warna (merah = telat, kuning =
   mendekati jatuh tempo, hijau = lunas, biru = aman).
6. Tap salah satu invoice → **Detail Invoice**: detail, link PDF, riwayat pembayaran
   (bisa dicicil beberapa kali), tombol "Tambah Pembayaran".

---

## 2. Setup Google Sheet Baru (khusus Invoice)

Buat Google Spreadsheet **baru** (terpisah dari sheet Absen SSP yang sudah ada), lalu
buat 2 tab dengan nama dan urutan kolom **persis** seperti ini:

### Tab `Invoice`
| A: ID | B: TanggalInvoice | C: Tujuan | D: Nominal | E: JatuhTempo | F: Keterangan | G: LinkPdf | H: TotalDibayar | I: Status |
|---|---|---|---|---|---|---|---|---|

Baris pertama adalah header (isi manual sesuai nama kolom di atas). Kolom H & I diisi
otomatis oleh script, tidak perlu diisi manual.

### Tab `PembayaranInvoice`
| A: IDInvoice | B: TanggalBayar | C: NominalDibayar | D: Catatan |
|---|---|---|---|

---

## 3. Setup Folder Google Drive

Buat folder baru di Google Drive untuk menyimpan PDF hasil scan invoice. Buka folder
tersebut, salin ID folder dari URL-nya (bagian setelah `/folders/`).

---

## 4. Deploy Apps Script

1. Di Spreadsheet invoice yang baru dibuat, buka **Extensions > Apps Script**.
2. Hapus isi default, tempel seluruh isi `InvoiceCode.gs` (disertakan terpisah dari
   folder Kotlin ini — cari file `InvoiceCode.gs`).
3. Ganti `FOLDER_ID_DRIVE` di baris paling atas dengan ID folder Drive dari langkah 3.
4. **Deploy > New deployment > Web app**:
   - Execute as: **Me**
   - Who has access: **Anyone**
5. Salin URL Web App yang dihasilkan.
6. Buka `InvoiceConfig.kt` di folder ini, ganti nilai `INVOICE_SCRIPT_URL` dengan URL
   tersebut.

---

## 5. Yang Sudah Terpasang di Project Ini

- Semua file `.kt` & layout XML sudah di-package ulang ke `com.sirlasirliputeri.absenssp.invoice`.
- 3 Activity (`InvoiceFormActivity`, `InvoiceMonitoringActivity`, `InvoiceDetailActivity`)
  sudah didaftarkan di `AndroidManifest.xml`.
- Dependency ML Kit Document Scanner, RecyclerView, CardView, dan Material Components
  sudah ditambahkan di `app/build.gradle.kts`.
- Menu **"Invoice"** sudah muncul di layar Standby, membuka `InvoiceMonitoringActivity`
  langsung.

Yang **belum** otomatis (harus dilakukan manual, lihat bagian 2–4 di atas): buat
Spreadsheet + Apps Script baru khusus Invoice, dan isi `INVOICE_SCRIPT_URL`. Tanpa ini,
menu Invoice akan terbuka tapi gagal menyimpan/memuat data.

---

## 6. Catatan Teknis

- **ML Kit Document Scanner** memerlukan Google Play services terpasang di device
  (hampir semua device Android modern punya). Model & UI scanner didownload otomatis
  lewat Play services saat pertama dipakai, jadi tidak menambah ukuran APK secara
  signifikan.
- Batas jumlah halaman per sesi scan diset 10 di `DocumentScanHelper.kt`
  (`MAX_HALAMAN`), bisa disesuaikan.
- "Umur invoice" dihitung dari **tanggal invoice** sampai hari ini, dihitung langsung
  di app (tidak disimpan di sheet). Info telat/jatuh tempo dihitung terpisah dari
  **tanggal jatuh tempo**. Kalau ternyata yang dimaksud "umur" itu justru dari jatuh
  tempo, tinggal ganti pemakaian `umurHari()` jadi `selisihJatuhTempo()` di
  `InvoiceAdapter.kt` dan `InvoiceDetailActivity.kt`.
- Status (Belum Bayar / Sebagian / Lunas) dihitung otomatis di backend setiap ada
  pembayaran baru masuk, berdasarkan akumulasi tab `PembayaranInvoice`.
- Pembayaran mendukung cicilan (banyak baris pembayaran per 1 invoice).
- Tampilan modul ini pakai View/XML klasik (Activity biasa), BUKAN Jetpack Compose
  seperti menu lain di aplikasi (Absen, Gudang Alat, Laporan Operasional) — jadi
  gaya visualnya akan terasa berbeda dari menu lain. Ini pilihan sadar demi kecepatan
  integrasi; kalau nanti ingin diseragamkan ke Compose, bisa dikerjakan menyusul.

---

## 7. Isi Folder Ini

```
invoice/
├── Invoice.kt                   # model data invoice
├── Payment.kt                   # model data 1 baris pembayaran
├── DateUtil.kt                  # hitung umur invoice & selisih jatuh tempo
├── InvoiceConfig.kt             # URL Apps Script invoice (isi manual)
├── InvoiceApiService.kt         # HTTP ke Apps Script (GET/POST)
├── DocumentScanHelper.kt        # wrapper ML Kit Document Scanner
├── InvoiceFormActivity.kt       # form input sebelum scan
├── InvoiceMonitoringActivity.kt # daftar + filter status
├── InvoiceAdapter.kt            # adapter list invoice
├── InvoiceDetailActivity.kt     # detail + riwayat + tambah pembayaran
├── PaymentAdapter.kt            # adapter riwayat pembayaran
└── README.md                    # berkas ini

app/src/main/res/layout/
├── activity_invoice_form.xml
├── activity_invoice_monitoring.xml
├── activity_invoice_detail.xml
├── item_invoice.xml
├── item_payment.xml
└── dialog_add_payment.xml
```

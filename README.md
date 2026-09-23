# Absen SSP
Sistem absensi terpusat berbasis NFC untuk PT Sirla Sirli Puteri.

## Struktur Proyek
- `app/` — Aplikasi Android (Kotlin + Jetpack Compose)
- `google-apps-script/Code.gs` — Backend Google Apps Script (Web App + Google Sheets)
- `.github/workflows/android.yml` — CI/CD build APK Debug otomatis

## Langkah Pemasangan

### 1. Backend (Google Apps Script)
1. Buat Google Spreadsheet baru.
2. Buka **Extensions > Apps Script**, tempel isi `google-apps-script/Code.gs`.
3. Buat 4 sheet dengan nama PERSIS:
   - `Karyawan` — kolom: UID | Nama | Tempat Kerja | Posisi | Tanggal Daftar
   - `Absensi` — kolom: UID | Nama | Tanggal | Jam | Jenis | Keterangan | Proyek
   - `Proyek` — kolom: Nama Proyek
   - `Riwayat Karyawan` — kolom: UID | Nama | Tempat Kerja | Posisi | Tanggal Daftar | Tanggal Diarsipkan
4. Format kolom UID sebagai **Plain text** (Format > Number > Plain text) di ketiga sheet yang memuat UID.
5. **Deploy > New deployment > Web app**. Execute as: *Me*. Who has access: *Anyone*.
6. Salin URL Web App yang muncul.

### 2. Android
1. Buka folder proyek ini di Android Studio (Giraffe/Koala ke atas).
2. Buka `app/src/main/java/.../model/AppConstants.kt`, ganti `WEB_APP_URL` dengan URL dari langkah backend di atas.
3. Ganti ikon: letakkan aset `ChatGPT Image 1 Jul 2026, 11.38.43.png` sebagai
   `app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png` (di-crop rasio 1:1, ukuran 512x512,
   objek utama dalam area aman 66% tengah sesuai pedoman Adaptive Icon Android). Ulangi untuk
   densitas mipmap lain (hdpi/xxhdpi/xxxhdpi) jika ingin kualitas optimal.
4. Sync Gradle, jalankan di HP fisik (emulator umumnya tidak mendukung NFC).

### 3. CI/CD
- Setiap push ke branch `main`/`master` akan memicu GitHub Actions membangun APK Debug.
- Unduh hasil build dari tab **Actions > (run terbaru) > Artifacts > AbsenSSP-debug-apk**.

## Catatan Keamanan
- Ganti `DEFAULT_ADMIN_PASSWORD` pada `AppConstants.kt` sebelum dipakai di lapangan.
- Web App GAS dengan akses "Anyone" dapat diakses siapa saja yang tahu URL-nya; jangan
  sebarkan URL tersebut secara publik.

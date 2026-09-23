package com.sirlasirliputeri.absenssp.invoice

/**
 * URL Web App Apps Script KHUSUS untuk modul invoice.
 * Sengaja dipisah dari Config.kt milik modul tool management,
 * karena pencatatan invoice memakai Google Spreadsheet yang berbeda.
 *
 * Cara isi:
 * 1. Buka Google Sheet baru khusus invoice, buat 2 tab: "Invoice" dan "PembayaranInvoice"
 *    (lihat README.md untuk struktur kolom lengkap).
 * 2. Buka Extensions > Apps Script di sheet tersebut, tempel isi apps-script/InvoiceCode.gs
 * 3. Deploy > New deployment > Web app > Execute as: Me, Who has access: Anyone
 * 4. Salin URL yang dihasilkan ke bawah ini.
 */
object InvoiceConfig {
    const val INVOICE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzAu3twqFvqTjPFbAQRSpABm3iHRprU6hixOJYQRH5VwkGxEkdhUNVyLiaKDgJZkG7r7w/exec"
}

// File Path: app/src/main/java/com/sirlasirliputeri/absenssp/MainActivity.kt
package com.sirlasirliputeri.absenssp

import android.Manifest
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sirlasirliputeri.absenssp.camera.CameraCaptureScreen
import com.sirlasirliputeri.absenssp.model.AbsensiHariIniEntry
import com.sirlasirliputeri.absenssp.model.AlatDipinjam
import com.sirlasirliputeri.absenssp.model.AppConstants
import com.sirlasirliputeri.absenssp.model.Karyawan
import com.sirlasirliputeri.absenssp.invoice.InvoiceMonitoringActivity
import com.sirlasirliputeri.absenssp.model.KategoriOperasional
import com.sirlasirliputeri.absenssp.model.OperasionalDraft
import com.sirlasirliputeri.absenssp.model.OperasionalEntry
import com.sirlasirliputeri.absenssp.model.PendingOperasionalSubmission
import com.sirlasirliputeri.absenssp.model.ProfilKaryawan
import com.sirlasirliputeri.absenssp.model.RiwayatOperasionalEntry
import com.sirlasirliputeri.absenssp.model.TapResult
import com.sirlasirliputeri.absenssp.model.TipeOperasional
import com.sirlasirliputeri.absenssp.network.ApiClient
import com.sirlasirliputeri.absenssp.network.NetworkStatusHelper
import com.sirlasirliputeri.absenssp.nfc.NfcHelper
import com.sirlasirliputeri.absenssp.offline.OfflineQueueManager
import com.sirlasirliputeri.absenssp.ui.screens.*
import com.sirlasirliputeri.absenssp.ui.theme.AbsenSSPTheme
import com.sirlasirliputeri.absenssp.ui.theme.GreenSuccess
import com.sirlasirliputeri.absenssp.ui.theme.RedAccent
import com.sirlasirliputeri.absenssp.util.AbsensiRules
import com.sirlasirliputeri.absenssp.util.ImageUtils
import com.sirlasirliputeri.absenssp.util.OperasionalQueueManager
import com.sirlasirliputeri.absenssp.util.PrefsHelper
import com.sirlasirliputeri.absenssp.util.HapticHelper
import com.sirlasirliputeri.absenssp.util.RupiahFormatter
import com.sirlasirliputeri.absenssp.util.SoundHelper
import com.sirlasirliputeri.absenssp.util.TimeValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen {
    STANDBY, REGISTER, HISTORY, PROFIL_PILIH_IDENTITAS, PROFIL_DETAIL,
    OPERASIONAL_PILIH_IDENTITAS, OPERASIONAL_MENU, OPERASIONAL_FORM, OPERASIONAL_RIWAYAT,
    GUDANG_MENU, GUDANG_AMBIL_TAP, GUDANG_AMBIL_FORM, GUDANG_KEMBALI_TAP, GUDANG_KEMBALI_FORM, GUDANG_HISTORI
}

private data class HasilKirimOperasional(
    val sukses: Boolean,
    val pesan: String,
    val saldoBaru: Double = 0.0,
    val entryBaru: OperasionalEntry? = null
)

private data class PendingSubmitParams(val tipe: String, val kategori: String, val jumlah: Double, val keterangan: String)

/**
 * ARSITEKTUR "LOCAL-FIRST": semua pengecekan (kartu ini milik siapa, sudah absen atau
 * belum, proyek apa saat berangkat, Profil Karyawan, dan saldo Laporan Operasional)
 * dilakukan dari CACHE DI HP (lihat PrefsHelper), bukan dengan bertanya ke server tiap
 * kali kartu ditempelkan. Server hanya dipakai untuk:
 *  1) Mengisi cache ini lewat tombol "Sinkronisasi Data" (bisa diakses langsung dari
 *     layar utama, plus otomatis berjalan sekali saat aplikasi pertama dibuka), dan
 *  2) Menerima kiriman hasil absen di belakang layar (fire-and-forget, tidak ditunggu),
 *     menyimpan pengajuan bon, dan menyimpan/mengedit/menghapus Laporan Operasional
 *     beserta foto bukti (ini semua BUTUH internet karena harus tervalidasi & tersimpan
 *     di server sebagai sumber kebenaran -- termasuk mencegah duplikasi lintas-HP).
 *
 * Menu Laporan Operasional TIDAK memakai tap kartu -- HP-nya dipegang pribadi oleh
 * satu admin lapangan, jadi identitas cukup dipilih sekali lalu disimpan di HP
 * (lihat PrefsHelper.saveOperasionalIdentity).
 */
class MainActivity : ComponentActivity() {

    private lateinit var nfcHelper: NfcHelper
    private lateinit var offlineQueue: OfflineQueueManager
    private lateinit var operasionalQueue: OperasionalQueueManager

    private var lastTapTimestamp = 0L
    private var pendingSubmitParams: PendingSubmitParams? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                showCamera.value = true
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk mengambil foto bukti.", Toast.LENGTH_SHORT).show()
            }
        }

    private val galleryPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val bitmap = try {
                    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                } catch (e: Exception) {
                    null
                }
                if (bitmap != null) capturedPhotoBitmap.value = bitmap
            }
        }

    // ------- State yang dibagikan ke Compose UI -------
    private val currentScreen = mutableStateOf(Screen.STANDBY)
    private val isOnline = mutableStateOf(true)
    private val jamValid = mutableStateOf(true)
    private val tapResult = mutableStateOf<TapResult?>(null)

    private val daftarKaryawan = mutableStateOf<List<Karyawan>>(emptyList())
    private val daftarProyek = mutableStateOf<List<String>>(emptyList())
    private val absensiHariIni = mutableStateOf<List<AbsensiHariIniEntry>>(emptyList())

    private val showProjectPicker = mutableStateOf(false)
    private var karyawanMenungguProyek: Karyawan? = null

    private val uidTerbaca = mutableStateOf("")
    private val isSubmittingRegister = mutableStateOf(false)
    private val showRegisterResultDialog = mutableStateOf(false)
    private val registerResultSuccess = mutableStateOf(false)
    private val registerResultMessage = mutableStateOf("")

    private val isLoadingHistory = mutableStateOf(false)
    private val hasilPencarian = mutableStateOf<List<com.sirlasirliputeri.absenssp.model.AbsensiRecord>>(emptyList())
    private val totalKehadiran = mutableStateOf(0)

    // Profil Karyawan & Pendaftaran Bon (sepenuhnya offline dari cache lokal)
    private val profilErrorMessage = mutableStateOf<String?>(null)
    private val profilKaryawan = mutableStateOf<ProfilKaryawan?>(null)
    private val showBonDialog = mutableStateOf(false)
    private val isSubmittingBon = mutableStateOf(false)
    private val showBonResultDialog = mutableStateOf(false)
    private val bonResultSuccess = mutableStateOf(false)
    private val bonResultMessage = mutableStateOf("")

    // Laporan Operasional
    private val showCamera = mutableStateOf(false)
    private val operasionalKaryawan = mutableStateOf<Karyawan?>(null)
    private val operasionalHariIni = mutableStateOf<List<OperasionalEntry>>(emptyList())
    private val riwayatOperasional = mutableStateOf<List<RiwayatOperasionalEntry>>(emptyList())
    private val pendingOperasionalSubmissions = mutableStateOf<List<PendingOperasionalSubmission>>(emptyList())
    private val editingOperasionalEntry = mutableStateOf<OperasionalEntry?>(null)
    private val capturedPhotoBitmap = mutableStateOf<Bitmap?>(null)
    private val showOperasionalResultDialog = mutableStateOf(false)
    private val operasionalResultSuccess = mutableStateOf(false)
    private val operasionalResultOffline = mutableStateOf(false)
    private val operasionalResultMessage = mutableStateOf("")
    private val showSaldoMinusWarning = mutableStateOf(false)
    private val previewSaldoMinusValue = mutableStateOf(0.0)

    // Gudang Alat -- SELALU ONLINE (langsung tulis ke server, tidak dicache/antre
    // offline seperti Absen) karena status pinjam-kembali harus akurat real-time.
    private val gudangKaryawan = mutableStateOf<Karyawan?>(null)
    private val formGudangAlatText = mutableStateOf("")
    private val isSubmittingGudang = mutableStateOf(false)
    private val gudangAlatUntukDikembalikan = mutableStateOf<List<AlatDipinjam>>(emptyList())
    private val isLoadingGudangKembali = mutableStateOf(false)
    private val gudangSelectedIds = mutableStateOf<Set<String>>(emptySet())
    private val gudangAlatAktifSemua = mutableStateOf<List<AlatDipinjam>>(emptyList())
    private val isLoadingGudangHistori = mutableStateOf(false)
    private val gudangHistoriError = mutableStateOf<String?>(null)
    private val showGudangResultDialog = mutableStateOf(false)
    private val gudangResultSuccess = mutableStateOf(false)
    private val gudangResultMessage = mutableStateOf("")

    // Form Operasional -- DILIFT ke sini (bukan remember di dalam composable) supaya
    // TIDAK HILANG saat layar kamera dibuka/ditutup (composable form sempat "dilepas"
    // dari komposisi saat kamera full-screen ditampilkan).
    private val formTipe = mutableStateOf(TipeOperasional.MASUK)
    private val formKategori = mutableStateOf(KategoriOperasional.DAFTAR.first())
    private val formJumlahField = mutableStateOf(TextFieldValue(""))
    private val formKeterangan = mutableStateOf("")

    private var logoTapCount = 0
    private var lastLogoTapTime = 0L
    private val showAdminPasswordDialog = mutableStateOf(false)
    private val showAdminMenuDialog = mutableStateOf(false)
    private val isDownloadingPdf = mutableStateOf(false)
    private val isSyncingData = mutableStateOf(false)
    private val adminStatusMessage = mutableStateOf<String?>(null)
    private val darkModePref = mutableStateOf("system") // "system" | "light" | "dark"
    private val showSplash = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        offlineQueue = OfflineQueueManager(applicationContext)
        operasionalQueue = OperasionalQueueManager(applicationContext)
        nfcHelper = NfcHelper(this) { uid -> onKartuTerbaca(uid) }
        darkModePref.value = com.sirlasirliputeri.absenssp.util.PrefsHelper.getDarkModePreference(applicationContext)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    showCamera.value -> showCamera.value = false
                    showSaldoMinusWarning.value -> { showSaldoMinusWarning.value = false; pendingSubmitParams = null }
                    showOperasionalResultDialog.value -> showOperasionalResultDialog.value = false
                    showBonDialog.value -> showBonDialog.value = false
                    showBonResultDialog.value -> showBonResultDialog.value = false
                    showRegisterResultDialog.value -> showRegisterResultDialog.value = false
                    showAdminMenuDialog.value -> {
                        showAdminMenuDialog.value = false
                        adminStatusMessage.value = null
                    }
                    showAdminPasswordDialog.value -> showAdminPasswordDialog.value = false
                    showProjectPicker.value -> {
                        showProjectPicker.value = false
                        karyawanMenungguProyek = null
                    }
                    currentScreen.value == Screen.OPERASIONAL_FORM -> {
                        capturedPhotoBitmap.value = null
                        currentScreen.value = Screen.OPERASIONAL_MENU
                    }
                    currentScreen.value == Screen.OPERASIONAL_RIWAYAT -> currentScreen.value = Screen.OPERASIONAL_MENU
                    showGudangResultDialog.value -> showGudangResultDialog.value = false
                    currentScreen.value == Screen.GUDANG_AMBIL_FORM -> currentScreen.value = Screen.GUDANG_MENU
                    currentScreen.value == Screen.GUDANG_KEMBALI_FORM -> currentScreen.value = Screen.GUDANG_MENU
                    currentScreen.value == Screen.GUDANG_AMBIL_TAP -> currentScreen.value = Screen.GUDANG_MENU
                    currentScreen.value == Screen.GUDANG_KEMBALI_TAP -> currentScreen.value = Screen.GUDANG_MENU
                    currentScreen.value == Screen.GUDANG_HISTORI -> currentScreen.value = Screen.GUDANG_MENU
                    currentScreen.value == Screen.GUDANG_MENU -> currentScreen.value = Screen.STANDBY
                    currentScreen.value != Screen.STANDBY -> currentScreen.value = Screen.STANDBY
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        setContent {
            val effectiveDark = com.sirlasirliputeri.absenssp.ui.theme.resolveDarkTheme(darkModePref.value)
            AbsenSSPTheme(darkTheme = effectiveDark) {
                if (showSplash.value) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1200)
                        showSplash.value = false
                    }
                    com.sirlasirliputeri.absenssp.ui.screens.SplashScreen()
                } else if (showCamera.value) {
                    CameraCaptureScreen(
                        onCaptured = { bitmap ->
                            capturedPhotoBitmap.value = bitmap
                            showCamera.value = false
                        },
                        onCancel = { showCamera.value = false }
                    )
                } else {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val totalMasukHariIni = absensiHariIni.value
                            .filter { it.jenis == AppConstants.JENIS_BERANGKAT }
                            .distinctBy { it.uid }
                            .size

                        when (currentScreen.value) {
                            Screen.STANDBY -> StandbyScreen(
                                isOnline = isOnline.value,
                                jamValid = jamValid.value,
                                totalMasukHariIni = totalMasukHariIni,
                                absensiHariIni = absensiHariIni.value,
                                showProjectPicker = showProjectPicker.value,
                                daftarProyek = daftarProyek.value,
                                tapResult = tapResult.value,
                                onProyekDipilih = { proyek -> konfirmasiAbsenBerangkat(proyek) },
                                onDismissProjectPicker = {
                                    showProjectPicker.value = false
                                    karyawanMenungguProyek = null
                                },
                                onLogoTapped = ::handleLogoTap,
                                onGotoRegister = { currentScreen.value = Screen.REGISTER },
                                onGotoHistory = { currentScreen.value = Screen.HISTORY },
                                onGotoProfil = ::bukaMenuProfil,
                                onGotoOperasional = ::bukaMenuOperasional,
                                onGotoGudang = { currentScreen.value = Screen.GUDANG_MENU },
                                onGotoInvoice = {
                                    startActivity(Intent(this@MainActivity, InvoiceMonitoringActivity::class.java))
                                },
                                onGotoSinkronisasi = ::sinkronisasiData,
                                isSyncing = isSyncingData.value,
                                isDarkMode = effectiveDark,
                                onToggleDarkMode = {
                                    val baru = if (effectiveDark) "light" else "dark"
                                    darkModePref.value = baru
                                    com.sirlasirliputeri.absenssp.util.PrefsHelper.setDarkModePreference(applicationContext, baru)
                                }
                            )
                            Screen.REGISTER -> RegisterScreen(
                                uidTerbaca = uidTerbaca.value,
                                isSubmitting = isSubmittingRegister.value,
                                onSubmit = { nama, tempatKerja, posisi -> registerKaryawan(nama, tempatKerja, posisi) },
                                onBack = {
                                    uidTerbaca.value = ""
                                    currentScreen.value = Screen.STANDBY
                                }
                            )
                            Screen.HISTORY -> HistoryScreen(
                                isLoading = isLoadingHistory.value,
                                hasilPencarian = hasilPencarian.value,
                                totalKehadiran = totalKehadiran.value,
                                onCari = { nama -> cariRiwayat(nama) },
                                onBack = { currentScreen.value = Screen.STANDBY }
                            )
                            Screen.PROFIL_PILIH_IDENTITAS -> PilihIdentitasScreen(
                                judul = "Profil Karyawan",
                                subjudul = "Pilih nama Anda -- HP ini akan mengingatnya, tidak perlu pilih lagi lain kali.",
                                daftarKaryawan = daftarKaryawan.value,
                                onPilih = { karyawan -> pilihIdentitasProfil(karyawan) },
                                onBack = { currentScreen.value = Screen.STANDBY }
                            )
                            Screen.PROFIL_DETAIL -> {
                                profilKaryawan.value?.let { profil ->
                                    ProfilKaryawanScreen(
                                        profil = profil,
                                        onAjukanBon = { showBonDialog.value = true },
                                        onGantiIdentitas = {
                                            PrefsHelper.clearProfilIdentity(this@MainActivity)
                                            profilKaryawan.value = null
                                            currentScreen.value = Screen.PROFIL_PILIH_IDENTITAS
                                        },
                                        onBack = { currentScreen.value = Screen.STANDBY }
                                    )
                                }
                            }
                            Screen.OPERASIONAL_PILIH_IDENTITAS -> PilihIdentitasScreen(
                                judul = "Siapa Anda?",
                                subjudul = "Pilih nama Anda -- HP ini akan mengingatnya, tidak perlu pilih lagi lain kali.",
                                daftarKaryawan = daftarKaryawan.value.filter { it.aksesOperasional },
                                onPilih = { karyawan -> pilihIdentitasOperasional(karyawan) },
                                onBack = { currentScreen.value = Screen.STANDBY }
                            )
                            Screen.OPERASIONAL_MENU -> {
                                operasionalKaryawan.value?.let { karyawan ->
                                    OperasionalMenuScreen(
                                        nama = karyawan.nama,
                                        saldo = karyawan.saldoOperasional,
                                        laporanHariIni = operasionalHariIni.value.filter { it.uid == karyawan.uid },
                                        pendingSubmissions = pendingOperasionalSubmissions.value,
                                        onBuatBaru = ::mulaiLaporanBaru,
                                        onEditLaporan = ::mulaiEditLaporan,
                                        onRetryPending = { pending -> retryPendingOperasional(pending) },
                                        onGantiIdentitas = {
                                            PrefsHelper.clearOperasionalIdentity(this@MainActivity)
                                            operasionalKaryawan.value = null
                                            currentScreen.value = Screen.OPERASIONAL_PILIH_IDENTITAS
                                        },
                                        onGotoRiwayat = {
                                            riwayatOperasional.value = PrefsHelper.getRiwayatOperasional(this@MainActivity, karyawan.uid)
                                            currentScreen.value = Screen.OPERASIONAL_RIWAYAT
                                        },
                                        onBack = { currentScreen.value = Screen.STANDBY }
                                    )
                                }
                            }
                            Screen.OPERASIONAL_RIWAYAT -> RiwayatOperasionalScreen(
                                daftar = riwayatOperasional.value,
                                onBukaFoto = { url ->
                                    try {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Tidak bisa membuka tautan.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onBack = { currentScreen.value = Screen.OPERASIONAL_MENU }
                            )
                            Screen.OPERASIONAL_FORM -> {
                                val editEntry = editingOperasionalEntry.value
                                OperasionalFormScreen(
                                    isEdit = editEntry != null,
                                    tipe = formTipe.value,
                                    onTipeChange = { formTipe.value = it },
                                    kategori = formKategori.value,
                                    onKategoriChange = { formKategori.value = it },
                                    jumlahField = formJumlahField.value,
                                    onJumlahFieldChange = { formJumlahField.value = it },
                                    keterangan = formKeterangan.value,
                                    onKeteranganChange = { formKeterangan.value = it },
                                    fotoBitmap = capturedPhotoBitmap.value,
                                    punyaFotoLama = editEntry?.namaFileBukti?.isNotEmpty() == true,
                                    onAmbilFotoKamera = ::onAmbilFotoKameraClicked,
                                    onPilihDariGaleri = {
                                        galleryPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onSubmit = ::handleSubmitOperasionalClicked,
                                    onHapus = if (editEntry != null) ({ hapusLaporanOperasional() }) else null,
                                    onBack = {
                                        capturedPhotoBitmap.value = null
                                        currentScreen.value = Screen.OPERASIONAL_MENU
                                    }
                                )
                            }
                            Screen.GUDANG_MENU -> GudangMenuScreen(
                                isOnline = isOnline.value,
                                onGotoAmbil = { currentScreen.value = Screen.GUDANG_AMBIL_TAP },
                                onGotoKembali = { currentScreen.value = Screen.GUDANG_KEMBALI_TAP },
                                onGotoHistori = {
                                    currentScreen.value = Screen.GUDANG_HISTORI
                                    muatGudangHistori()
                                },
                                onBack = { currentScreen.value = Screen.STANDBY }
                            )
                            Screen.GUDANG_AMBIL_TAP -> GudangTapScreen(
                                judul = "Tempelkan Kartu",
                                instruksi = "Tempelkan kartu peminjam untuk mulai mencatat pengambilan alat.",
                                warna = GreenSuccess,
                                onBack = { currentScreen.value = Screen.GUDANG_MENU }
                            )
                            Screen.GUDANG_KEMBALI_TAP -> GudangTapScreen(
                                judul = "Tempelkan Kartu",
                                instruksi = "Tempelkan kartu peminjam untuk melihat & mencatat pengembalian alat.",
                                warna = RedAccent,
                                onBack = { currentScreen.value = Screen.GUDANG_MENU }
                            )
                            Screen.GUDANG_AMBIL_FORM -> {
                                gudangKaryawan.value?.let { karyawan ->
                                    GudangAmbilFormScreen(
                                        karyawan = karyawan,
                                        daftarAlatText = formGudangAlatText.value,
                                        onDaftarAlatChange = { formGudangAlatText.value = it },
                                        isSubmitting = isSubmittingGudang.value,
                                        onSubmit = ::submitAmbilAlat,
                                        onGantiKartu = {
                                            gudangKaryawan.value = null
                                            formGudangAlatText.value = ""
                                            currentScreen.value = Screen.GUDANG_AMBIL_TAP
                                        },
                                        onBack = { currentScreen.value = Screen.GUDANG_MENU }
                                    )
                                }
                            }
                            Screen.GUDANG_KEMBALI_FORM -> {
                                gudangKaryawan.value?.let { karyawan ->
                                    GudangKembaliFormScreen(
                                        karyawan = karyawan,
                                        isLoading = isLoadingGudangKembali.value,
                                        daftarAlatAktif = gudangAlatUntukDikembalikan.value,
                                        selectedIds = gudangSelectedIds.value,
                                        onToggle = { id ->
                                            gudangSelectedIds.value = if (gudangSelectedIds.value.contains(id)) {
                                                gudangSelectedIds.value - id
                                            } else {
                                                gudangSelectedIds.value + id
                                            }
                                        },
                                        isSubmitting = isSubmittingGudang.value,
                                        onSubmit = ::submitKembalikanAlat,
                                        onGantiKartu = {
                                            gudangKaryawan.value = null
                                            gudangSelectedIds.value = emptySet()
                                            gudangAlatUntukDikembalikan.value = emptyList()
                                            currentScreen.value = Screen.GUDANG_KEMBALI_TAP
                                        },
                                        onBack = { currentScreen.value = Screen.GUDANG_MENU }
                                    )
                                }
                            }
                            Screen.GUDANG_HISTORI -> GudangHistoriScreen(
                                isLoading = isLoadingGudangHistori.value,
                                daftarAlatAktif = gudangAlatAktifSemua.value,
                                errorMessage = gudangHistoriError.value,
                                onRefresh = ::muatGudangHistori,
                                onBack = { currentScreen.value = Screen.GUDANG_MENU }
                            )
                        }

                        if (showGudangResultDialog.value) {
                            GudangResultDialog(
                                isSukses = gudangResultSuccess.value,
                                pesan = gudangResultMessage.value,
                                onDismiss = { showGudangResultDialog.value = false }
                            )
                        }

                        if (showRegisterResultDialog.value) {
                            RegisterResultDialog(
                                isSukses = registerResultSuccess.value,
                                pesan = registerResultMessage.value,
                                onDismiss = { showRegisterResultDialog.value = false }
                            )
                        }

                        if (showBonDialog.value) {
                            profilKaryawan.value?.let { profil ->
                                PendaftaranBonDialog(
                                    maksimalBon = profil.maksimalBon,
                                    isSubmitting = isSubmittingBon.value,
                                    onAjukan = { jumlah -> ajukanBon(jumlah) },
                                    onDismiss = { showBonDialog.value = false }
                                )
                            }
                        }

                        if (showBonResultDialog.value) {
                            BonResultDialog(
                                isSukses = bonResultSuccess.value,
                                pesan = bonResultMessage.value,
                                onDismiss = { showBonResultDialog.value = false }
                            )
                        }

                        if (showSaldoMinusWarning.value) {
                            SaldoMinusWarningDialog(
                                perkiraanSaldo = previewSaldoMinusValue.value,
                                onLanjutkan = {
                                    showSaldoMinusWarning.value = false
                                    pendingSubmitParams?.let { p -> submitOperasional(p.tipe, p.kategori, p.jumlah, p.keterangan) }
                                    pendingSubmitParams = null
                                },
                                onBatal = {
                                    showSaldoMinusWarning.value = false
                                    pendingSubmitParams = null
                                }
                            )
                        }

                        if (showOperasionalResultDialog.value) {
                            OperasionalResultDialog(
                                isSukses = operasionalResultSuccess.value,
                                tersimpanOffline = operasionalResultOffline.value,
                                pesan = operasionalResultMessage.value,
                                onDismiss = { showOperasionalResultDialog.value = false }
                            )
                        }

                        if (showAdminPasswordDialog.value) {
                            AdminPasswordDialog(
                                validatePassword = { input -> input == PrefsHelper.getAdminPassword(this@MainActivity) },
                                onPasswordBenar = {
                                    showAdminPasswordDialog.value = false
                                    showAdminMenuDialog.value = true
                                },
                                onDismiss = { showAdminPasswordDialog.value = false }
                            )
                        }
                        if (showAdminMenuDialog.value) {
                            AdminMenuDialog(
                                isDownloading = isDownloadingPdf.value,
                                statusMessage = adminStatusMessage.value,
                                onClearCache = ::clearCacheAdmin,
                                onDownloadPdf = { bulan, tahun -> downloadRekapPdf(bulan, tahun) },
                                onDismiss = {
                                    showAdminMenuDialog.value = false
                                    adminStatusMessage.value = null
                                }
                            )
                        }
                    }
                }
            }
        }

        muatDataAwal()
        sinkronisasiData()
    }

    override fun onResume() {
        super.onResume()
        nfcHelper.enableReaderMode()
        muatDataAwal()

        if (isOnline.value) {
            if (offlineQueue.hasQueuedData()) {
                lifecycleScope.launch { withContext(Dispatchers.IO) { offlineQueue.flushQueue() } }
            }
            if (operasionalQueue.hasQueuedData()) {
                lifecycleScope.launch { flushOperasionalQueue() }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcHelper.disableReaderMode()
    }

    // ---------------- Logika Tap Kartu (SEMUA LOKAL, TANPA NUNGGU SERVER) ----------------

    private fun onKartuTerbaca(uid: String) {
        val now = System.currentTimeMillis()
        if (now - lastTapTimestamp < AppConstants.NFC_DEBOUNCE_MS) return
        lastTapTimestamp = now

        runOnUiThread {
            when (currentScreen.value) {
                Screen.REGISTER -> uidTerbaca.value = uid
                Screen.STANDBY -> prosesTapAbsen(uid)
                Screen.GUDANG_AMBIL_TAP -> prosesTapGudangAmbil(uid)
                Screen.GUDANG_KEMBALI_TAP -> prosesTapGudangKembali(uid)
                else -> {} // Profil Karyawan & Laporan Operasional tidak lagi pakai tap kartu
            }
        }
    }

    private fun prosesTapAbsen(uid: String) {
        if (!jamValid.value) {
            SoundHelper.playError()
            HapticHelper.gagal(this)
            tapResult.value = TapResult("ERROR", "Jam Perangkat Tidak Valid! Hubungi Admin")
            sembunyikanHasilSetelahJeda()
            return
        }

        val karyawan = daftarKaryawan.value.find { it.uid == uid }
        if (karyawan == null) {
            SoundHelper.playError()
            HapticHelper.gagal(this)
            tapResult.value = TapResult("TIDAK_DIKENAL", "Kartu tidak dikenali. Hubungi admin / lakukan sinkronisasi data.")
            sembunyikanHasilSetelahJeda()
            return
        }

        val jenis = if (TimeValidator.isJamBerangkat()) AppConstants.JENIS_BERANGKAT else AppConstants.JENIS_PULANG

        val sudahAbsen = absensiHariIni.value.any { it.uid == uid && it.jenis == jenis }
        if (sudahAbsen) {
            SoundHelper.playSudahAbsen()
            HapticHelper.gagal(this)
            tapResult.value = TapResult("SUDAH_ABSEN", "Anda sudah absen $jenis hari ini.", karyawan.nama, jenis)
            sembunyikanHasilSetelahJeda()
            return
        }

        if (jenis == AppConstants.JENIS_BERANGKAT) {
            karyawanMenungguProyek = karyawan
            showProjectPicker.value = true
        } else {
            val proyek = absensiHariIni.value
                .find { it.uid == uid && it.jenis == AppConstants.JENIS_BERANGKAT }
                ?.proyek ?: "Tidak Diketahui"
            catatAbsenLokal(karyawan, AppConstants.JENIS_PULANG, proyek)
        }
    }

    private fun konfirmasiAbsenBerangkat(proyek: String) {
        val karyawan = karyawanMenungguProyek ?: return
        showProjectPicker.value = false
        karyawanMenungguProyek = null
        catatAbsenLokal(karyawan, AppConstants.JENIS_BERANGKAT, proyek)
    }

    private fun catatAbsenLokal(karyawan: Karyawan, jenis: String, proyek: String) {
        val jamHHmm = TimeValidator.currentTimeHHmm()
        val keterangan = if (jenis == AppConstants.JENIS_BERANGKAT) {
            AbsensiRules.keteranganBerangkat(jamHHmm)
        } else {
            AbsensiRules.keteranganPulang(jamHHmm)
        }
        val jamLengkap = TimeValidator.currentTimeString()
        val timestamp = System.currentTimeMillis()

        val entryBaru = AbsensiHariIniEntry(
            uid = karyawan.uid, nama = karyawan.nama, jenis = jenis, proyek = proyek, keterangan = keterangan, jam = jamLengkap
        )
        val updated = absensiHariIni.value + entryBaru
        absensiHariIni.value = updated
        PrefsHelper.saveAbsensiHariIni(this, TimeValidator.currentDateString(), updated)

        SoundHelper.playSukses()
        HapticHelper.sukses(this)
        tapResult.value = TapResult("SUKSES", "Absen $jenis berhasil dicatat.", karyawan.nama, jenis, keterangan)
        sembunyikanHasilSetelahJeda()

        kirimAbsenKeServerDiBelakangLayar(karyawan.uid, jenis, proyek, timestamp)
    }

    private fun kirimAbsenKeServerDiBelakangLayar(uid: String, jenis: String, proyek: String, timestamp: Long) {
        lifecycleScope.launch {
            if (!NetworkStatusHelper.isOnline(this@MainActivity)) {
                withContext(Dispatchers.IO) { offlineQueue.enqueue(uid, jenis, proyek, timestamp) }
                return@launch
            }
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.post(
                        mapOf("action" to "absen", "uid" to uid, "jenis" to jenis, "proyek" to proyek, "timestamp" to timestamp.toString())
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.IO) { offlineQueue.enqueue(uid, jenis, proyek, timestamp) }
            }
        }
    }

    private fun sembunyikanHasilSetelahJeda() {
        lifecycleScope.launch {
            kotlinx.coroutines.delay(AppConstants.RESULT_DISPLAY_MS)
            tapResult.value = null
        }
    }

    // ---------------- Rekam Kartu Baru ----------------

    private fun registerKaryawan(nama: String, tempatKerja: String, posisi: String) {
        val uid = uidTerbaca.value
        if (uid.isEmpty()) return
        isSubmittingRegister.value = true

        lifecycleScope.launch {
            try {
                val hasil = withContext(Dispatchers.IO) {
                    ApiClient.post(mapOf("action" to "registerCard", "uid" to uid, "nama" to nama, "tempatKerja" to tempatKerja, "posisi" to posisi))
                }
                val sukses = hasil.optString("status") == "SUKSES"
                registerResultSuccess.value = sukses
                registerResultMessage.value = hasil.optString("pesan", if (sukses) "Data berhasil disimpan." else "Gagal menyimpan data.")
                showRegisterResultDialog.value = true

                if (sukses) {
                    SoundHelper.playSukses()
                    uidTerbaca.value = ""
                    val updated = daftarKaryawan.value.filterNot { it.uid == uid } +
                        Karyawan(uid, nama, tempatKerja, posisi, TimeValidator.currentDateString())
                    daftarKaryawan.value = updated
                    withContext(Dispatchers.IO) { PrefsHelper.saveDaftarKaryawan(this@MainActivity, updated) }
                } else {
                    SoundHelper.playError()
                }
            } catch (e: Exception) {
                registerResultSuccess.value = false
                registerResultMessage.value = "Gagal menyimpan data: ${e.javaClass.simpleName} - ${e.message}"
                showRegisterResultDialog.value = true
                SoundHelper.playError()
            } finally {
                isSubmittingRegister.value = false
            }
        }
    }

    // ---------------- Riwayat & Pencarian ----------------

    private fun cariRiwayat(nama: String) {
        isLoadingHistory.value = true
        lifecycleScope.launch {
            try {
                val hasil = withContext(Dispatchers.IO) { ApiClient.get("searchHistory", mapOf("nama" to nama)) }
                val arr = hasil.optJSONArray("data")
                val list = mutableListOf<com.sirlasirliputeri.absenssp.model.AbsensiRecord>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            com.sirlasirliputeri.absenssp.model.AbsensiRecord(
                                uid = obj.optString("uid"), nama = obj.optString("nama"), tanggal = obj.optString("tanggal"),
                                jam = obj.optString("jam"), jenis = obj.optString("jenis"), keterangan = obj.optString("keterangan"),
                                proyek = obj.optString("proyek")
                            )
                        )
                    }
                }
                hasilPencarian.value = list
                totalKehadiran.value = hasil.optInt("totalMasuk", 0)
            } catch (e: Exception) {
                hasilPencarian.value = emptyList()
                totalKehadiran.value = 0
            } finally {
                isLoadingHistory.value = false
            }
        }
    }

    // ---------------- Profil Karyawan & Pendaftaran Bon (OFFLINE, dari cache lokal) ----------------

    /**
     * Buka menu Profil: kalau identitas sudah tersimpan -> langsung tampil profilnya.
     * Kalau belum -> tampilkan layar pilih nama (BUKAN tap kartu lagi).
     */
    private fun bukaMenuProfil() {
        val savedUid = PrefsHelper.getProfilIdentityUid(this)
        val karyawan = savedUid?.let { uid -> daftarKaryawan.value.find { it.uid == uid } }
        if (karyawan != null) {
            muatProfilDariKaryawan(karyawan)
        } else {
            currentScreen.value = Screen.PROFIL_PILIH_IDENTITAS
        }
    }

    private fun pilihIdentitasProfil(karyawan: Karyawan) {
        PrefsHelper.saveProfilIdentity(this, karyawan.uid)
        muatProfilDariKaryawan(karyawan)
    }

    private fun muatProfilDariKaryawan(karyawan: Karyawan) {
        profilErrorMessage.value = null
        profilKaryawan.value = ProfilKaryawan(
            uid = karyawan.uid, nama = karyawan.nama, jumlahHariMasuk = karyawan.jumlahHariMasuk,
            upahHarian = karyawan.upahHarian, upahLembur = karyawan.upahLembur, jumlahJamLembur = karyawan.jumlahJamLembur,
            bonDiterima = karyawan.bonDiterima, maksimalBon = karyawan.maksimalBon, adaPengajuanMenunggu = karyawan.adaPengajuanMenunggu
        )
        currentScreen.value = Screen.PROFIL_DETAIL
    }

    private fun ajukanBon(jumlah: Double) {
        val uid = profilKaryawan.value?.uid ?: return
        isSubmittingBon.value = true
        lifecycleScope.launch {
            try {
                val hasil = withContext(Dispatchers.IO) {
                    ApiClient.post(mapOf("action" to "ajukanBon", "uid" to uid, "jumlah" to jumlah.toString()))
                }
                val sukses = hasil.optString("status") == "SUKSES"
                bonResultSuccess.value = sukses
                bonResultMessage.value = hasil.optString("pesan", if (sukses) "Pengajuan berhasil dicatat." else "Pengajuan gagal.")

                if (sukses) {
                    SoundHelper.playSukses()
                    showBonDialog.value = false
                    profilKaryawan.value = profilKaryawan.value?.copy(adaPengajuanMenunggu = true)
                    val updatedKaryawan = daftarKaryawan.value.map { if (it.uid == uid) it.copy(adaPengajuanMenunggu = true) else it }
                    daftarKaryawan.value = updatedKaryawan
                    withContext(Dispatchers.IO) { PrefsHelper.saveDaftarKaryawan(this@MainActivity, updatedKaryawan) }
                } else {
                    SoundHelper.playError()
                }
                showBonResultDialog.value = true
            } catch (e: Exception) {
                bonResultSuccess.value = false
                bonResultMessage.value = "Gagal mengirim pengajuan. Periksa koneksi internet."
                showBonResultDialog.value = true
                SoundHelper.playError()
            } finally {
                isSubmittingBon.value = false
            }
        }
    }

    // ---------------- Laporan Operasional ----------------

    /**
     * Buka menu Operasional: kalau identitas sudah tersimpan & masih berakses -> langsung
     * ke saldo. Kalau belum -> tampilkan layar pilih identitas (BUKAN tap kartu lagi,
     * karena HP ini dipegang pribadi oleh satu admin lapangan).
     */
    private fun bukaMenuOperasional() {
        val savedUid = PrefsHelper.getOperasionalIdentityUid(this)
        val karyawan = savedUid?.let { uid -> daftarKaryawan.value.find { it.uid == uid && it.aksesOperasional } }
        if (karyawan != null) {
            operasionalKaryawan.value = karyawan
            currentScreen.value = Screen.OPERASIONAL_MENU
        } else {
            currentScreen.value = Screen.OPERASIONAL_PILIH_IDENTITAS
        }
    }

    private fun pilihIdentitasOperasional(karyawan: Karyawan) {
        PrefsHelper.saveOperasionalIdentity(this, karyawan.uid)
        operasionalKaryawan.value = karyawan
        currentScreen.value = Screen.OPERASIONAL_MENU
    }

    private fun mulaiLaporanBaru() {
        editingOperasionalEntry.value = null
        capturedPhotoBitmap.value = null
        formTipe.value = TipeOperasional.MASUK
        formKategori.value = KategoriOperasional.DAFTAR.first()
        formJumlahField.value = TextFieldValue("")
        formKeterangan.value = ""
        currentScreen.value = Screen.OPERASIONAL_FORM
    }

    private fun mulaiEditLaporan(entry: OperasionalEntry) {
        editingOperasionalEntry.value = entry
        capturedPhotoBitmap.value = null
        formTipe.value = entry.tipe
        formKategori.value = entry.kategori
        val digitJumlah = entry.jumlah.toLong().toString()
        val formatted = RupiahFormatter.formatInputDigits(digitJumlah)
        formJumlahField.value = TextFieldValue(formatted, selection = TextRange(formatted.length))
        formKeterangan.value = entry.keterangan
        currentScreen.value = Screen.OPERASIONAL_FORM
    }

    private fun onAmbilFotoKameraClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCamera.value = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Dipanggil saat tombol Kirim/Simpan ditekan. Mengecek dulu perkiraan Saldo setelah
     * transaksi ini -- kalau akan jadi minus, tampilkan peringatan dulu sebelum benar-benar
     * mengirim (pengguna tetap boleh melanjutkan).
     */
    private fun handleSubmitOperasionalClicked() {
        val karyawan = operasionalKaryawan.value ?: return
        val tipe = formTipe.value
        // Kategori hanya berlaku untuk Tipe "Keluar" -- kosongkan kalau Masuk.
        val kategori = if (tipe == TipeOperasional.MASUK) "" else formKategori.value
        val jumlah = formJumlahField.value.text.replace(".", "").toDoubleOrNull() ?: 0.0
        val keterangan = formKeterangan.value
        val editEntry = editingOperasionalEntry.value

        val perkiraanSaldo = if (editEntry != null) {
            val deltaLama = if (editEntry.tipe == TipeOperasional.MASUK) editEntry.jumlah else -editEntry.jumlah
            val deltaBaru = if (tipe == TipeOperasional.MASUK) jumlah else -jumlah
            karyawan.saldoOperasional - deltaLama + deltaBaru
        } else {
            karyawan.saldoOperasional + (if (tipe == TipeOperasional.MASUK) jumlah else -jumlah)
        }

        if (perkiraanSaldo < 0) {
            pendingSubmitParams = PendingSubmitParams(tipe, kategori, jumlah, keterangan)
            previewSaldoMinusValue.value = perkiraanSaldo
            showSaldoMinusWarning.value = true
        } else {
            submitOperasional(tipe, kategori, jumlah, keterangan)
        }
    }

    /**
     * Kirim laporan di BACKGROUND: langsung kembali ke menu (bisa langsung isi laporan
     * berikutnya), status "Mengirim..." muncul di daftar. Draft SELALU disimpan dulu ke
     * antrean lokal sebelum dicoba kirim (aman kalau app ke-kill di tengah proses).
     */
    private fun submitOperasional(tipe: String, kategori: String, jumlah: Double, keterangan: String) {
        val karyawan = operasionalKaryawan.value ?: return
        val editEntry = editingOperasionalEntry.value
        val fotoBitmap = capturedPhotoBitmap.value
        val localId = "OP${System.currentTimeMillis()}"
        val timestamp = System.currentTimeMillis()

        pendingOperasionalSubmissions.value = pendingOperasionalSubmissions.value +
            PendingOperasionalSubmission(localId, editEntry?.id, tipe, kategori, jumlah, keterangan, "MENGIRIM")

        capturedPhotoBitmap.value = null
        editingOperasionalEntry.value = null
        currentScreen.value = Screen.OPERASIONAL_MENU

        lifecycleScope.launch {
            val fotoBytes = fotoBitmap?.let { ImageUtils.compressToJpeg(ImageUtils.resizeBitmap(it), 70) }
            withContext(Dispatchers.IO) {
                operasionalQueue.simpanDraft(
                    OperasionalDraft(
                        localId = localId, uid = karyawan.uid, namaPengirim = karyawan.nama, editId = editEntry?.id,
                        tipe = tipe, kategori = kategori, jumlah = jumlah, keterangan = keterangan,
                        fotoFileName = "$localId.jpg", timestamp = timestamp
                    ),
                    fotoBytes ?: ByteArray(0)
                )
            }
            cobaKirimDraftOperasional(localId)
        }
    }

    private fun retryPendingOperasional(pending: PendingOperasionalSubmission) {
        pendingOperasionalSubmissions.value = pendingOperasionalSubmissions.value.map {
            if (it.localId == pending.localId) it.copy(status = "MENGIRIM") else it
        }
        lifecycleScope.launch { cobaKirimDraftOperasional(pending.localId) }
    }

    private suspend fun cobaKirimDraftOperasional(localId: String) {
        val draft = operasionalQueue.getDaftarDraft().find { it.localId == localId } ?: run {
            pendingOperasionalSubmissions.value = pendingOperasionalSubmissions.value.filterNot { it.localId == localId }
            return
        }

        if (!NetworkStatusHelper.isOnline(this@MainActivity)) {
            tandaiPendingGagal(localId)
            return
        }

        val hasil = kirimSatuDraftOperasional(draft)
        if (hasil.sukses) {
            terapkanHasilSuksesOperasional(draft, hasil)
            pendingOperasionalSubmissions.value = pendingOperasionalSubmissions.value.filterNot { it.localId == localId }
            selesaikanNotifikasi(true, false, hasil.pesan)
        } else {
            tandaiPendingGagal(localId)
            selesaikanNotifikasi(false, false, hasil.pesan)
        }
    }

    private suspend fun kirimSatuDraftOperasional(draft: OperasionalDraft): HasilKirimOperasional {
        return try {
            val fotoBytes = operasionalQueue.bacaFotoBytes(draft.fotoFileName)
            val params = mutableMapOf(
                "action" to if (draft.editId != null) "editOperasional" else "operasional",
                "uid" to draft.uid, "tipe" to draft.tipe, "kategori" to draft.kategori,
                "jumlah" to draft.jumlah.toString(), "keterangan" to draft.keterangan, "timestamp" to draft.timestamp.toString()
            )
            if (draft.editId != null) params["id"] = draft.editId
            if (fotoBytes != null && fotoBytes.isNotEmpty()) params["fotoBase64"] = ImageUtils.toBase64NoWrap(fotoBytes)

            val hasil = withContext(Dispatchers.IO) { ApiClient.post(params, ApiClient.TIMEOUT_UPLOAD_MS) }
            val sukses = hasil.optString("status") == "SUKSES"
            if (sukses) {
                withContext(Dispatchers.IO) { operasionalQueue.hapusDraft(draft.localId) }
                val saldoBaru = hasil.optDouble("saldo", 0.0)
                val entryBaru = OperasionalEntry(
                    id = hasil.optString("id", draft.editId ?: ""), uid = draft.uid, tanggal = TimeValidator.currentDateString(),
                    tipe = draft.tipe, kategori = draft.kategori, jumlah = draft.jumlah, keterangan = draft.keterangan,
                    saldo = saldoBaru, namaFileBukti = hasil.optString("namaFileBukti"), urlBukti = hasil.optString("urlBukti"),
                    diinputOleh = draft.namaPengirim
                )
                HasilKirimOperasional(true, hasil.optString("pesan", "Laporan berhasil disimpan."), saldoBaru, entryBaru)
            } else {
                HasilKirimOperasional(false, hasil.optString("pesan", "Laporan gagal disimpan."))
            }
        } catch (e: Exception) {
            HasilKirimOperasional(false, "Belum berhasil terkirim, tersimpan di HP dan akan dicoba lagi otomatis.")
        }
    }

    private suspend fun terapkanHasilSuksesOperasional(draft: OperasionalDraft, hasil: HasilKirimOperasional) {
        val entryBaru = hasil.entryBaru ?: return
        val updatedKaryawanList = daftarKaryawan.value.map {
            if (it.uid == draft.uid) it.copy(saldoOperasional = hasil.saldoBaru) else it
        }
        daftarKaryawan.value = updatedKaryawanList
        if (operasionalKaryawan.value?.uid == draft.uid) {
            operasionalKaryawan.value = operasionalKaryawan.value?.copy(saldoOperasional = hasil.saldoBaru)
        }

        val updatedOperasional = if (draft.editId != null) {
            operasionalHariIni.value.map { if (it.id == draft.editId) entryBaru else it }
        } else {
            operasionalHariIni.value + entryBaru
        }
        operasionalHariIni.value = updatedOperasional

        withContext(Dispatchers.IO) {
            PrefsHelper.saveDaftarKaryawan(this@MainActivity, updatedKaryawanList)
            PrefsHelper.saveOperasionalHariIni(this@MainActivity, TimeValidator.currentDateString(), updatedOperasional)
        }
    }

    private fun tandaiPendingGagal(localId: String) {
        val ada = pendingOperasionalSubmissions.value.any { it.localId == localId }
        if (ada) {
            pendingOperasionalSubmissions.value = pendingOperasionalSubmissions.value.map {
                if (it.localId == localId) it.copy(status = "GAGAL") else it
            }
        }
    }

    /** Popup HANYA muncul kalau pengguna masih di menu Laporan Operasional. */
    private fun selesaikanNotifikasi(sukses: Boolean, offline: Boolean, pesan: String) {
        if (sukses) SoundHelper.playSukses() else SoundHelper.playError()
        if (currentScreen.value == Screen.OPERASIONAL_MENU || currentScreen.value == Screen.OPERASIONAL_FORM) {
            operasionalResultSuccess.value = sukses
            operasionalResultOffline.value = offline
            operasionalResultMessage.value = pesan
            showOperasionalResultDialog.value = true
        }
    }

    /** Dijalankan saat HP kembali online (onResume) -- coba kirim ulang semua draft tertunda. */
    private suspend fun flushOperasionalQueue() {
        val drafts = withContext(Dispatchers.IO) { operasionalQueue.getDaftarDraft() }
        for (draft in drafts) {
            val hasil = kirimSatuDraftOperasional(draft)
            if (hasil.sukses) {
                terapkanHasilSuksesOperasional(draft, hasil)
                pendingOperasionalSubmissions.value = pendingOperasionalSubmissions.value.filterNot { it.localId == draft.localId }
            } else {
                tandaiPendingGagal(draft.localId)
            }
        }
    }

    /**
     * Hapus laporan (hanya tersedia di layar edit). Butuh internet -- server yang
     * menghitung ulang Saldo berantai & menghapus foto di Drive.
     */
    private fun hapusLaporanOperasional() {
        val editEntry = editingOperasionalEntry.value ?: return
        val karyawan = operasionalKaryawan.value ?: return

        lifecycleScope.launch {
            try {
                val hasil = withContext(Dispatchers.IO) {
                    ApiClient.post(mapOf("action" to "hapusOperasional", "id" to editEntry.id, "uid" to karyawan.uid))
                }
                val sukses = hasil.optString("status") == "SUKSES"
                operasionalResultSuccess.value = sukses
                operasionalResultOffline.value = false
                operasionalResultMessage.value = hasil.optString("pesan", if (sukses) "Laporan berhasil dihapus." else "Gagal menghapus laporan.")

                if (sukses) {
                    SoundHelper.playSukses()
                    val saldoBaru = hasil.optDouble("saldo", karyawan.saldoOperasional)
                    val updatedKaryawan = daftarKaryawan.value.map { if (it.uid == karyawan.uid) it.copy(saldoOperasional = saldoBaru) else it }
                    daftarKaryawan.value = updatedKaryawan
                    operasionalKaryawan.value = operasionalKaryawan.value?.copy(saldoOperasional = saldoBaru)
                    withContext(Dispatchers.IO) { PrefsHelper.saveDaftarKaryawan(this@MainActivity, updatedKaryawan) }

                    val updatedOperasional = operasionalHariIni.value.filterNot { it.id == editEntry.id }
                    operasionalHariIni.value = updatedOperasional
                    withContext(Dispatchers.IO) {
                        PrefsHelper.saveOperasionalHariIni(this@MainActivity, TimeValidator.currentDateString(), updatedOperasional)
                    }

                    editingOperasionalEntry.value = null
                    capturedPhotoBitmap.value = null
                    currentScreen.value = Screen.OPERASIONAL_MENU
                } else {
                    SoundHelper.playError()
                }
                showOperasionalResultDialog.value = true
            } catch (e: Exception) {
                operasionalResultSuccess.value = false
                operasionalResultOffline.value = false
                operasionalResultMessage.value = "Gagal menghapus. Periksa koneksi internet."
                showOperasionalResultDialog.value = true
                SoundHelper.playError()
            }
        }
    }

    // ---------------- Gudang Alat (SELALU ONLINE, tidak dicache/antre offline) ----------------

    /**
     * Kartu ditap di layar tunggu "Ambil Alat" -- kenali dari cache lokal (SAMA seperti
     * absen), lalu buka form pengisian nama alat. Tidak butuh internet untuk tahap ini.
     */
    private fun prosesTapGudangAmbil(uid: String) {
        val karyawan = daftarKaryawan.value.find { it.uid == uid }
        if (karyawan == null) {
            SoundHelper.playError()
            HapticHelper.gagal(this)
            Toast.makeText(this, "Kartu tidak dikenali. Lakukan sinkronisasi data.", Toast.LENGTH_SHORT).show()
            return
        }
        SoundHelper.playSukses()
        HapticHelper.sukses(this)
        gudangKaryawan.value = karyawan
        formGudangAlatText.value = ""
        currentScreen.value = Screen.GUDANG_AMBIL_FORM
    }

    /**
     * Kartu ditap di layar tunggu "Kembalikan Alat" -- kenali dari cache lokal, lalu
     * tarik LIVE dari server daftar alat yang MASIH tercatat dipinjam atas nama UID ini
     * (BUKAN dari cache, karena status pinjam harus akurat real-time).
     */
    private fun prosesTapGudangKembali(uid: String) {
        val karyawan = daftarKaryawan.value.find { it.uid == uid }
        if (karyawan == null) {
            SoundHelper.playError()
            HapticHelper.gagal(this)
            Toast.makeText(this, "Kartu tidak dikenali. Lakukan sinkronisasi data.", Toast.LENGTH_SHORT).show()
            return
        }
        SoundHelper.playSukses()
        HapticHelper.sukses(this)
        gudangKaryawan.value = karyawan
        gudangSelectedIds.value = emptySet()
        gudangAlatUntukDikembalikan.value = emptyList()
        currentScreen.value = Screen.GUDANG_KEMBALI_FORM
        isLoadingGudangKembali.value = true

        lifecycleScope.launch {
            try {
                val hasil = withContext(Dispatchers.IO) { ApiClient.get("getGudangAktif", mapOf("uid" to karyawan.uid)) }
                gudangAlatUntukDikembalikan.value = parseAlatAktif_(hasil)
            } catch (e: Exception) {
                gudangAlatUntukDikembalikan.value = emptyList()
                Toast.makeText(this@MainActivity, "Gagal memuat daftar alat. Periksa koneksi internet.", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingGudangKembali.value = false
            }
        }
    }

    private fun parseAlatAktif_(hasil: org.json.JSONObject): List<AlatDipinjam> {
        val arr = hasil.optJSONArray("aktif") ?: return emptyList()
        val list = mutableListOf<AlatDipinjam>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                AlatDipinjam(
                    id = obj.optString("id"), uid = obj.optString("uid"), nama = obj.optString("nama"),
                    namaAlat = obj.optString("namaAlat"), tanggalAmbil = obj.optString("tanggalAmbil"),
                    jumlahHari = obj.optInt("jumlahHari", 0)
                )
            )
        }
        return list
    }

    /** Kirim pengambilan alat (bisa banyak sekaligus, satu alat per baris) ke server. */
    private fun submitAmbilAlat() {
        val karyawan = gudangKaryawan.value ?: return
        val daftarAlat = formGudangAlatText.value.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (daftarAlat.isEmpty()) return

        isSubmittingGudang.value = true
        lifecycleScope.launch {
            try {
                val namaAlatJson = org.json.JSONArray(daftarAlat).toString()
                val hasil = withContext(Dispatchers.IO) {
                    ApiClient.post(mapOf("action" to "ambilAlat", "uid" to karyawan.uid, "namaAlatList" to namaAlatJson))
                }
                val sukses = hasil.optString("status") == "SUKSES"
                gudangResultSuccess.value = sukses
                gudangResultMessage.value = hasil.optString(
                    "pesan",
                    if (sukses) "${daftarAlat.size} alat berhasil dicatat diambil oleh ${karyawan.nama}." else "Gagal mencatat pengambilan alat."
                )
                showGudangResultDialog.value = true
                if (sukses) {
                    SoundHelper.playSukses()
                    formGudangAlatText.value = ""
                    gudangKaryawan.value = null
                    currentScreen.value = Screen.GUDANG_MENU
                } else {
                    SoundHelper.playError()
                }
            } catch (e: Exception) {
                gudangResultSuccess.value = false
                gudangResultMessage.value = "Gagal mengirim data. Periksa koneksi internet."
                showGudangResultDialog.value = true
                SoundHelper.playError()
            } finally {
                isSubmittingGudang.value = false
            }
        }
    }

    /** Kirim pengembalian alat (bisa banyak sekaligus) yang dicentang ke server. */
    private fun submitKembalikanAlat() {
        val karyawan = gudangKaryawan.value ?: return
        val ids = gudangSelectedIds.value
        if (ids.isEmpty()) return

        isSubmittingGudang.value = true
        lifecycleScope.launch {
            try {
                val idsJson = org.json.JSONArray(ids.toList()).toString()
                val hasil = withContext(Dispatchers.IO) {
                    ApiClient.post(mapOf("action" to "kembalikanAlat", "uid" to karyawan.uid, "ids" to idsJson))
                }
                val sukses = hasil.optString("status") == "SUKSES"
                gudangResultSuccess.value = sukses
                gudangResultMessage.value = hasil.optString(
                    "pesan",
                    if (sukses) "${ids.size} alat berhasil dicatat dikembalikan." else "Gagal mencatat pengembalian alat."
                )
                showGudangResultDialog.value = true
                if (sukses) {
                    SoundHelper.playSukses()
                    gudangAlatUntukDikembalikan.value = gudangAlatUntukDikembalikan.value.filterNot { ids.contains(it.id) }
                    gudangSelectedIds.value = emptySet()
                    gudangKaryawan.value = null
                    currentScreen.value = Screen.GUDANG_MENU
                } else {
                    SoundHelper.playError()
                }
            } catch (e: Exception) {
                gudangResultSuccess.value = false
                gudangResultMessage.value = "Gagal mengirim data. Periksa koneksi internet."
                showGudangResultDialog.value = true
                SoundHelper.playError()
            } finally {
                isSubmittingGudang.value = false
            }
        }
    }

    /** Muat SEMUA alat gudang yang masih dipinjam (lintas orang), untuk menu Histori. */
    private fun muatGudangHistori() {
        isLoadingGudangHistori.value = true
        gudangHistoriError.value = null
        lifecycleScope.launch {
            try {
                val hasil = withContext(Dispatchers.IO) { ApiClient.get("getGudangAktif") }
                gudangAlatAktifSemua.value = parseAlatAktif_(hasil)
            } catch (e: Exception) {
                gudangHistoriError.value = "Gagal memuat data. Periksa koneksi internet."
            } finally {
                isLoadingGudangHistori.value = false
            }
        }
    }

    // ---------------- Menu Rahasia Admin ----------------

    private fun handleLogoTap() {
        val now = System.currentTimeMillis()
        if (now - lastLogoTapTime > 2000) logoTapCount = 0
        lastLogoTapTime = now
        logoTapCount++
        if (logoTapCount >= 5) {
            logoTapCount = 0
            showAdminPasswordDialog.value = true
        }
    }

    private fun sinkronisasiData() {
        isSyncingData.value = true
        adminStatusMessage.value = null
        lifecycleScope.launch {
            try {
                val hasilProyek = withContext(Dispatchers.IO) { ApiClient.get("getProjects") }
                val proyekArr = hasilProyek.optJSONArray("proyek")
                val proyekList = mutableListOf<String>()
                if (proyekArr != null) for (i in 0 until proyekArr.length()) proyekList.add(proyekArr.getString(i))

                val hasilKaryawan = withContext(Dispatchers.IO) { ApiClient.get("getKaryawan") }
                val karyawanArr = hasilKaryawan.optJSONArray("karyawan")
                val karyawanList = mutableListOf<Karyawan>()
                if (karyawanArr != null) {
                    for (i in 0 until karyawanArr.length()) {
                        val obj = karyawanArr.getJSONObject(i)
                        karyawanList.add(
                            Karyawan(
                                uid = obj.optString("uid"), nama = obj.optString("nama"), tempatKerja = obj.optString("tempatKerja"),
                                posisi = obj.optString("posisi"), tanggalDaftar = obj.optString("tanggalDaftar"),
                                upahHarian = obj.optDouble("upahHarian", 0.0), upahLembur = obj.optDouble("upahLembur", 0.0),
                                jumlahHariMasuk = obj.optDouble("jumlahHariMasuk", 0.0), jumlahJamLembur = obj.optDouble("jumlahJamLembur", 0.0),
                                bonDiterima = obj.optDouble("bonDiterima", 0.0), maksimalBon = obj.optDouble("maksimalBon", 0.0),
                                adaPengajuanMenunggu = obj.optBoolean("adaPengajuanMenunggu", false),
                                aksesOperasional = obj.optBoolean("aksesOperasional", false),
                                saldoOperasional = obj.optDouble("saldoOperasional", 0.0)
                            )
                        )
                    }
                }

                val hasilAbsensi = withContext(Dispatchers.IO) { ApiClient.get("getTodayAbsensi") }
                val absensiArr = hasilAbsensi.optJSONArray("absensi")
                val absensiList = mutableListOf<AbsensiHariIniEntry>()
                if (absensiArr != null) {
                    for (i in 0 until absensiArr.length()) {
                        val obj = absensiArr.getJSONObject(i)
                        absensiList.add(
                            AbsensiHariIniEntry(
                                uid = obj.optString("uid"), nama = obj.optString("nama"), jenis = obj.optString("jenis"),
                                proyek = obj.optString("proyek"), keterangan = obj.optString("keterangan"), jam = obj.optString("jam")
                            )
                        )
                    }
                }

                val hasilOperasional = withContext(Dispatchers.IO) { ApiClient.get("getTodayOperasional") }
                val operasionalArr = hasilOperasional.optJSONArray("operasional")
                val operasionalList = mutableListOf<OperasionalEntry>()
                if (operasionalArr != null) {
                    for (i in 0 until operasionalArr.length()) {
                        val obj = operasionalArr.getJSONObject(i)
                        operasionalList.add(
                            OperasionalEntry(
                                id = obj.optString("id"), uid = obj.optString("uid"), tanggal = obj.optString("tanggal"),
                                tipe = obj.optString("tipe"), kategori = obj.optString("kategori"), jumlah = obj.optDouble("jumlah", 0.0),
                                keterangan = obj.optString("keterangan"), saldo = obj.optDouble("saldo", 0.0),
                                namaFileBukti = obj.optString("namaFileBukti"), urlBukti = obj.optString("urlBukti"),
                                diinputOleh = obj.optString("diinputOleh")
                            )
                        )
                    }
                }

                daftarProyek.value = proyekList
                daftarKaryawan.value = karyawanList
                absensiHariIni.value = absensiList
                operasionalHariIni.value = operasionalList

                withContext(Dispatchers.IO) {
                    PrefsHelper.saveDaftarProyek(this@MainActivity, proyekList)
                    PrefsHelper.saveDaftarKaryawan(this@MainActivity, karyawanList)
                    PrefsHelper.saveAbsensiHariIni(this@MainActivity, TimeValidator.currentDateString(), absensiList)
                    PrefsHelper.saveOperasionalHariIni(this@MainActivity, TimeValidator.currentDateString(), operasionalList)
                }

                // Riwayat Laporan Operasional (SELURUH riwayat, bukan cuma hari ini) --
                // HANYA milik identitas yang tersimpan di HP ini (kalau ada), supaya
                // tidak menarik data semua orang. Foto TIDAK ikut disinkron (hemat kuota).
                val savedOperasionalUid = PrefsHelper.getOperasionalIdentityUid(this@MainActivity)
                var jumlahRiwayat = 0
                if (savedOperasionalUid != null) {
                    try {
                        val hasilRiwayat = withContext(Dispatchers.IO) {
                            ApiClient.get("getRiwayatOperasional", mapOf("uid" to savedOperasionalUid))
                        }
                        val riwayatArr = hasilRiwayat.optJSONArray("riwayat")
                        val riwayatList = mutableListOf<RiwayatOperasionalEntry>()
                        if (riwayatArr != null) {
                            for (i in 0 until riwayatArr.length()) {
                                val obj = riwayatArr.getJSONObject(i)
                                riwayatList.add(
                                    RiwayatOperasionalEntry(
                                        id = obj.optString("id"), tanggal = obj.optString("tanggal"), tipe = obj.optString("tipe"),
                                        kategori = obj.optString("kategori"), jumlah = obj.optDouble("jumlah", 0.0),
                                        keterangan = obj.optString("keterangan"), saldo = obj.optDouble("saldo", 0.0),
                                        urlBukti = obj.optString("urlBukti")
                                    )
                                )
                            }
                        }
                        riwayatOperasional.value = riwayatList
                        jumlahRiwayat = riwayatList.size
                        withContext(Dispatchers.IO) {
                            PrefsHelper.saveRiwayatOperasional(this@MainActivity, savedOperasionalUid, riwayatList)
                        }
                    } catch (e: Exception) {
                        // Gagal ambil riwayat bukan masalah fatal -- data lain tetap tersinkron.
                    }
                }

                operasionalKaryawan.value?.let { aktif ->
                    karyawanList.find { it.uid == aktif.uid }?.let { operasionalKaryawan.value = it }
                }

                adminStatusMessage.value =
                    "Sinkron: ${karyawanList.size} karyawan, ${proyekList.size} proyek, ${absensiList.size} absen hari ini, ${operasionalList.size} laporan operasional hari ini, $jumlahRiwayat riwayat laporan."
                Toast.makeText(this@MainActivity, "Sinkronisasi data berhasil.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                adminStatusMessage.value = "Gagal sinkronisasi. Periksa koneksi internet."
                Toast.makeText(this@MainActivity, adminStatusMessage.value, Toast.LENGTH_SHORT).show()
            } finally {
                isSyncingData.value = false
            }
        }
    }

    private fun clearCacheAdmin() {
        lifecycleScope.launch(Dispatchers.IO) {
            applicationContext.filesDir.listFiles()
                ?.filter { it.name == AppConstants.OFFLINE_QUEUE_FILE }
                ?.forEach { it.delete() }
            withContext(Dispatchers.Main) {
                adminStatusMessage.value = "Cache & antrean offline berhasil dibersihkan."
            }
        }
    }

    private fun downloadRekapPdf(bulan: Int, tahun: Int) {
        isDownloadingPdf.value = true
        adminStatusMessage.value = null
        lifecycleScope.launch {
            try {
                val hasil = withContext(Dispatchers.IO) {
                    ApiClient.get("exportPdf", mapOf("bulan" to bulan.toString(), "tahun" to tahun.toString()))
                }
                val url = hasil.optString("pdfUrl")
                if (url.isNotEmpty()) {
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setTitle("Rekap Absen SSP $bulan-$tahun")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "RekapAbsenSSP_${bulan}_${tahun}.pdf")
                    val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(request)
                    adminStatusMessage.value = "Unduhan PDF dimulai, cek folder Download."
                } else {
                    adminStatusMessage.value = "Server belum menyediakan rekap untuk periode ini."
                }
            } catch (e: Exception) {
                adminStatusMessage.value = "Gagal mengunduh PDF. Periksa koneksi internet."
            } finally {
                isDownloadingPdf.value = false
            }
        }
    }

    // ---------------- Util Umum ----------------

    private fun muatDataAwal() {
        refreshStatusJaringan()
        cekJamPerangkat()
        daftarProyek.value = PrefsHelper.getDaftarProyek(this)
        daftarKaryawan.value = PrefsHelper.getDaftarKaryawan(this)
        absensiHariIni.value = PrefsHelper.getAbsensiHariIni(this, TimeValidator.currentDateString())
        operasionalHariIni.value = PrefsHelper.getOperasionalHariIni(this, TimeValidator.currentDateString())
    }

    private fun refreshStatusJaringan() {
        isOnline.value = NetworkStatusHelper.isOnline(this)
    }

    private fun cekJamPerangkat() {
        jamValid.value = TimeValidator.isAutoTimeEnabled(this)
    }
}

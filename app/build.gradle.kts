// File Path: app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sirlasirliputeri.absenssp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sirlasirliputeri.absenssp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        // Kunci Debug TETAP/SAMA supaya setiap APK yang dibangun GitHub Actions bisa
        // menimpa (update) instalasi sebelumnya di HP, TANPA perlu uninstall dulu.
        // Tanpa ini, tiap build CI dijalankan di komputer virtual baru yang otomatis
        // bikin kunci debug baru setiap kali -- akibatnya Android menganggap tiap
        // APK hasil CI sebagai "aplikasi lain" walau applicationId-nya sama persis.
        create("debugTetap") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debugTetap")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Hanya pustaka bawaan Android + Compose + CameraX. TIDAK memakai OkHttp / Room
    // sesuai spesifikasi (efisiensi ukuran file). Network pakai HttpURLConnection,
    // parsing JSON pakai org.json bawaan Android SDK.
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Ikon Material inti (set standar ~90 ikon, BUKAN material-icons-extended yang jauh
    // lebih berat) -- dipakai di seluruh layar hasil redesain UI/UX v2 sebagai pengganti
    // teks/emoji polos, tetap sejalan dengan prinsip efisiensi ukuran file di atas.
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // CameraX -- dipakai untuk layar kamera kustom Laporan Operasional (foto bukti
    // bisa diambil langsung dari kamera dalam app, atau dipilih dari galeri).
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Modul Invoice (lihat app/src/main/java/.../invoice/) -- SUDAH full Jetpack Compose,
    // 3 Activity-nya cuma ComponentActivity biasa (sama seperti MainActivity), jadi TIDAK
    // butuh AppCompat/RecyclerView/CardView/Material Components lagi (sudah dibuang,
    // sesuai prinsip "efisiensi ukuran file" di atas).
    // ML Kit Document Scanner: auto crop, manual crop, multi-halaman, export PDF.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
}

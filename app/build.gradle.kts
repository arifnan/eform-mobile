plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.kotlin.compose) // Plugin Compose biasanya tidak di-alias seperti ini, tapi via buildFeatures atau composeOptions
    id("org.jetbrains.kotlin.plugin.compose") // Cara standar untuk mengaktifkan plugin Compose Compiler
    id("kotlin-kapt")
}

android {
    namespace = "com.example.eform"
    compileSdk = 35 // Pastikan ini adalah versi yang stabil dan terinstal

    defaultConfig {
        applicationId = "com.example.eform"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Untuk vectorDrawables jika menggunakan XML vector drawables
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Pertimbangkan true untuk rilis produksi
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    // Opsi Compose Compiler (jika Anda tidak menggunakan plugin 'org.jetbrains.kotlin.plugin.compose')
    // composeOptions {
    //     kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    // }

    // >>> Tambahkan blok packagingOptions di sini <<<
    packagingOptions {
        resources {
            excludes += "META-INF/INDEX.LIST"
            // Tambahkan exclude lain jika error duplikasi muncul untuk file berbeda di META-INF
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module" // Umumnya aman untuk diexclude
            excludes += "DebugProbesKt.bin" // Terkadang ini juga bisa menyebabkan masalah
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    // Pastikan versi material3 konsisten atau gunakan dari BOM
    // implementation("androidx.compose.material3:material3:1.3.2") // Anda menyebutkan versi ini
    implementation(libs.androidx.material3) // Jika Anda sudah mendefinisikan libs.androidx.material3 di libs.versions.toml

    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.maps)
    // implementation(libs.androidx.navigation.runtime.ktx) // Ini biasanya transitif dari navigation.compose

    // Firebase App Distribution Gradle Plugin seharusnya hanya di level project build.gradle
    // Jika Anda menambahkannya di sini sebagai 'implementation', itu bisa jadi sumber masalah.
    // Cek apakah 'libs.firebase.appdistribution.gradle' benar-benar library runtime.
    // Sepertinya ini adalah plugin, bukan library. Hapus jika ini adalah plugin.
    // implementation(libs.firebase.appdistribution.gradle) // <<< PERIKSA INI

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.foundation) // Biasanya bagian dari compose.bom atau ui

    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor) // okhttp3-logging-interceptor

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Room Database
    implementation(libs.androidx.room.runtime)
    // annotationProcessor(libs.androidx.room.compiler) // Tidak perlu jika menggunakan kapt
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler) // Gunakan alias yang benar dari libs.versions.toml, misal libs.androidx.room.compiler

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Coil (Image Loading)
    implementation(libs.coil.compose)

    // Animated Navigation Bar (jika ini library pihak ketiga)
    implementation(libs.animated.navigation.bar)

    // Material Icons Extended
    implementation(libs.androidx.material.icons.extended)

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0") // Gunakan versi terbaru yang stabil
    //kalo mau pake google maps api cloud
    //implementation("com.google.maps.android:maps-compose:4.4.1")
    //implementation("com.google.android.gms:play-services-maps:19.2.0")

    // Compose Reorderable (jika masih digunakan)
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    //gson
    implementation(libs.converter.gson)
}
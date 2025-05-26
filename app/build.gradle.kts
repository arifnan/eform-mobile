plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id ("kotlin-kapt")
}

android {
    namespace = "com.example.eform"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.eform"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
//cek versinya di gradle/libs.version.tom;
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.foundation)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.logging.interceptor)
    implementation(libs.androidx.datastore.preferences)

    // Room Database dependencies
    implementation (libs.androidx.room.runtime)
    annotationProcessor (libs.androidx.room.compiler) // Untuk Java
    implementation (libs.androidx.room.ktx) // Untuk Kotlin
   kapt(libs.androidx.room.compiler.v261)

    // Coroutine support (opsional, jika menggunakan coroutine)
    implementation (libs.kotlinx.coroutines.android)


    implementation (libs.coil.compose)
//bottom bar animated
    implementation(libs.animated.navigation.bar)


    //untuk icon
//    implementation ("com.mikepenz:iconics-core:5.2.2")
//    implementation ("com.mikepenz:fontawesome-typeface:5.15.3.1")
    implementation(libs.androidx.material.icons.extended)

    //untuk lokasi
    implementation ("com.google.android.gms:play-services-location:21.0.1")

    //card
    implementation ("org.burnoutcrew.composereorderable:reorderable:0.9.6")

}
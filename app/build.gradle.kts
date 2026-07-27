import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compiler.extension)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Load keystore info from local.properties
val keystorePropertiesFile = rootProject.file("local.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.alhasanah.alhasanahmedia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alhasanah.alhasanahmedia"
        minSdk = 29
        targetSdk = 35
        versionCode = project.findProperty("versionCode")?.toString()?.toIntOrNull() ?: 1
        versionName = project.findProperty("versionName")?.toString() ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        buildConfigField("String", "SUPABASE_URL", "\"${keystoreProperties.getProperty("supabase.url", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${keystoreProperties.getProperty("supabase.anon.key", "")}\"")
        buildConfigField("String", "AHMAD_SANUSI_API_KEY", "\"${keystoreProperties.getProperty("ahmadsanusi.api.key", "")}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("release.keystore") ?: "non_existent_file")
            storePassword = keystoreProperties.getProperty("release.keystore.password")
            keyAlias = keystoreProperties.getProperty("release.key.alias")
            keyPassword = keystoreProperties.getProperty("release.key.password")
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            //applicationIdSuffix = ".debug"
            isDebuggable = true
        }

        getByName("release") {
            // OPTIMASI RILIS (Dimatikan untuk mode pengembangan/debugging)
            isMinifyEnabled = true      // Ubah ke true saat mau rilis
            isShrinkResources = true  // Ubah ke true saat mau rilis
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        // Menonaktifkan pengecekan yang menyebabkan crash pada Kotlin 2.1.0
        disable += "NullSafeMutableLiveData"
        checkReleaseBuilds = true
        abortOnError = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            pickFirsts += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "META-INF/DEPENDENCIES"
            pickFirsts += "META-INF/LICENSE"
            pickFirsts += "META-INF/LICENSE.txt"
            pickFirsts += "META-INF/license.txt"
            pickFirsts += "META-INF/NOTICE"
            pickFirsts += "META-INF/NOTICE.txt"
            pickFirsts += "META-INF/notice.txt"
            pickFirsts += "META-INF/ASL2.0"
            pickFirsts += "META-INF/*.kotlin_module"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material:material")
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Coil
    implementation(libs.coil.compose)

    // Supabase & Ktor
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Koin
    implementation(libs.koin.androidx.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")

    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // ✅ Lokasi & Waktu Sholat
    implementation(libs.play.services.location)
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation(libs.adhan2)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.play.services)

    // WorkManager - periodic background tasks
    implementation(libs.androidx.work.runtime)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // Fix Runtime Crash
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.guava)

    // Kizitonwose Calendar
    implementation("com.kizitonwose.calendar:compose:2.7.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.debug.androidx.compose.ui.tooling)
    debugImplementation(libs.debug.androidx.compose.ui.test.manifest)
}

configurations.all {
    resolutionStrategy {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    }
}

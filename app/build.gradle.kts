plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ====== Pas dit aan als je GitHub-gebruikersnaam of repo-naam anders is ======
val webHost = "gunnarcstudios.github.io"   // <gebruikersnaam>.github.io
val webPath = "/Koffie"                    // /<repo-naam>
// ==============================================================================

android {
    namespace = "nl.koffie.wiehaaltkoffie"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.koffie.wiehaaltkoffie"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "8.1"

        buildConfigField("String", "WEB_HOST", "\"$webHost\"")
        buildConfigField("String", "WEB_URL", "\"https://$webHost$webPath/\"")
        manifestPlaceholders["webHost"] = webHost
        manifestPlaceholders["webPath"] = webPath
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.2")
}

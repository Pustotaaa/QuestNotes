plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Подпись.
// app/debug.keystore — общий отладочный ключ, лежит в репозитории, чтобы любая сборка
// (в GitHub Actions или у вас в Android Studio) ставилась поверх предыдущей без удаления.
// Для публикации в Google Play заведите свой ключ: положите его в секреты репозитория
// QN_KEYSTORE_BASE64 / QN_STORE_PASSWORD / QN_KEY_PASSWORD — workflow подхватит его сам.
val keystorePath: String? = System.getenv("QN_KEYSTORE")
val hasReleaseKey = keystorePath != null && file(keystorePath).exists()

android {
    namespace = "com.questnotes.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.questnotes.app"
        minSdk = 26
        targetSdk = 35
        // номер сборки GitHub Actions, чтобы каждая новая версия ставилась поверх старой
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt() + 100
        versionName = "0.2." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("QN_STORE_PASSWORD")
                keyAlias = System.getenv("QN_KEY_ALIAS") ?: "questnotes"
                keyPassword = System.getenv("QN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        // woff2 уже сжаты, не тратим время на повторное сжатие
        noCompress += listOf("woff2")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}

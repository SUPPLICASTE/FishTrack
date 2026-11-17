plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.fishtrack.app"
    compileSdk = 35

    defaultConfig {
        // Identificador permanente: no cambiarlo, así Android reconoce las actualizaciones.
        applicationId = "org.fishtrack.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 14
        versionName = "3.3.3"
    }

    signingConfigs {
        create("release") {
            // La misma keystore se usa en todas las versiones publicadas.
            val keystore = file("fishtrack.keystore")
            val storePass = System.getenv("KEYSTORE_PASSWORD")
            val alias = System.getenv("KEY_ALIAS")
            val keyPass = System.getenv("KEY_PASSWORD")

            if (keystore.exists() && !storePass.isNullOrBlank() &&
                !alias.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                storeFile = keystore
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // Las compilaciones debug locales conservan la firma debug habitual.
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
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
}

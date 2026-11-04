
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.fishtrack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.fishtrack.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "3.1.2"
    }
}

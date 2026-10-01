import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Put MAPS_API_KEY=... in local.properties (never commit it)
val mapsApiKey: String = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}.getProperty("MAPS_API_KEY", "")

android {
    namespace = "io.github.sdsd08013.viewmarkerkit.sample.views"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.sdsd08013.viewmarkerkit.sample.views"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":viewmarkerkit"))
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
}

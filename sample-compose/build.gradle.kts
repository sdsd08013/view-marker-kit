import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Put MAPS_API_KEY=... in local.properties (never commit it)
val mapsApiKey: String = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}.getProperty("MAPS_API_KEY", "")

android {
    namespace = "io.github.sdsd08013.viewmarkerkit.sample.compose"

    // Compose 1.12 requires API 37
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.sdsd08013.viewmarkerkit.sample.compose"
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
        compose = true
    }
}

dependencies {
    implementation(project(":viewmarkerkit"))
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.google.maps.services)
    debugImplementation(libs.compose.ui.tooling)
}

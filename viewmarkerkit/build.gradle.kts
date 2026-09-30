plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "io.github.sdsd08013.viewmarkerkit"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // api: types exposed in the public API
    api(libs.androidx.lifecycle.runtime)
    api(libs.google.maps.services)

    implementation(libs.androidx.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.vintage)
    testImplementation(libs.kotest.runner)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.sdsd08013"
            artifactId = "view-marker-kit"
            version = providers.gradleProperty("version").orElse("0.0.1-SNAPSHOT").get()

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

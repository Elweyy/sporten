import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Dépôt GitHub public où sont publiées les releases (APK). L'app y cherche les mises à jour.
 * Format « propriétaire/dépôt ».
 */
val updateRepo = "Elweyy/sporten"

/**
 * Clé de signature des releases : hors du dépôt, dans ~/.config/sporten/signing.properties
 * (storeFile relatif à ce dossier). Sans ce fichier, la release n'est pas signée.
 */
val signingDir = File(System.getProperty("user.home"), ".config/sporten")
val signingProps = File(signingDir, "signing.properties").takeIf { it.exists() }?.let { f ->
    Properties().apply { f.inputStream().use { load(it) } }
}

android {
    namespace = "fr.loic.seances"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.loic.seances"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.10"
    }

    signingConfigs {
        if (signingProps != null) create("release") {
            storeFile = File(signingDir, signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
        }
        debug {
            // Une version de dev (autre clé) ne peut pas installer une release : pas de mises à jour.
            buildConfigField("String", "UPDATE_REPO", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Captures d'écran du README (docs/screenshots) : ./gradlew testDebugUnitTest -Pscreenshots
                if (project.hasProperty("screenshots")) {
                    it.systemProperty("roborazzi.test.record", "true")
                    it.filter.includeTestsMatching("*ScreenshotsTest*")
                } else {
                    it.exclude("**/ScreenshotsTest*")
                }
            }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}

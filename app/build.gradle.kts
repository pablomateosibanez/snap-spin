import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Las credenciales nunca se escriben en el código ni se suben al repositorio:
 * se leen de `local.properties` (ignorado por git) y se exponen vía BuildConfig.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String, default: String = ""): String =
    (localProperties.getProperty(key) ?: System.getenv(key) ?: default).trim()

android {
    namespace = "com.example.snapspin"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.snapspin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DISCOGS_TOKEN", "\"${secret("DISCOGS_TOKEN")}\"")
        buildConfigField("String", "DISCOGS_USERNAME", "\"${secret("DISCOGS_USERNAME")}\"")
        buildConfigField("String", "DISCOGS_USER_AGENT", "\"SnapSpin/1.0 +https://github.com/local/discogs-collection-app\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${secret("GEMINI_API_KEY")}\"")
        buildConfigField(
            "String",
            "GEMINI_MODEL",
            "\"${secret("GEMINI_MODEL", "gemini-flash-latest")}\"",
        )

        // Opcional: si se define, se usa una API key simple en vez de la cuenta de servicio.
        buildConfigField("String", "VISION_API_KEY", "\"${secret("VISION_API_KEY")}\"")
        // Fichero JSON de la cuenta de servicio dentro de assets/ (vacío = deshabilitado).
        buildConfigField(
            "String",
            "VISION_SERVICE_ACCOUNT_ASSET",
            "\"${secret("VISION_SERVICE_ACCOUNT_ASSET", "google-vision-service-account.json")}\"",
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    packaging {
        resources.excludes += setOf("META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Cámara + lectura de códigos de barras (capa de datos)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    // Reconocimiento de portadas
    implementation(libs.generativeai)

    // Red + JSON
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Carátulas
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

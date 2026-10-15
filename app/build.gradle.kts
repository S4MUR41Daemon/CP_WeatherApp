import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ── Firma compartida ATMOS ──────────────────────────────────────────────
// La misma clave firma ATMOS y el launcher → el permiso de firma que los conecta funciona.
// En GitHub Actions llega por variables de entorno (secretos); en el PC, local.properties apunta
// al fichero keystore.properties (fuera del repo). Si no hay clave, se usa la debug como antes.
val atmosSigning: Map<String, String>? = run {
    System.getenv("ATMOS_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }?.let { path ->
        return@run mapOf(
            "storeFile" to path,
            "storePassword" to System.getenv("ATMOS_KEYSTORE_PASSWORD").orEmpty(),
            "keyAlias" to System.getenv("ATMOS_KEY_ALIAS").orEmpty(),
            "keyPassword" to System.getenv("ATMOS_KEY_PASSWORD").orEmpty()
        )
    }
    val local = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { local.load(it) }
    val f = local.getProperty("atmos.keystore.props")?.let { file(it) }?.takeIf { it.exists() } ?: return@run null
    val p = Properties().apply { f.inputStream().use { load(it) } }
    p.stringPropertyNames().associateWith { p.getProperty(it) }
}

android {
    namespace = "com.atmos.weather"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.atmos.weather"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "2.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (atmosSigning != null) create("atmos") {
            storeFile = file(atmosSigning.getValue("storeFile"))
            storePassword = atmosSigning.getValue("storePassword")
            keyAlias = atmosSigning.getValue("keyAlias")
            keyPassword = atmosSigning.getValue("keyPassword")
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (atmosSigning != null) signingConfigs.getByName("atmos") else signingConfigs.getByName("debug")
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.text.google.fonts)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)
    implementation(libs.serialization.json)

    implementation(libs.okhttp)
    implementation(libs.profileinstaller)
    // Play Services arrastra un Fragment < 1.3 incompatible con la API ActivityResult.
    implementation(libs.fragment)

    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.play.services.location)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}

import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun releaseProperty(name: String, environmentName: String): String? =
    providers.gradleProperty("cricketWatch.$name").orNull
        ?: localProperties.getProperty("cricketWatch.$name")
        ?: keystoreProperties.getProperty(name)
        ?: providers.environmentVariable(environmentName).orNull

fun debugSigningProperty(name: String): String? =
    providers.gradleProperty("androidDebugSigning.$name").orNull
        ?: localProperties.getProperty("androidDebugSigning.$name")

val debugStoreFile = debugSigningProperty("storeFile")
val debugStorePassword = debugSigningProperty("storePassword") ?: "android"
val debugKeyAlias = debugSigningProperty("keyAlias") ?: "androiddebugkey"
val debugKeyPassword = debugSigningProperty("keyPassword") ?: debugStorePassword
val hasStableDebugSigning = !debugStoreFile.isNullOrBlank()

val releaseStoreFile = releaseProperty("storeFile", "CRICKET_WATCH_KEYSTORE_FILE")
val releaseStorePassword = releaseProperty("storePassword", "CRICKET_WATCH_KEYSTORE_PASSWORD")
val releaseKeyAlias = releaseProperty("keyAlias", "CRICKET_WATCH_KEY_ALIAS")
val releaseKeyPassword = releaseProperty("keyPassword", "CRICKET_WATCH_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

fun configuredFile(path: String): File {
    val home = System.getProperty("user.home")
    val expanded = when {
        path == "~" -> home
        path.startsWith("~/") -> "$home/${path.removePrefix("~/")}"
        path.startsWith("\$HOME/") -> "$home/${path.removePrefix("\$HOME/")}"
        path.startsWith("\${user.home}/") -> "$home/${path.removePrefix("\${user.home}/")}"
        else -> path
    }
    return file(expanded)
}

android {
    namespace = "com.nedrichards.cricketwatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nedrichards.cricketwatch"
        minSdk = 30
        targetSdk = 33
        versionCode = (
            providers.gradleProperty("cricketWatch.versionCode").orNull
                ?: localProperties.getProperty("cricketWatch.versionCode")
                ?: "1"
            ).toInt()
        versionName = providers.gradleProperty("cricketWatch.versionName").orNull
            ?: localProperties.getProperty("cricketWatch.versionName")
            ?: "1.0"

        buildConfigField("String", "CRICKET_API_KEY", "\"${localProperties.getProperty("CRICKET_API_KEY") ?: ""}\"")
    }

    signingConfigs {
        if (hasStableDebugSigning) {
            create("stableDebug") {
                storeFile = configuredFile(debugStoreFile!!)
                storePassword = debugStorePassword
                keyAlias = debugKeyAlias
                keyPassword = debugKeyPassword
            }
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = configuredFile(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (hasStableDebugSigning) {
                signingConfig = signingConfigs.getByName("stableDebug")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    
    // Compose for Wear OS
    implementation("androidx.wear.compose:compose-material:1.6.1")
    implementation("androidx.wear.compose:compose-foundation:1.6.1")
    implementation("androidx.wear.compose:compose-navigation:1.6.1")
    
    // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
}

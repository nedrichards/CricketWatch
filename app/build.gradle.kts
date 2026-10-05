import java.io.File
import java.util.Properties
import com.android.build.api.dsl.ApplicationExtension

apply(plugin = "com.android.application")
apply(plugin = "org.jetbrains.kotlin.plugin.compose")

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

fun localBooleanProperty(name: String): Boolean {
    val value = providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
    return value.equals("true", ignoreCase = true)
}

val debugStoreFile = debugSigningProperty("storeFile")
val debugStorePassword = debugSigningProperty("storePassword") ?: "android"
val debugKeyAlias = debugSigningProperty("keyAlias") ?: "androiddebugkey"
val debugKeyPassword = debugSigningProperty("keyPassword") ?: debugStorePassword
val hasStableDebugSigning = !debugStoreFile.isNullOrBlank()
val debugSignRelease = localBooleanProperty("cricketWatch.debugSignRelease")

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

extensions.configure<ApplicationExtension>("android") {
    namespace = "com.nedrichards.cricketwatch"
    compileSdk = 37

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
            signingConfig = when {
                hasReleaseSigning -> signingConfigs.getByName("release")
                debugSignRelease && hasStableDebugSigning -> signingConfigs.getByName("stableDebug")
                debugSignRelease -> signingConfigs.getByName("debug")
                else -> null
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            keepDebugSymbols += "**/libandroidx.graphics.path.so"
        }
    }
}

dependencies {
    add("implementation", "androidx.core:core-ktx:1.19.1")
    add("implementation", "com.google.android.gms:play-services-wearable:20.0.1")
    add("implementation", "androidx.percentlayout:percentlayout:1.0.0")
    add("implementation", "androidx.legacy:legacy-support-v4:1.0.0")
    add("implementation", "androidx.recyclerview:recyclerview:1.4.0")

    add("implementation", platform("androidx.compose:compose-bom:2026.09.00"))
    
    // Compose for Wear OS
    add("implementation", "androidx.wear.compose:compose-material:1.7.0")
    add("implementation", "androidx.wear.compose:compose-foundation:1.7.0")
    add("implementation", "androidx.wear.compose:compose-navigation:1.7.0")
    
    // Core Compose
    add("implementation", "androidx.compose.ui:ui")
    add("implementation", "androidx.compose.ui:ui-tooling-preview")
    add("implementation", "androidx.activity:activity-compose:1.13.0")
    add("implementation", "androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    
    // Networking
    add("implementation", "com.squareup.retrofit2:retrofit:3.0.0")
    add("implementation", "com.squareup.retrofit2:converter-gson:3.0.0")
    add("implementation", "com.squareup.okhttp3:okhttp:5.5.0")

    add("testImplementation", "junit:junit:4.13.2")
}

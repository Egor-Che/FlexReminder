import java.util.Base64
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun propOrEnv(key: String): String? =
    localProps.getProperty(key) ?: System.getenv(key)

val ciKeystoreBase64 = System.getenv("KEYSTORE_BASE64")
val localKeystorePath = propOrEnv("KEYSTORE_FILE")
val localKeystorePassword = propOrEnv("KEYSTORE_PASSWORD")
val localKeyAlias = propOrEnv("KEY_ALIAS") ?: "flexreminder"
val localKeyPassword = propOrEnv("KEY_PASSWORD") ?: localKeystorePassword

val hasCiSigning = !ciKeystoreBase64.isNullOrBlank()
val hasLocalSigning = !localKeystorePath.isNullOrBlank() &&
        !localKeystorePassword.isNullOrBlank() &&
        rootProject.file(localKeystorePath).exists()
val hasReleaseSigning = hasCiSigning || hasLocalSigning

android {
    namespace = "com.example.flexreminder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.flexreminder"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            when {
                hasCiSigning -> {
                    val keystoreFile = File(
                        project.layout.buildDirectory.get().asFile,
                        "release.keystore"
                    )
                    keystoreFile.parentFile?.mkdirs()
                    keystoreFile.writeBytes(
                        Base64.getMimeDecoder().decode(ciKeystoreBase64)
                    )
                    storeFile = keystoreFile
                    storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                    keyAlias = System.getenv("KEY_ALIAS") ?: "flexreminder"
                    keyPassword = System.getenv("KEY_PASSWORD") ?: ""
                }
                hasLocalSigning -> {
                    storeFile = rootProject.file(localKeystorePath!!)
                    storePassword = localKeystorePassword
                    keyAlias = localKeyAlias
                    keyPassword = localKeyPassword
                }
            }
        }
    }

    buildFeatures { compose = true }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        debug {
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
	implementation("androidx.appcompat:appcompat:1.6.1")
}
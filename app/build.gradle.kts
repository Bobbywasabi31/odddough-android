plugins {
    id("com.android.application")
}

android {
    namespace = "com.cashcompass.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cashcompass.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 73
        versionName = "1.64.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Stable signing for preview builds: when CI provides the keystore
    // (ODDDOUGH_KEYSTORE_FILE env pointing at the decoded file, passwords
    // from repo secrets), every build is signed with the SAME key, so the
    // in-app updater can install a new preview over the previous one.
    // Without those envs (local dev, fork PRs) the default per-machine
    // debug key is used, exactly as before.
    val stableStoreFile = System.getenv("ODDDOUGH_KEYSTORE_FILE")
    signingConfigs {
        if (!stableStoreFile.isNullOrBlank()) {
            create("stable") {
                storeFile = file(stableStoreFile)
                storePassword = System.getenv("ODDDOUGH_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ODDDOUGH_KEY_ALIAS")
                keyPassword = System.getenv("ODDDOUGH_KEY_PASSWORD")
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            if (!stableStoreFile.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("stable")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // On-device receipt OCR (thin client; model via Play Services, keeps APK small)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    // At-rest encryption of the live plan (Jetpack Security EncryptedFile, key in Android Keystore)
    implementation("androidx.security:security-crypto:1.0.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    testImplementation("junit:junit:4.13.2")
    // Real org.json for JVM unit tests (android.jar only has stubs). Test-scope
    // only — never packaged into the APK.
    testImplementation("org.json:json:20240303")
}

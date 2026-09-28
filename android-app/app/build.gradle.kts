import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val apiBaseUrl = providers.gradleProperty("PAY_API_BASE_URL")
    .orElse("https://payments.example.invalid")
val identityUri = providers.gradleProperty("PAY_IDENTITY_URI")
    .orElse("https://pay.example.invalid")
val allowedSolanaMints = providers.gradleProperty("PAY_SOLANA_ALLOWED_MINTS")
    .orElse("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v")
val solanaRpcUrl = providers.gradleProperty("PAY_SOLANA_RPC_URL")
    .orElse("https://api.mainnet-beta.solana.com")
val appVersionCode = providers.gradleProperty("PAY_VERSION_CODE")
    .map(String::toInt)
    .orElse(1)
val appVersionName = providers.gradleProperty("PAY_VERSION_NAME")
    .orElse("0.1.0")
// A Play update must retain the exact existing Play Console application ID.
// Supply it explicitly for releases; sandbox keeps the local WAYQO namespace.

// The live wayqo.app site uses the v2 wordmark/scanner assets. Fail the build if
// an older logo is copied back into the app under the approved resource names.
val verifyWayqoBrandAssets = tasks.register("verifyWayqoBrandAssets") {
    val approved = mapOf(
        "wayqo_logo.png" to "6343567415423340f5f307c4be9dceeed403ac0aba4e92ae91f1f2051d2ce2e9",
        "wayqo_logo_scanner.png" to "c071c1a13c6cbd22a2bf389e69ef5b44b184df5b634d9281dcd67075a92f3372",
        "wayqo_scanner.png" to "c7a8c25143943a558958c89b8bc21dcc314639fc305ae25dfb31e21537f657ee",
        "wayqo_icon.png" to "63bca542851a2e5d1515069142e28c9ddda01983d11101a59bdb12ee73772f10",
    )
    val resourceDir = layout.projectDirectory.dir("src/main/res/drawable-nodpi")
    approved.keys.forEach { inputs.file(resourceDir.file(it)) }
    doLast {
        approved.forEach { (name, expected) ->
            val bytes = resourceDir.file(name).asFile.readBytes()
            val actual = MessageDigest.getInstance("SHA-256")
                .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
            check(actual == expected) {
                "$name differs from the approved wayqo.app v2 asset; review the site source before changing it"
            }
        }
    }
}

// Exact English BIP39 list from trezor/python-mnemonic. A reordered or edited
// list would change wallet addresses even when the 24 words looked valid.
val verifyRecoveryWordlist = tasks.register("verifyRecoveryWordlist") {
    val wordlist = layout.projectDirectory.file("src/main/assets/bip39_english.txt")
    inputs.file(wordlist)
    doLast {
        val actual = MessageDigest.getInstance("SHA-256")
            .digest(wordlist.asFile.readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        check(actual == "2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda") {
            "BIP39 English wordlist differs from trezor/python-mnemonic"
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(verifyWayqoBrandAssets)
    dependsOn(verifyRecoveryWordlist)
}

// Release signing. Provide these via a local (git-ignored) gradle.properties or -P/env;
// never commit the keystore or passwords. If absent, the release build is left unsigned
// so CI can still assemble it.
val keystoreFileProp = providers.gradleProperty("PAY_KEYSTORE_FILE")
val keystorePasswordProp = providers.gradleProperty("PAY_KEYSTORE_PASSWORD")
val keyAliasProp = providers.gradleProperty("PAY_KEY_ALIAS")
val keyPasswordProp = providers.gradleProperty("PAY_KEY_PASSWORD")

android {
    namespace = "app.wayqo.wallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.wayqo.wallet"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode.get()
        versionName = appVersionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SOLANA_ALLOWED_MINTS", "\"${allowedSolanaMints.get()}\"")
        buildConfigField("String", "SOLANA_RPC_URL", "\"${solanaRpcUrl.get()}\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("fdroid") {
            dimension = "environment"
            // Independent signing and storage; never replaces a Play or sandbox wallet.
            applicationId = "app.wayqo.wallet.fdroid"
            resValue("string", "app_name", "WAYQO Open")
            manifestPlaceholders["kycScheme"] = "wayqo-fdroid"
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.get()}\"")
            buildConfigField("String", "IDENTITY_URI", "\"${identityUri.get()}\"")
            buildConfigField("String", "KYC_CALLBACK_URL", "\"wayqo-fdroid://kyc-complete\"")
            buildConfigField("String", "QR_DECODER", "\"ZXing-C++\"")
            buildConfigField("String", "EDITION", "\"WAYQO Open\"")
            buildConfigField("boolean", "OPEN_EDITION", "true")
        }
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    signingConfigs {
        create("release") {
            if (keystoreFileProp.isPresent) {
                storeFile = file(keystoreFileProp.get())
                storePassword = keystorePasswordProp.get()
                keyAlias = keyAliasProp.get()
                keyPassword = keyPasswordProp.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreFileProp.isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

// Exact resolved runtime coordinates for the publication license audit.
tasks.register("writeOpenDependencyInventory") {
    val output = layout.buildDirectory.file("reports/open-runtime-coordinates.txt")
    outputs.file(output)
    doLast {
        val coordinates = configurations.getByName("fdroidReleaseRuntimeClasspath")
            .resolvedConfiguration.resolvedArtifacts.map { it.moduleVersion.id.toString() }.distinct().sorted()
        output.get().asFile.apply { parentFile.mkdirs(); writeText(coordinates.joinToString("\n", postfix = "\n")) }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.biometric:biometric:1.1.0")
    // Do not rely on ML Kit to pull in a FragmentActivity version compatible
    // with the recovery file ActivityResult launchers in the free flavor.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.zxing:core:3.5.4")
    // ML Kit is the accuracy-first decoder. ZXing-C++ is a fast native fallback
    // for frames/images ML Kit misses (the combined device corpus result is 6300/6300).
    // 2.3.0 is the newest release compatible with the pinned Kotlin 2.0 compiler;
    // newer 3.x Android wrappers currently publish Kotlin 2.3 metadata.
    implementation("io.github.zxing-cpp:android:2.3.0")

    implementation("com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.7") {
        // This SDK publishes its unused JUnit KTX test dependency at runtime.
        // Its production classes do not reference JUnit; keep test tools out of the app.
        exclude(group = "androidx.test.ext")
    }
    // Ed25519 keygen/signing for the embedded non-custodial wallet (lightweight API,
    // pure-JVM, no JCA provider registration so it won't clash with Android's BC).
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("com.googlecode.libphonenumber:libphonenumber:9.0.40")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    // Recent Android builds removed reflective InputManager.getInstance; 3.7
    // uses the supported system service API when injecting UI test events.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseStorePath = providers.environmentVariable("LOCALPDF_KEYSTORE_PATH")
val releaseStorePassword = providers.environmentVariable("LOCALPDF_KEYSTORE_PASSWORD")
val releaseKeyPassword = providers.environmentVariable("LOCALPDF_KEY_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("LOCALPDF_KEY_ALIAS").orElse("localpdf-release")

android {
    namespace = "com.localpdf"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.localpdf"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("production") {
            if (releaseStorePath.isPresent && releaseStorePassword.isPresent && releaseKeyPassword.isPresent) {
                storeFile = file(releaseStorePath.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("production")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { task ->
        task.project == project && (task.name.contains("Release", ignoreCase = true) || task.name == "bundleRelease")
    }
    if (releaseRequested) {
        val missing = buildList {
            if (!releaseStorePath.isPresent) add("LOCALPDF_KEYSTORE_PATH")
            if (!releaseStorePassword.isPresent) add("LOCALPDF_KEYSTORE_PASSWORD")
            if (!releaseKeyPassword.isPresent) add("LOCALPDF_KEY_PASSWORD")
        }
        require(missing.isEmpty()) { "Release signing is unavailable. Set: ${missing.joinToString()}" }
        require(file(releaseStorePath.get()).isFile) { "LOCALPDF_KEYSTORE_PATH must point to an external keystore file" }
        require(!file(releaseStorePath.get()).canonicalPath.startsWith(rootDir.canonicalPath)) {
            "The production keystore must be stored outside the Git repository"
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":feature:library"))
    implementation(project(":feature:scanner"))
    implementation(project(":feature:search"))
    implementation(project(":feature:viewer"))
    implementation(project(":feature:ocr-edit"))
    implementation(project(":core:security"))
    implementation(project(":feature:vault"))
    implementation(project(":feature:redaction"))
    implementation(project(":feature:editor"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
}

tasks.register<Copy>("collectReleaseArtifacts") {
    dependsOn("assembleRelease", "bundleRelease")
    into(layout.buildDirectory.dir("outputs/release"))
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk")) { rename { "localpdf-0.1.0-universal-release.apk" } }
    from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab")) { rename { "localpdf-0.1.0-release.aab" } }
}

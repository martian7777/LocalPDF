plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android {
    namespace = "com.localpdf.core.cvscanner"; compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation(project(":core:model")); implementation(libs.opencv); implementation(libs.androidx.camera.core) }

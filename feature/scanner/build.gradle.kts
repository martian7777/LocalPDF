plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose) }
android {
    namespace = "com.localpdf.feature.scanner"; compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(project(":core:model")); implementation(project(":core:cv-scanner")); implementation(project(":core:designsystem"))
    implementation(libs.androidx.camera.core); implementation(libs.androidx.camera.camera2); implementation(libs.androidx.camera.lifecycle); implementation(libs.androidx.camera.view)
    implementation(libs.androidx.core.ktx); implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.material.icons.core)
}

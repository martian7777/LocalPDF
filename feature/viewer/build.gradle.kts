plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose) }
android { namespace = "com.localpdf.feature.viewer"; compileSdk = 35; defaultConfig { minSdk = 26 }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
dependencies {
    implementation(project(":core:model")); implementation(project(":core:data")); implementation(project(":core:designsystem")); implementation(project(":feature:ocr-edit")); implementation(project(":feature:redaction")); implementation(project(":feature:editor"))
    implementation(libs.androidx.lifecycle.viewmodel.compose); implementation(libs.androidx.lifecycle.runtime.compose); implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.material.icons.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

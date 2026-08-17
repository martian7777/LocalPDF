plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android {
    namespace = "com.localpdf.core.work"; compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(project(":core:model")); implementation(project(":core:database")); implementation(project(":core:ai-ocr")); implementation(project(":core:pdf"))
    implementation(libs.androidx.work.runtime.ktx); implementation(libs.kotlinx.coroutines.android)
}

plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android {
    namespace = "com.localpdf.core.data"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(project(":core:model")); implementation(project(":core:database")); implementation(project(":core:pdf")); implementation(project(":core:work"))
    implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.android)
}

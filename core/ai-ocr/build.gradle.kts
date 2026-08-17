plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android) }
android {
    namespace = "com.localpdf.core.aiocr"; compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    androidResources { noCompress += "onnx" }
}
dependencies {
    implementation(project(":core:model")); implementation(libs.onnxruntime.android); implementation(libs.opencv); implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}

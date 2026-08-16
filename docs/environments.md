# Environments & Build Configurations — LocalPDF

## Build Variants

LocalPDF defines three primary build variants:

1. **`debug`**:
   - `applicationIdSuffix = ".debug"`
   - `versionNameSuffix = "-DEBUG"`
   - `debuggable = true`
   - StrictMode and LeakCanary enabled
   - Timber verbose logging enabled
2. **`release`**:
   - `isMinifyEnabled = true` (R8 full mode)
   - `isShrinkResources = true`
   - ProGuard rules active for OpenCV, ONNX Runtime Mobile, and PDFBox-Android
   - Logs stripped, zero debugging hooks
3. **`benchmark`**:
   - Optimized for Baseline Profile generation and Macrobenchmark execution

## ABI Split Configuration

To keep download sizes small while including native OpenCV and ONNX Runtime libraries, configure ABI splits:

```kotlin
splits {
    abi {
        isEnable = true
        reset()
        include("arm64-v8a", "armeabi-v7a", "x86_64")
        isUniversalApk = true
    }
}
```

## ProGuard / R8 Rules

Crucial keep rules for native JNI and ONNX reflection:
```proguard
# ONNX Runtime Mobile
-keep class ai.onnxruntime.** { *; }
-keepattributes *Annotation*

# OpenCV
-keep class org.opencv.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# PDFBox-Android
-keep class com.tom_roush.pdfbox.** { *; }

# Room SQLite FTS
-keep class androidx.room.** { *; }
```

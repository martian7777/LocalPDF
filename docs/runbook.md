# Operations & Troubleshooting Runbook — LocalPDF

## Troubleshooting Common Issues

### 1. OpenCV Native Library Load Failure (`UnsatisfiedLinkError`)
- **Symptom**: Crash on camera open or scan filter application: `java.lang.UnsatisfiedLinkError: Couldn't load opencv_java4`.
- **Cause**: Missing native `.so` library for the device ABI (e.g. running an `arm64-v8a` only build on an `x86_64` emulator).
- **Remedy**: Ensure `splits.abi` includes the current ABI or test with `universalApk = true`. Verify `System.loadLibrary("opencv_java4")` is initialized in Application `onCreate`.

### 2. ONNX Runtime Out-Of-Memory (OOM) on Large Images
- **Symptom**: Native crash or memory heap exhaustion during multi-page batch OCR.
- **Cause**: Passing full uncompressed 300+ DPI 4K bitmaps directly into ONNX tensor buffers.
- **Remedy**: Ensure `OcrInferenceWorker` downsamples the image to a standardized long-edge limit (e.g. 1536px or 2048px) prior to tensor conversion, maintaining aspect ratio. Always release input/output `OrtValue` tensors promptly.

### 3. BiometricPrompt Authentication Fails / Key Invalidation
- **Symptom**: `KeyPermanentlyInvalidatedException` when user tries to open the Private Vault.
- **Cause**: User enrolled a new fingerprint or biometric credential in Android OS settings, causing Android Keystore to invalidate keys configured with `setUserAuthenticationRequired(true)`.
- **Remedy**: Handle `KeyPermanentlyInvalidatedException` gracefully in UI; prompt user to re-authenticate with their fallback master passphrase to re-encrypt and restore the vault key.

### 4. SQLite FTS5 Query Syntax Errors
- **Symptom**: SQLite exception when user types special characters like `*`, `-`, `OR`, `AND`, `"` in the search bar.
- **Cause**: Raw user text passed directly into FTS5 `MATCH` clause without query sanitization.
- **Remedy**: Always pass search queries through `FtsQuerySanitizer.sanitize(query)` to wrap terms in quotes and escape SQLite FTS syntax tokens.

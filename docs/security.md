# Security & Privacy Architecture — LocalPDF

## Core Security Tenet: Zero Cloud Egress

LocalPDF is built on the strict architectural premise of **100% on-device data processing**.
- No mandatory remote accounts or telemetry SDKs.
- No network transmission of scanned images, PDF binaries, or extracted OCR text.
- All AI inference (PaddleOCR, classification, entity extraction) runs locally via ONNX Runtime Mobile.

## Multi-Tiered Storage & Encryption Model

LocalPDF employs a two-tier storage model:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                          LOCALPDF APP SANDBOX                          │
│                                                                        │
│  ┌─────────────────────────────────┐   ┌────────────────────────────┐  │
│  │      STANDARD APP-PRIVATE       │   │       PRIVATE VAULT        │  │
│  │            STORAGE              │   │         (ENCRYPTED)        │  │
│  ├─────────────────────────────────┤   ├────────────────────────────┤  │
│  │ • context.filesDir/docs/        │   │ • EncryptedFile            │  │
│  │ • Linux UID sandbox isolation   │   │ • AES-256-GCM hardware key │  │
│  │ • Unencrypted on disk for fast  │   │ • BiometricPrompt required │  │
│  │   multi-page rendering          │   │ • Master key in Keystore   │  │
│  │ • Fast Room FTS5 indexing       │   │ • Auto-lock on app minimize│  │
│  └─────────────────────────────────┘   └────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### 1. Standard Tier (App-Private Sandbox)
- Documents, generated searchable PDFs, and page bitmaps reside in `context.filesDir/documents/`.
- Protected by standard Linux process UID isolation (inaccessible to other third-party apps on non-rooted devices).

### 2. Private Vault Tier (Hardware-Backed Encryption)
- Highly sensitive documents (National IDs, Passports, Tax Returns, Medical records) can be moved to the **Private Vault**.
- Encrypted using `androidx.security.crypto.EncryptedFile` with **AES-256-GCM** key encryption (`MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).setUserAuthenticationRequired(true)`).
- Decryption keys are stored inside the hardware-backed **Android Keystore** (utilizing StrongBox Keymaster where supported).
- Unlocking requires `BiometricPrompt` authentication (Class 3 Strong Biometrics or device PIN/pattern fallback).

## Verifiable Permanent Redaction

Many PDF apps produce "fake redaction" by merely drawing a black rectangle over text without removing the underlying text stream or image data. LocalPDF provides **True Permanent Redaction**:

1. **Raster/Bitmap Burning**: The pixel bounding box is permanently overwritten with solid color directly in the raw bitmap array.
2. **PDF Vector & Text Stream Stripping**: The PDF stream is parsed with `PDFBox-Android`; text characters, glyphs, and vector commands within the redacted bounding box are removed from the PDF content stream.
3. **Metadata & EXIF Sanitization**: Author tags, creation software, GPS location tags, and device metadata are stripped.
4. **Flattening Verification**: Redacted pages are exported as a flattened searchable layer, guaranteeing hidden data cannot be recovered via text-selection or PDF stream decoders.

## Safe Sharing Protocol

When sharing documents outside the app:
- Optional dynamic watermarking ("CONFIDENTIAL", "FOR VISA APPLICATION ONLY", "COPY").
- Temporary scoped `FileProvider` URIs (`content://...`) with `FLAG_GRANT_READ_URI_PERMISSION` that expire immediately upon consumption.
- Automatic warning when sharing unredacted documents containing detected sensitive fields (ID numbers, credit card numbers, bank accounts).

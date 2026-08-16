# Networking Architecture & Boundaries — LocalPDF

## Core Philosophy: Zero Mandatory Network

LocalPDF operates as a completely offline, zero-cloud application. The core document capture, OCR, classification, search, editing, redaction, and storage engines have zero network dependencies and will never make outbound HTTP/HTTPS calls during regular usage.

## Optional Network Boundaries

Network connectivity is strictly gated and restricted to explicitly user-initiated, optional scenarios:

1. **On-Demand Language Pack & Embedding Model Downloads**:
   - When a user requests an additional OCR language pack (e.g. Arabic, Urdu, Japanese) or advanced vector embedding model not bundled in the core APK assets.
   - Downloaded via secure HTTPS with SHA-256 integrity verification.
2. **User-Configured Custom WebDAV / Nextcloud Backup**:
   - Optional, user-configured remote backup endpoint.
   - Credentials encrypted via Android Keystore.
3. **In-App Update Checks & Release Notes** (Optional GitHub release query for F-Droid/standalone builds).

## Network Implementation Stack (Optional Features)

- **HTTP Client**: Ktor Client with OkHttp / Android engine.
- **Serialization**: Kotlinx Serialization JSON.
- **Strict Network Security Config**: Enforcing HTTPS / TLS 1.3 only, certificate pinning for official model downloads, and zero cleartext traffic (`android:usesCleartextTraffic="false"`).

## Privacy Guarantee & Leak Prevention

- Zero analytics SDKs, zero crash tracking telemetry uploading document data, zero third-party ad networks.
- Strict gating: No background network worker will ever access document contents or extracted OCR text.

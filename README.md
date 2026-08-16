# LocalPDF — Private Document AI

> **Scan, understand, search, protect, and act on your documents with 100% on-device privacy.**

LocalPDF is an enterprise-grade, privacy-first Android application that combines computer vision, on-device neural networks (PaddleOCR & ONNX Runtime Mobile), SQLite FTS5 full-text search, and hardware-backed Android Keystore encryption into a powerful offline document workstation.

---

## Key Features

- 📸 **Smart Document Scanner**: CameraX integration with OpenCV live edge detection, auto-shutter capture, perspective correction, shadow removal, and contrast filters.
- 🧠 **On-Device OCR & Intelligence**: PaddleOCR text detection (`DBNet`) & recognition (`CRNN/SVTR`) running locally via ONNX Runtime Mobile with zero cloud dependency.
- 🏷️ **Automatic Document Classification**: Automatic detection of Invoices, Receipts, Utility Bills, Bank Statements, Contracts, Passports, and IDs.
- ⚡ **Smart Field Extraction**: Automatically extracts financial totals, dates, vendor names, line items, IBANs, and reference numbers.
- 🔍 **SQLite FTS5 Full-Text Search**: Instant offline full-text search across all scanned pages and extracted fields with highlighted snippets.
- 🛡️ **Permanent Redaction & Private Vault**: True permanent raster/vector redaction and hardware-backed `AES-256-GCM` (`EncryptedFile`) vault protected by `BiometricPrompt`.
- 📑 **Comprehensive PDF Tools**: Merge, split, reorder, rotate, compress, and create searchable PDFs with invisible text layers.
- 📊 **Privacy Dashboard**: 100% local processing indicator, zero analytics trackers, and zero network egress.

---

## Tech Stack

| Category | Technologies |
|---|---|
| **Core & Language** | Kotlin 2.x, Coroutines, Flow, Kotlinx Serialization |
| **UI & Presentation** | Jetpack Compose, Material 3, Navigation Compose, MVI / UDF |
| **Dependency Injection** | Hilt (Dagger) |
| **Camera & Computer Vision**| CameraX, OpenCV 4.x (C++ NDK bindings) |
| **On-Device AI / ML** | ONNX Runtime Mobile, Quantized INT8 PaddleOCR, Custom Classifier |
| **PDF Engines** | Android `PdfRenderer` (UI viewer) + `PDFBox-Android` (manipulation) |
| **Database & Search** | Room, SQLite FTS5 (Full-Text Search) |
| **Security & Cryptography** | Android Keystore, `androidx.security.crypto.EncryptedFile` (AES-256-GCM), BiometricPrompt |
| **Background Work** | Jetpack WorkManager (Chained Expedited Coroutine Workers) |
| **Build & Tooling** | Gradle Kotlin DSL, KSP, R8 / ProGuard, GitHub Actions |

---

## Architecture Overview

LocalPDF follows a strict, feature-oriented multi-module architecture:

```text
:app                         # Application entry point, Hilt DI root, top-level navigation

:core
  ├── :core:common           # Dispatchers, Result monad, logging, coroutines
  ├── :core:model            # Pure Kotlin domain data models (Document, Page, OcrBlock, etc.)
  ├── :core:designsystem     # Material 3 themes, color tokens, reusable components
  ├── :core:ui               # Shared Compose UI elements (Zoomable canvas, PDF rendering)
  ├── :core:database         # Room database, DAOs, SQLite FTS5 virtual tables, migrations
  ├── :core:cv-scanner       # OpenCV 4.x native integration, contour detection, warp, filters
  ├── :core:ai-ocr           # ONNX Runtime Mobile, PaddleOCR text detection + recognition
  ├── :core:pdf              # Android PdfRenderer + PDFBox-Android manipulation pipeline
  ├── :core:security         # Android Keystore, EncryptedFile (AES-256-GCM), BiometricPrompt
  └── :core:work             # WorkManager worker definitions for batch processing

:feature
  ├── :feature:scanner       # CameraX capture session with live edge detection overlay
  ├── :feature:viewer        # High-performance PDF reader with selectable OCR text overlay
  ├── :feature:editor        # PDF page reorganization, merging, splitting, watermarking
  ├── :feature:ocr-edit      # Interactive OCR correction screen with side-by-side original image crop
  ├── :feature:redaction     # Privacy scanner & permanent redaction studio
  ├── :feature:search        # Full-text SQLite FTS5 search & smart structured filters
  ├── :feature:library       # Document list/grid, smart collections, favorites, tags
  ├── :feature:vault         # Biometric-gated encrypted document vault
  └── :feature:settings      # Privacy dashboard, storage management, export/backup
```

---

## Documentation Index

For in-depth engineering documentation, consult the `docs/` router:

- [Product Vision & Roadmap](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/product.md)
- [Android Architecture](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/architecture.md)
- [UI / UX Design System](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/ui-ux.md)
- [Presentation (MVI / UDF)](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/presentation.md)
- [Domain & Business Logic](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/domain.md)
- [Data & Repositories](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/data.md)
- [Database & SQLite FTS5](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/database.md)
- [Security, Privacy & Redaction](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/security.md)
- [Offline & Background WorkManager](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/offline-sync.md)
- [Performance & Memory Management](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/performance.md)
- [Networking Boundaries](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/networking.md)
- [Testing Strategy](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/testing.md)
- [Observability & Privacy Dashboard](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/observability.md)
- [Environments & Build Variants](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/environments.md)
- [Release & Play Store Strategy](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/release.md)
- [Operations Runbook](file:///d:/localpdf/kotlin-android-enterprise-ai-template-complete/docs/runbook.md)

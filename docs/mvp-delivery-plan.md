# MVP Delivery Plan

This plan turns the product roadmap into buildable, testable vertical slices. A phase is complete only after its quality gate passes; placeholder UI does not count as a completed engine feature.

## Phase 0 — Build Foundation (complete)

- Android application and Gradle wrapper
- `:core:model` and `:core:designsystem` boundaries
- responsive compact, medium, and expanded app shell
- reusable glass surface and light/dark color tokens
- camera permission rationale and settings recovery
- cleartext traffic disabled and backups disabled

Gate: `:app:assembleDebug` succeeds.

## Phase 1 — Local Library and Import

- Room document/page/tag schema with exported schema
- app-private document storage
- Storage Access Framework PDF/image import
- library UDF state, empty/loading/error/content states, grid/list adaptation
- document details and safe deletion
- repository, ViewModel, Room, and Compose tests

Gate: imported documents survive process death and remain usable offline.

## Phase 2 — Scanner and Image Enhancement

- CameraX preview and capture
- manual crop quadrilateral and multi-page tray
- OpenCV edge detection, perspective correction, filters, blur/light warnings
- bounded image memory and background processing
- permission, cancellation, rotation, and device-aspect tests

Gate: multi-page capture creates durable enhanced page images without main-thread work.

## Phase 3 — OCR, Extraction, Search, and PDF

- verified INT8 OCR model assets and ONNX lifecycle management
- OCR correction studio
- deterministic classification and invoice/receipt/bill entity extraction
- searchable PDF generation
- Room full-text index with sanitized queries
- WorkManager progress and retry behavior

Gate: a scanned page becomes a searchable, correctable PDF with no network access.

## Phase 4 — Privacy, Redaction, PDF Tools, and Vault

- sensitive entity detection and review
- pixel-burned flattened redaction with metadata removal and verification tests
- watermark/share flow through scoped `FileProvider` URIs
- merge, split, reorder, rotate, delete, and compression
- Keystore-backed vault and biometric/device-credential recovery states

Gate: redacted content cannot be recovered and vaulted files are unreadable outside an authenticated session.

## Phase 5 — Production Release

- unit, integration, Compose, lint, release-R8, and offline checks
- accessibility, font scaling, orientation, foldable/tablet, and dark-mode review
- release signing from non-committed environment secrets
- signed universal APK and Play AAB generation and signature verification

Gate: all P0/P1 findings are closed and release artifacts pass signature and install verification.


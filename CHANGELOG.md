# Changelog

All notable changes to **LocalPDF (Private Document AI)** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Smart Document Scanner** (`:feature:scanner`): CameraX viewfinder with real-time OpenCV contour detection, auto-shutter capture, perspective deskewing, and shadow removal filters.
- **On-Device OCR & Intelligence** (`:feature:ocr-edit` & `:core:ai-ocr`): Quantized INT8 PaddleOCR text detection (`DBNet`) and recognition (`CRNN/SVTR`) via ONNX Runtime Mobile.
- **Interactive OCR Correction Studio**: Side-by-side synchronized view with high-res original image crop and character confusion assistant (`0/O`, `1/I/l`, `5/S`).
- **Document Classification & Field Extraction**: Zero-shot on-device classifier for Invoices, Receipts, Bills, Contracts, IDs, and smart field extraction.
- **SQLite FTS5 Full-Text Search** (`:feature:search` & `:core:database`): Instant offline indexed search with match snippet extraction and category filters.
- **Privacy Scanner & Permanent Redaction** (`:feature:redaction`): PII detection, verifiable bitmap pixel burning, and PDFBox vector stream stripping.
- **Private Vault** (`:feature:vault` & `:core:security`): Hardware-backed Android Keystore `AES-256-GCM` (`EncryptedFile`) gated by `BiometricPrompt`.
- **PDF Manipulation Toolkit** (`:feature:editor` & `:core:pdf`): Dual-engine pipeline (`PdfRenderer` for Compose UI + `PDFBox-Android` for merge, split, watermark, and compress).
- **Privacy Dashboard** (`:feature:settings`): Local-processing counter, zero cloud egress guarantee, and index management.

### Security
- Implemented multi-tiered storage model with hardware-backed encryption.
- Zero analytics, zero advertising SDKs, and zero telemetry network calls.

---

## [1.0.0-alpha01] - 2026-08-17

### Added
- Initial project architecture scaffolding, engineering guardrails (`AGENTS.md`), and comprehensive technical documentation.
- Multi-module Gradle configuration structure for `:app`, `:core:*`, and `:feature:*`.
- GitHub Actions CI/CD workflows for linting, testing, and automated APK/AAB artifact builds.

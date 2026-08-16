## 📋 Description
Provide a concise summary of the changes introduced in this pull request and the problem/ticket they resolve.

Fixes #(issue number)

## 🧩 Type of Change
- [ ] 🐛 Bug fix (non-breaking change fixing an issue)
- [ ] ✨ New feature (non-breaking change adding functionality)
- [ ] ♻️ Architecture / Refactoring (non-breaking cleanup)
- [ ] ⚡ Performance optimization
- [ ] 🔒 Security / Privacy enhancement
- [ ] 📝 Documentation update

## 📦 Affected Modules
- [ ] `:app`
- [ ] `:core:common` / `:core:model` / `:core:designsystem` / `:core:ui`
- [ ] `:core:database` (Room / FTS5)
- [ ] `:core:cv-scanner` (OpenCV)
- [ ] `:core:ai-ocr` (ONNX / PaddleOCR)
- [ ] `:core:pdf` (`PdfRenderer` / `PDFBox-Android`)
- [ ] `:core:security` (Keystore / Vault / Biometrics)
- [ ] `:feature:scanner` / `:feature:viewer` / `:feature:editor`
- [ ] `:feature:ocr-edit` / `:feature:redaction` / `:feature:search` / `:feature:library`

## 🧪 Testing Performed
- [ ] Unit tests added / updated (`./gradlew test`)
- [ ] Static analysis & Lint verified (`./gradlew lint detekt`)
- [ ] Tested on physical device / emulator (API: ___)
- [ ] 100% offline verification (tested in Airplane mode with no network)
- [ ] Memory/leak check performed (no unreleased OpenCV `Mat` or ONNX tensor allocations)

## 🔒 Privacy & Security Checklist
- [ ] **Zero Cloud Egress**: Verified no outbound network requests are made with document data.
- [ ] **Permanent Redaction**: Verified that redactions permanently burn pixels and strip vector text streams.
- [ ] **No Secrets / PII in Logs**: Verified that logging statements contain no user file names or OCR text.

## 📸 Screenshots / UI Comparison
| Before | After |
|---|---|
| *(Image)* | *(Image)* |

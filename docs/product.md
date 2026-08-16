# Product — LocalPDF (Private Document AI)

## App Name

**LocalPDF — Private Document AI**

## Product Vision

LocalPDF is a 100% on-device, privacy-first Document AI platform for Android. It transforms everyday smartphones and tablets into intelligent document workstations where users can scan, clean, recognize (OCR), classify, extract structured data, search, redact, edit, and query documents without any data ever leaving their device.

The app bridges the gap between basic utility PDF scanners and modern AI intelligence by running quantized neural networks (PaddleOCR, custom ONNX classification, and local vector embeddings) locally on the device with zero cloud dependency, zero mandatory user accounts, and zero tracking.

## Product Positioning

> **"Private Document AI — scan, understand, search, protect, and act on your documents without giving up your privacy."**

While conventional PDF tools attract users for basic scan/edit operations, LocalPDF differentiates itself through:
1. **Uncompromising 100% On-Device AI**: Local OCR, field extraction, semantic search, and document reasoning.
2. **True Security & Smart Redaction**: Hardware-backed Keystore encryption, biometric vault, and verifiable permanent redaction.
3. **Deep Actionability**: Turning static scans into searchable PDFs, calendar reminders, structured spreadsheets, and actionable workflows.

## Primary Users

- **Privacy-Conscious Professionals**: Lawyers, financial advisors, consultants, and executives handling sensitive client contracts, agreements, and tax filings.
- **Medical & Healthcare Practitioners / Patients**: Managing prescriptions, medical diagnostic reports, and insurance claims with strict HIPAA/privacy guarantees.
- **Small Business Owners & Freelancers**: Scanning invoices, tracking receipts, extracting line items, categorizing expenses, and exporting to CSV/Excel.
- **Students & Academics**: Digitizing textbook pages, dewarping curved pages, extracting tables, and searching through study materials.
- **General Mobile Users**: Scanning IDs, passports, utility bills, and application forms with automatic smart naming and safe sharing watermarks.

## Main Problems Solved

1. **Cloud Privacy & Surveillance Risk**: Existing document scanner apps upload sensitive personal documents (passports, tax filings, medical reports) to remote servers for OCR and AI processing.
2. **Unstructured & Lost Information**: Mobile scans remain "dumb images" buried in camera rolls or PDF folders without searchable text, structured metadata, or actionable dates.
3. **Poor Scan & OCR Quality**: Skewed, curved book pages, shadows, low-contrast text, and OCR character confusion (`0/O`, `1/I/l`) lead to broken workflows.
4. **Data Leakage When Sharing**: Sending documents with visible social security numbers, bank details, home addresses, or hidden metadata/EXIF tags.
5. **Subscription Fatigue & Ad Bloat**: Commercial PDF utilities locked behind intrusive subscriptions and data-harvesting SDKs.

## Core Jobs To Be Done

1. **Capture & Enhance**: "When I snap a picture of a document or bill, I want it automatically detected, cropped, deskewed, shadow-removed, and enhanced so that it looks like a professional flatbed scan."
2. **Read & Understand (OCR + Extraction)**: "When I scan an invoice, receipt, or contract, I want text and structured key fields (totals, dates, vendor, line items, expiration) instantly extracted locally."
3. **Find Instantly**: "When I need a document from 6 months ago, I want to search by exact words, document type, amount, or natural questions ('Show electricity bills from last year') offline."
4. **Protect & Redact**: "When I share a proof of address or ID, I want sensitive numbers and signatures automatically detected and permanently redacted with an optional custom watermark."
5. **Organize & Act**: "When a bill or contract has a due date, I want automatic reminders, smart folder organization, and direct export to PDF/Excel."

## Primary User Journeys

### Journey 1: Scan, Clean, OCR & Smart Save
```text
Open App / Tap Camera
→ CameraX Live Edge Detection & Auto-Capture
→ OpenCV Pipeline (Crop, Deskew, Shadow Removal, Contrast Enhancement)
→ On-Device ONNX PaddleOCR Inference (Text Detection + Recognition)
→ Smart Document Classifier (Identifies "LESCO Utility Bill")
→ Entity Extractor (Extracts Due Date: Aug 28, Amount: PKR 14,500)
→ Auto-suggests Name "LESCO_Bill_August_2026.pdf"
→ Saves Searchable PDF + Indexes into Room FTS5
→ Prompts "Add Due Date Reminder to Calendar?"
```

### Journey 2: Safe Share & Permanent Redaction
```text
Select Document (e.g. National ID / Tax Form)
→ Privacy Scanner Detects Sensitive Entities (ID Number, DOB, Address)
→ User Reviews Suggested Redaction Regions in Compose Viewer
→ User Taps "Apply Permanent Redaction & Add Watermark 'FOR VISA USE ONLY'"
→ PDFBox Engine Flattens Pixels & Strips PDF Text Streams / Metadata / EXIF
→ Generated Shareable PDF Shared via Android System Share Sheet
```

### Journey 3: Multi-Document Search & Offline Question Answering
```text
User Opens Search Tab
→ Types "Total electricity spent in 2025" or searches "INV-4231"
→ SQLite FTS5 + Local Structured Entity Aggregator queries Room DB
→ Instantly returns matched documents, aggregated sums, and page snippets
→ Tapping snippet jumps directly to highlighted bounding box on the PDF page
```

## Feature Roadmap

### Phase 1: Core MVP (Current Scope)
- CameraX-based document scanner with live edge detection and auto-capture.
- OpenCV document cleanup: automatic cropping, perspective warp, deskewing, shadow removal, B&W/grayscale/color modes.
- On-device PaddleOCR (INT8 quantized ONNX models for text detection + text recognition).
- Interactive OCR mistake correction interface with side-by-side original image crop inspection.
- Searchable PDF generation (embedding invisible text layer over page images).
- Automatic document classification (Invoices, Receipts, Utility Bills, Contracts, IDs, Bank Statements, Notes, Generic).
- Smart field extraction for Invoices, Receipts, and Bills (vendor, invoice #, date, total amount, due date, line items).
- Smart filename suggestions (e.g., `Vendor_Type_Date.pdf`).
- Full-text document search powered by SQLite FTS5 in Room.
- Automatic smart collections and tagging.
- Privacy Scanner for sensitive information (IDs, phone numbers, emails, IBAN, bank accounts).
- Smart permanent redaction with visual flattening and metadata stripping.
- Safe sharing with preset watermarks ("CONFIDENTIAL", "FOR BANK USE ONLY", "COPY").
- Core PDF utilities: Merge, Split, Reorder pages, Rotate, Delete, and Compress.
- Multi-tiered security: Internal app sandbox storage + Biometric-protected Private Vault.
- 100% offline operation with Privacy Dashboard.

### Phase 2: Intelligence & Semantic Expansion
- Semantic search using local on-device vector embeddings.
- "Ask Your PDF" single-document question answering with bounding box source citations.
- "Ask Across Documents" multi-document search and cross-document comparison.
- AI-generated document summarization and legal/financial clause explanation.
- Contract analysis: obligation checklists, deadline extraction, and renewal alerts.
- Structured table extraction with export to CSV / Excel / JSON.
- Document comparison tool (visual side-by-side diff of two PDF versions).
- Document translation (English, Urdu, Arabic, Hindi, etc.) using offline translation models.

### Phase 3: Advanced Ecosystem & Workflows
- Multi-document cross-reasoning and automated personal knowledge base.
- Comprehensive expense analytics dashboard with monthly spending charts and merchant breakdowns.
- Document timelines (chronological milestone tracking across signed contracts and invoices).
- Advanced form detection, interactive fillable PDF form controls, and saved profile auto-fill.
- Visual duplicate and document revision versioning detection.

## Platform Requirements

- **Minimum SDK**: API 26 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)
- **Compile SDK**: API 35 (Android 15)
- **Device Support**: Phones, Foldables (tabletop & dual-screen postures), and Tablets (dual-pane master-detail layouts)
- **Orientation Support**: Portrait & Landscape adaptive layouts
- **Offline Requirement**: 100% Offline-First (No mandatory server, no accounts, zero telemetry)
- **Authentication**: Android Keystore + BiometricPrompt (Fingerprint / Face Unlock / Device PIN) for Private Vault & App Lock

## Success Metrics

- **OCR Accuracy**: > 97% character recognition accuracy on high-quality scans; < 500ms per page inference on mid-range devices.
- **Scan Latency**: < 100ms OpenCV edge detection and perspective correction pipeline.
- **Search Latency**: < 50ms FTS5 search query latency across 10,000+ indexed pages.
- **Zero Data Leakage**: 0 network egress calls during scanning, OCR, searching, editing, and redactions.
- **Redaction Integrity**: 100% verification that underlying redacted vector text and raw bitmap pixels are destroyed.

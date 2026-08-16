# Feature Specification — On-Device OCR & Intelligence

## Purpose

Execute 100% on-device optical character recognition (PaddleOCR INT8 ONNX models), document classification, structured field extraction (invoices, receipts, bills), and interactive OCR error correction.

## User Story

As a user with scanned documents or imported PDFs,
I want the app to automatically recognize all text, extract key fields (totals, dates, vendor), classify the document, and let me correct any OCR mistakes,
so that my documents become searchable, actionable, and structured.

## Scope

### In Scope
- ONNX Runtime Mobile inference for text detection (`DBNet`) and text recognition (`CRNN/SVTR`).
- Multi-language OCR support (English, digits, symbols; expandable on-demand).
- Bounding box extraction with word/line level confidence scores.
- Interactive side-by-side OCR correction studio (zoomable original image crop vs. editable text field).
- Document classification (Invoice, Receipt, Utility Bill, Bank Statement, Contract, ID, etc.).
- Entity extraction for financial amounts, invoice numbers, dates, emails, phones, IBANs.
- Automatic smart naming (e.g. `LESCO_Bill_August_2026.pdf`).

## User Flow

```text
Document Scanned or PDF Imported
→ On-Device PaddleOCR Detects & Recognizes Text Blocks
→ Classifier Identifies Category ("Invoice")
→ Entity Extractor Detects "Total: $420.00", "Due: Sept 15", "Vendor: Acme Corp"
→ Searchable PDF Generated with Embedded Text Layer
→ User Can Open "OCR Correction Studio" to Tap Low-Confidence Words & Edit
```

# Domain Architecture & Business Rules — LocalPDF

## Domain Models (`:core:model`)

The domain layer contains pure Kotlin business models and logic, completely independent of the Android framework, database, or UI libraries.

### Core Entities

```kotlin
data class Document(
    val id: String,
    val title: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val mimeType: String,
    val classification: DocumentClassification,
    val isVaulted: Boolean,
    val isFavorite: Boolean,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class DocumentClassification {
    INVOICE,
    RECEIPT,
    UTILITY_BILL,
    BANK_STATEMENT,
    CONTRACT,
    AGREEMENT,
    ID_CARD,
    PASSPORT,
    DRIVING_LICENSE,
    BUSINESS_CARD,
    CERTIFICATE,
    ACADEMIC,
    MEDICAL_REPORT,
    PRESCRIPTION,
    TAX_DOCUMENT,
    APPLICATION_FORM,
    HANDWRITTEN_NOTES,
    GENERIC_PDF
}

data class DocumentPage(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val imagePath: String,
    val widthPx: Int,
    val heightPx: Int,
    val ocrBlocks: List<OcrBlock>,
    val extractedEntities: List<ExtractedEntity>
)

data class OcrBlock(
    val id: String,
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val blockType: OcrBlockType,
    val language: String? = null
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class ExtractedEntity(
    val id: String,
    val type: EntityType,
    val rawValue: String,
    val normalizedValue: String?,
    val confidence: Float,
    val boundingBox: BoundingBox?
)

enum class EntityType {
    VENDOR_NAME,
    INVOICE_NUMBER,
    TOTAL_AMOUNT,
    SUBTOTAL_AMOUNT,
    TAX_AMOUNT,
    CURRENCY,
    ISSUE_DATE,
    DUE_DATE,
    EXPIRY_DATE,
    PERSON_NAME,
    EMAIL_ADDRESS,
    PHONE_NUMBER,
    IBAN,
    BANK_ACCOUNT_NUMBER,
    NATIONAL_ID_NUMBER,
    ADDRESS,
    LINE_ITEM
}
```

## Domain Use Cases

Use cases encapsulate single units of business logic and orchestrate repositories:

1. **`ScanAndProcessDocumentUseCase`**:
   - Takes raw scanned page bitmaps from CameraX/OpenCV.
   - Executes OpenCV enhancement (crop, deskew, shadow filter).
   - Triggers on-device ONNX PaddleOCR inference.
   - Executes `ClassifyDocumentUseCase` and `ExtractEntitiesUseCase`.
   - Generates searchable PDF with invisible text layer via PDFBox.
   - Saves document records to Room and indexes full-text content into SQLite FTS5.

2. **`RedactAndFlattenDocumentUseCase`**:
   - Takes target document ID and list of `RedactionRegion` bounding boxes.
   - Renders page images and burns opaque black/white rectangles directly into the raw pixel bitmap.
   - Rebuilds PDF with PDFBox, stripping any underlying text streams, vector paths, and metadata inside the redacted coordinates.
   - Creates a verifiable sanitized copy.

3. **`SearchDocumentsUseCase`**:
   - Orchestrates multi-term full-text search against Room SQLite FTS5 index.
   - Applies structured filters (document category, date ranges, minimum/maximum financial amounts).
   - Generates highlighted text snippets with exact matching positions.

4. **`ManipulatePdfUseCase`**:
   - Operations: Merge multiple PDFs, split ranges, rotate pages, delete pages, reorder pages, compress PDF stream objects.

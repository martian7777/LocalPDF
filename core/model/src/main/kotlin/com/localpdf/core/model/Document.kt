package com.localpdf.core.model

import java.time.Instant

data class Document(
    val id: String,
    val title: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val mimeType: String = "application/pdf",
    val classification: DocumentClassification = DocumentClassification.GENERIC_PDF,
    val isVaulted: Boolean = false,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class DocumentPage(
    val id: String,
    val documentId: String,
    val pageIndex: Int,
    val imagePath: String?,
    val widthPx: Int,
    val heightPx: Int,
    val ocrBlocks: List<OcrBlock> = emptyList(),
    val extractedEntities: List<ExtractedEntity> = emptyList(),
)

data class OcrBlock(
    val id: String,
    val pageId: String,
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val language: String = "en",
)

data class BoundingBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

data class ExtractedEntity(
    val id: String,
    val pageId: String,
    val type: EntityType,
    val rawValue: String,
    val normalizedValue: String? = null,
    val confidence: Float,
    val boundingBox: BoundingBox? = null,
)

enum class EntityType { VENDOR, INVOICE_NUMBER, DATE, DUE_DATE, CURRENCY, TOTAL, EMAIL, PHONE, IBAN, BANK_ACCOUNT }

enum class ProcessingState { IMPORTING, READY, PROCESSING, FAILED }

enum class DocumentSort { UPDATED_DESC, CREATED_DESC, TITLE_ASC, SIZE_DESC }

data class DocumentFilter(
    val includeVaulted: Boolean = false,
    val sort: DocumentSort = DocumentSort.UPDATED_DESC,
)

data class SearchFilter(
    val classifications: Set<DocumentClassification> = emptySet(),
    val tags: Set<String> = emptySet(),
    val minimumAmount: Double? = null,
    val maximumAmount: Double? = null,
)

data class SearchResult(val document: Document, val snippet: String, val pageIndex: Int?)

data class RedactionRegion(val pageIndex: Int, val bounds: BoundingBox)

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
    GENERIC_PDF,
}

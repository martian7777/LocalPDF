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


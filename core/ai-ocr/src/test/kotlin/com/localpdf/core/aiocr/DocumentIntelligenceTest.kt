package com.localpdf.core.aiocr

import com.localpdf.core.model.DocumentClassification
import com.localpdf.core.model.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentIntelligenceTest {
    @Test fun invoiceClassificationAndFields() {
        val text = "INVOICE Invoice No INV-4231 Date 2026-08-17 Total USD 420.00 support@example.com"
        assertEquals(DocumentClassification.INVOICE, DocumentIntelligence.classify(text))
        val fields = DocumentIntelligence.extract(text)
        assertTrue(fields.any { it.type == EntityType.INVOICE_NUMBER && it.value == "INV-4231" })
        assertTrue(fields.any { it.type == EntityType.TOTAL && it.value == "420.00" })
    }
}

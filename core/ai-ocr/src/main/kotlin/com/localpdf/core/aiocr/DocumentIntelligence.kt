package com.localpdf.core.aiocr

import com.localpdf.core.model.DocumentClassification
import com.localpdf.core.model.EntityType

data class DetectedField(val type: EntityType, val value: String)

object DocumentIntelligence {
    fun classify(text: String): DocumentClassification {
        val normalized = text.lowercase()
        return when {
            listOf("invoice", "invoice no", "bill to").count(normalized::contains) >= 2 -> DocumentClassification.INVOICE
            "receipt" in normalized || ("cash" in normalized && "total" in normalized) -> DocumentClassification.RECEIPT
            listOf("utility", "meter", "units consumed", "due date").count(normalized::contains) >= 2 -> DocumentClassification.UTILITY_BILL
            "bank statement" in normalized || ("account" in normalized && "balance" in normalized) -> DocumentClassification.BANK_STATEMENT
            "agreement" in normalized || "contract" in normalized -> DocumentClassification.CONTRACT
            else -> DocumentClassification.GENERIC_PDF
        }
    }

    fun extract(text: String): List<DetectedField> = buildList {
        find(INVOICE, text)?.let { add(DetectedField(EntityType.INVOICE_NUMBER, it)) }
        EMAIL.findAll(text).forEach { add(DetectedField(EntityType.EMAIL, it.value)) }
        PHONE.findAll(text).forEach { add(DetectedField(EntityType.PHONE, it.value.trim())) }
        IBAN.findAll(text.uppercase()).forEach { add(DetectedField(EntityType.IBAN, it.value.replace(" ", ""))) }
        TOTAL.findAll(text).lastOrNull()?.groupValues?.get(1)?.let { add(DetectedField(EntityType.TOTAL, it)) }
        DATE.findAll(text).forEach { add(DetectedField(EntityType.DATE, it.value)) }
    }.distinctBy { it.type to it.value }

    private fun find(regex: Regex, text: String) = regex.find(text)?.groupValues?.get(1)
    private val INVOICE = Regex("(?i)(?:invoice|inv)(?:\\s*(?:number|no\\.?|#|:|-))*\\s+([A-Z0-9-]*\\d[A-Z0-9-]*)")
    private val EMAIL = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    private val PHONE = Regex("(?<!\\d)(?:\\+?\\d[\\d ()-]{7,}\\d)(?!\\d)")
    private val IBAN = Regex("\\b[A-Z]{2}\\d{2}(?: ?[A-Z0-9]){11,30}\\b")
    private val TOTAL = Regex("(?i)(?:grand )?total[^0-9]{0,12}(?:[A-Z]{3}|[$€£₨])?\\s*([0-9][0-9,]*(?:\\.[0-9]{2})?)")
    private val DATE = Regex("\\b(?:\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4})\\b")
}

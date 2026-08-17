package com.localpdf.core.data

import android.content.Context
import com.localpdf.core.database.*
import com.localpdf.core.model.*
import com.localpdf.core.pdf.PdfEngine
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RedactionRepository { suspend fun createPermanentCopy(documentId: String, regions: List<RedactionRegion>): Result<Document> }

class OfflineRedactionRepository private constructor(private val context: Context) : RedactionRepository {
    private val dao = LocalPdfDatabase.create(context).documentDao()
    override suspend fun createPermanentCopy(documentId: String, regions: List<RedactionRegion>) = runCatching { withContext(Dispatchers.IO) {
        val source = requireNotNull(dao.getById(documentId)); require(!source.isVaulted) { "Restore the document before redacting it" }
        val pages = dao.getPages(documentId); val blocks = dao.getOcrBlocks(documentId).groupBy { it.pageId }
        require(pages.all { it.imagePath != null }) { "Document pages are still processing" }
        val newId = UUID.randomUUID().toString(); val directory = File(context.filesDir, "documents/$newId").apply { mkdirs() }
        try {
            val modelBlocks = pages.map { page -> File(page.imagePath!!) to blocks[page.id].orEmpty().map { OcrBlock(it.id, it.pageId, it.text, it.confidence, BoundingBox(it.left, it.top, it.right, it.bottom), it.language) } }
            val (pdf, safePages) = PdfEngine.createRedactedPdf(modelBlocks, regions, File(directory, "redacted.pdf"), File(directory, "pages"))
            val hash = MessageDigest.getInstance("SHA-256").digest(pdf.readBytes()).joinToString("") { "%02x".format(it) }; val now = System.currentTimeMillis()
            val entity = DocumentEntity(newId, "${source.title} — redacted", pdf.absolutePath, pdf.length(), safePages.size, "application/pdf", source.classification, false, false, ProcessingState.READY.name, null, hash, now, now)
            val pageEntities = safePages.mapIndexed { index, pair -> PageEntity("$newId-$index", newId, index, pair.first.absolutePath, pages[index].widthPx, pages[index].heightPx) }
            dao.insertComplete(entity, pageEntities, listOf(TagEntity(newId, "redacted-copy")))
            safePages.forEachIndexed { index, pair -> dao.insertOcrBlocks(pair.second.map { block -> OcrBlockEntity(UUID.randomUUID().toString(), "$newId-$index", block.text, block.confidence, block.boundingBox.left, block.boundingBox.top, block.boundingBox.right, block.boundingBox.bottom, block.language) }) }
            val safeText = safePages.flatMap { it.second }.joinToString("\n") { it.text }; dao.replaceFts(DocumentFtsEntity(documentId = newId, title = entity.title, content = safeText, classification = entity.classification, keywords = "redacted"))
            Document(newId, entity.title, pdf.absolutePath, pdf.length(), safePages.size, "application/pdf", DocumentClassification.valueOf(entity.classification), tags = listOf("redacted-copy"), createdAt = java.time.Instant.ofEpochMilli(now), updatedAt = java.time.Instant.ofEpochMilli(now))
        } catch (error: Throwable) { directory.deleteRecursively(); throw error }
    } }
    companion object { fun create(context: Context): RedactionRepository = OfflineRedactionRepository(context.applicationContext) }
}

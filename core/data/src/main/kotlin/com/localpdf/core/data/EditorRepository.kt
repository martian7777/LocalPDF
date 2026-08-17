package com.localpdf.core.data

import android.content.Context
import com.localpdf.core.database.DocumentEntity
import com.localpdf.core.database.LocalPdfDatabase
import com.localpdf.core.database.PageEntity
import com.localpdf.core.database.TagEntity
import com.localpdf.core.model.Document
import com.localpdf.core.model.DocumentClassification
import com.localpdf.core.model.ProcessingState
import com.localpdf.core.pdf.DocumentInspector
import com.localpdf.core.pdf.PdfEngine
import com.localpdf.core.work.DocumentProcessingWorker
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EditorPageEdit(val pageId: String, val rotationDegrees: Int = 0, val deleted: Boolean = false)

interface EditorRepository {
    suspend fun createEditedCopy(documentId: String, pageEdits: List<EditorPageEdit>, watermark: String?, quality: Int = 90): Result<Document>
}

class OfflineEditorRepository private constructor(private val context: Context) : EditorRepository {
    private val dao = LocalPdfDatabase.create(context).documentDao()

    override suspend fun createEditedCopy(documentId: String, pageEdits: List<EditorPageEdit>, watermark: String?, quality: Int) = runCatching { withContext(Dispatchers.IO) {
        val source = requireNotNull(dao.getById(documentId)); require(!source.isVaulted) { "Restore the document before editing it" }
        val pagesById = dao.getPages(documentId).associateBy { it.id }
        val kept = pageEdits.filterNot { it.deleted }
        require(kept.isNotEmpty()) { "Keep at least one page" }
        val editPages = kept.map { edit ->
            val page = requireNotNull(pagesById[edit.pageId]) { "Page no longer exists" }
            val imagePath = requireNotNull(page.imagePath) { "Document pages are still processing" }
            PdfEngine.EditPage(File(imagePath), edit.rotationDegrees)
        }
        val newId = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "documents/$newId").apply { mkdirs() }
        try {
            val pdf = PdfEngine.createEditedPdf(editPages, File(directory, "edited.pdf"), watermark, quality)
            val hash = MessageDigest.getInstance("SHA-256").digest(pdf.readBytes()).joinToString("") { "%02x".format(it) }
            val inspected = DocumentInspector.inspect(pdf, "application/pdf")
            val now = System.currentTimeMillis()
            val entity = DocumentEntity(newId, "${source.title} — edited", pdf.absolutePath, pdf.length(), inspected.pageCount, "application/pdf", source.classification, false, false, ProcessingState.READY.name, null, hash, now, now)
            val pageEntities = List(inspected.pageCount) { index -> PageEntity("$newId-$index", newId, index, null, inspected.widthPx, inspected.heightPx) }
            dao.insertComplete(entity, pageEntities, listOf(TagEntity(newId, "edited-copy")))
            DocumentProcessingWorker.enqueue(context, newId)
            Document(newId, entity.title, pdf.absolutePath, pdf.length(), inspected.pageCount, "application/pdf", DocumentClassification.valueOf(entity.classification), tags = listOf("edited-copy"), createdAt = Instant.ofEpochMilli(now), updatedAt = Instant.ofEpochMilli(now))
        } catch (error: Throwable) { directory.deleteRecursively(); throw error }
    } }

    companion object { fun create(context: Context): EditorRepository = OfflineEditorRepository(context.applicationContext) }
}

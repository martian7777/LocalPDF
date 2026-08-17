package com.localpdf.core.work

import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.localpdf.core.aiocr.DocumentIntelligence
import com.localpdf.core.aiocr.OnDeviceOcrEngine
import com.localpdf.core.database.DocumentFtsEntity
import com.localpdf.core.database.ExtractedEntityEntity
import com.localpdf.core.database.LocalPdfDatabase
import com.localpdf.core.database.OcrBlockEntity
import com.localpdf.core.model.ProcessingState
import com.localpdf.core.pdf.PdfEngine
import java.io.File
import java.util.UUID

class DocumentProcessingWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val dao = LocalPdfDatabase.create(applicationContext).documentDao()
        val document = dao.getById(id) ?: return Result.failure()
        return try {
            dao.updateProcessing(id, document.classification, ProcessingState.PROCESSING.name, null, System.currentTimeMillis())
            val directory = File(document.filePath).let { if (it.isDirectory) it else it.parentFile!! }
            val source = File(document.filePath)
            val imageFiles = if (document.mimeType == "application/pdf") PdfEngine.renderPages(source, document.mimeType, File(directory, "rendered")) else dao.getPages(id).mapNotNull { it.imagePath?.let(::File) }
            require(imageFiles.isNotEmpty()) { "No readable pages" }
            val pages = dao.getPages(id)
            val allBlocks = OnDeviceOcrEngine(applicationContext).use { engine ->
                imageFiles.mapIndexed { index, image ->
                    val page = pages[index]
                    val dimensions = BitmapFactory.Options().also { it.inJustDecodeBounds = true; BitmapFactory.decodeFile(image.absolutePath, it) }
                    dao.updatePageImage(page.id, image.absolutePath, dimensions.outWidth, dimensions.outHeight)
                    engine.recognizePage(image.absolutePath, page.id).also { blocks ->
                        dao.insertOcrBlocks(blocks.map { OcrBlockEntity(it.id, it.pageId, it.text, it.confidence, it.boundingBox.left, it.boundingBox.top, it.boundingBox.right, it.boundingBox.bottom, it.language) })
                    }
                }
            }
            val text = allBlocks.flatten().joinToString("\n") { it.text }
            val classification = DocumentIntelligence.classify(text)
            val fields = DocumentIntelligence.extract(text)
            val firstPageId = pages.first().id
            dao.insertEntities(fields.map { ExtractedEntityEntity(UUID.randomUUID().toString(), firstPageId, it.type.name, it.value) })
            dao.updateProcessing(id, classification.name, ProcessingState.PROCESSING.name, null, System.currentTimeMillis())
            dao.replaceFts(DocumentFtsEntity(documentId = id, title = document.title, content = text, classification = classification.name, keywords = fields.joinToString(" ") { it.value }))
            val output = PdfEngine.createSearchablePdf(imageFiles.zip(allBlocks), File(directory, "searchable.pdf"))
            dao.updateOutput(id, output.absolutePath, output.length(), ProcessingState.READY.name, System.currentTimeMillis())
            Result.success()
        } catch (error: Throwable) {
            dao.updateProcessing(id, document.classification, ProcessingState.FAILED.name, error.message?.take(300), System.currentTimeMillis())
            if (runAttemptCount < 2) Result.retry() else Result.failure(Data.Builder().putString("error", error.message).build())
        }
    }

    companion object {
        const val KEY_ID = "document_id"
        fun enqueue(context: Context, documentId: String) {
            val request = OneTimeWorkRequestBuilder<DocumentProcessingWorker>().setInputData(Data.Builder().putString(KEY_ID, documentId).build()).addTag("document_process_$documentId").build()
            WorkManager.getInstance(context).enqueueUniqueWork("document_process_$documentId", ExistingWorkPolicy.KEEP, request)
        }
    }
}

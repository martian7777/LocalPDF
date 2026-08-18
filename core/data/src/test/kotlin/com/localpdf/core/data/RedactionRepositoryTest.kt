package com.localpdf.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.localpdf.core.database.DocumentDao
import com.localpdf.core.database.DocumentEntity
import com.localpdf.core.database.LocalPdfDatabase
import com.localpdf.core.database.OcrBlockEntity
import com.localpdf.core.database.PageEntity
import com.localpdf.core.model.BoundingBox
import com.localpdf.core.model.ProcessingState
import com.localpdf.core.model.RedactionRegion
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RedactionRepositoryTest {
    private lateinit var context: Context
    private lateinit var dao: DocumentDao
    private lateinit var repository: RedactionRepository
    private lateinit var documentId: String

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dao = LocalPdfDatabase.create(context).documentDao()
        repository = OfflineRedactionRepository.create(context)
        documentId = runBlocking { seedDocument() }
    }

    private fun createPageFile(index: Int): File {
        val directory = File(context.filesDir, "documents/source/pages").apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawText("Line $index", 10f, 30f, Paint().apply { color = Color.BLACK; textSize = 20f })
        }
        val file = File(directory, "page-$index.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()
        return file
    }

    private suspend fun seedDocument(): String {
        val id = "doc-1"
        val page0 = createPageFile(0)
        val page1 = createPageFile(1)
        val document = DocumentEntity(id, "Statement", "/data/doc-1/source.pdf", 100, 2, "application/pdf", "GENERIC_PDF", false, false, ProcessingState.READY.name, null, "hash-$id", 1_000, 1_000)
        val pages = listOf(
            PageEntity("$id-0", id, 0, page0.absolutePath, 200, 300),
            PageEntity("$id-1", id, 1, page1.absolutePath, 200, 300),
        )
        dao.insertComplete(document, pages, emptyList())
        dao.insertOcrBlocks(
            listOf(
                OcrBlockEntity("block-secret", "$id-0", "Account 1234-5678", 0.9f, 0.05f, 0.05f, 0.6f, 0.15f, "en"),
                OcrBlockEntity("block-safe", "$id-1", "Thank you for your business", 0.9f, 0.05f, 0.05f, 0.6f, 0.15f, "en"),
            ),
        )
        return id
    }

    @Test fun redactedCopyDropsOverlappingBlocksAndKeepsSafeOnes() = runBlocking {
        val regions = listOf(RedactionRegion(0, BoundingBox(0f, 0f, 0.7f, 0.2f)))

        val result = repository.createPermanentCopy(documentId, regions)

        assertTrue(result.isSuccess)
        val redacted = result.getOrThrow()
        assertEquals(listOf("redacted-copy"), redacted.tags)
        assertEquals(2, redacted.pageCount)
        assertNotEquals(documentId, redacted.id)

        val blocks = dao.getOcrBlocks(redacted.id)
        assertTrue(blocks.none { it.text.contains("Account") })
        assertTrue(blocks.any { it.text.contains("Thank you") })
    }

    @Test fun vaultedDocumentCannotBeRedacted() = runBlocking {
        val vaultedId = "doc-vaulted"
        val document = DocumentEntity(vaultedId, "Vaulted", "/data/vaulted.lpv", 50, 1, "application/pdf", "GENERIC_PDF", true, false, ProcessingState.READY.name, null, "hash-vaulted", 1_000, 1_000)
        dao.insertComplete(document, emptyList(), emptyList())

        val result = repository.createPermanentCopy(vaultedId, listOf(RedactionRegion(0, BoundingBox(0f, 0f, 1f, 1f))))

        assertTrue(result.isFailure)
    }

    @Test fun emptyRegionsAreRejected() = runBlocking {
        val result = repository.createPermanentCopy(documentId, emptyList())
        assertTrue(result.isFailure)
    }
}

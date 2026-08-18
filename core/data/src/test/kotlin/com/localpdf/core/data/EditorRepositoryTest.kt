package com.localpdf.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.localpdf.core.database.DocumentDao
import com.localpdf.core.database.DocumentEntity
import com.localpdf.core.database.LocalPdfDatabase
import com.localpdf.core.database.PageEntity
import com.localpdf.core.model.ProcessingState
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
class EditorRepositoryTest {
    private lateinit var context: Context
    private lateinit var dao: DocumentDao
    private lateinit var repository: EditorRepository
    private lateinit var documentId: String
    private lateinit var pageIds: List<String>

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        dao = LocalPdfDatabase.create(context).documentDao()
        repository = OfflineEditorRepository.create(context)
        val seeded = runBlocking { seedDocument() }
        documentId = seeded.first
        pageIds = seeded.second
    }

    private fun createPageFile(index: Int): File {
        val directory = File(context.filesDir, "documents/source/pages").apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(if (index % 2 == 0) Color.WHITE else Color.LTGRAY)
        val file = File(directory, "page-$index.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()
        return file
    }

    private suspend fun seedDocument(): Pair<String, List<String>> {
        val id = "doc-1"
        val page0 = createPageFile(0)
        val page1 = createPageFile(1)
        val document = DocumentEntity(id, "Statement", "/data/doc-1/source.pdf", 100, 2, "application/pdf", "GENERIC_PDF", false, false, ProcessingState.READY.name, null, "hash-$id", 1_000, 1_000)
        val pages = listOf(
            PageEntity("$id-0", id, 0, page0.absolutePath, 200, 300),
            PageEntity("$id-1", id, 1, page1.absolutePath, 200, 300),
        )
        dao.insertComplete(document, pages, emptyList())
        return id to pages.map { it.id }
    }

    @Test fun editedCopyKeepsOnlyNonDeletedPages() = runBlocking {
        val edits = listOf(
            EditorPageEdit(pageIds[0], rotationDegrees = 90, deleted = false),
            EditorPageEdit(pageIds[1], rotationDegrees = 0, deleted = true),
        )

        val result = repository.createEditedCopy(documentId, edits, watermark = "CONFIDENTIAL", quality = 90)

        assertTrue(result.isSuccess)
        val edited = result.getOrThrow()
        assertEquals(listOf("edited-copy"), edited.tags)
        assertEquals(1, edited.pageCount)
        assertNotEquals(documentId, edited.id)
        assertTrue(File(edited.filePath).length() > 0)
    }

    @Test fun deletingAllPagesFails() = runBlocking {
        val edits = pageIds.map { EditorPageEdit(it, deleted = true) }

        val result = repository.createEditedCopy(documentId, edits, watermark = null, quality = 90)

        assertTrue(result.isFailure)
    }

    @Test fun vaultedDocumentCannotBeEdited() = runBlocking {
        val vaultedId = "doc-vaulted"
        dao.insertComplete(
            DocumentEntity(vaultedId, "Vaulted", "/data/vaulted.lpv", 50, 1, "application/pdf", "GENERIC_PDF", true, false, ProcessingState.READY.name, null, "hash-vaulted", 1_000, 1_000),
            emptyList(),
            emptyList(),
        )

        val result = repository.createEditedCopy(vaultedId, listOf(EditorPageEdit("missing-page")), watermark = null, quality = 90)

        assertTrue(result.isFailure)
    }
}

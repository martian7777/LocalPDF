package com.localpdf.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentDaoTest {
    private lateinit var database: LocalPdfDatabase
    private lateinit var dao: DocumentDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LocalPdfDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.documentDao()
    }

    @After
    fun closeDatabase() { database.close() }

    private fun document(id: String, title: String, hash: String, vaulted: Boolean = false) = DocumentEntity(
        id, title, "/data/$id.pdf", 1024L, 1, "application/pdf", "GENERIC_PDF",
        isVaulted = vaulted, isFavorite = false, processingState = "READY", failureReason = null,
        contentHash = hash, createdAt = 1_000L, updatedAt = 1_000L,
    )

    @Test fun insertCompleteInsertsDocumentPagesAndTags() = runTest {
        val doc = document("doc-1", "Invoice", "hash-1")
        val pages = listOf(PageEntity("doc-1-0", "doc-1", 0, "/data/doc-1/page-0.jpg", 800, 1200))
        dao.insertComplete(doc, pages, listOf(TagEntity("doc-1", "invoice")))

        assertEquals(doc, dao.getById("doc-1"))
        assertEquals(pages, dao.getPages("doc-1"))
        assertEquals(listOf(TagEntity("doc-1", "invoice")), dao.observeTags().first())
    }

    @Test fun findIdByHashDetectsDuplicates() = runTest {
        dao.insertComplete(document("doc-1", "Invoice", "same-hash"), emptyList(), emptyList())
        assertEquals("doc-1", dao.findIdByHash("same-hash"))
        assertNull(dao.findIdByHash("other-hash"))
    }

    @Test fun observeLibraryExcludesVaultedDocuments() = runTest {
        dao.insertComplete(document("doc-1", "Library doc", "hash-1", vaulted = false), emptyList(), emptyList())
        dao.insertComplete(document("doc-2", "Vault doc", "hash-2", vaulted = true), emptyList(), emptyList())

        val library = dao.observeLibrary().first()
        assertEquals(1, library.size)
        assertEquals("doc-1", library.first().id)

        val vault = dao.observeVault().first()
        assertEquals(1, vault.size)
        assertEquals("doc-2", vault.first().id)
    }

    @Test fun deletingDocumentCascadesToPagesAndOcrBlocks() = runTest {
        dao.insertComplete(document("doc-1", "Invoice", "hash-1"), listOf(PageEntity("doc-1-0", "doc-1", 0, null, 800, 1200)), emptyList())
        dao.insertOcrBlocks(listOf(OcrBlockEntity("block-1", "doc-1-0", "Total 42.00", 0.9f, 0f, 0f, 1f, 1f, "en")))

        assertEquals(1, dao.delete("doc-1"))

        assertTrue(dao.getPages("doc-1").isEmpty())
        assertTrue(dao.getOcrBlocks("doc-1").isEmpty())
    }

    @Test fun updateOcrTextRewritesStoredBlock() = runTest {
        dao.insertComplete(document("doc-1", "Invoice", "hash-1"), listOf(PageEntity("doc-1-0", "doc-1", 0, null, 800, 1200)), emptyList())
        dao.insertOcrBlocks(listOf(OcrBlockEntity("block-1", "doc-1-0", "Toatl 42.00", 0.4f, 0f, 0f, 1f, 1f, "en")))

        assertEquals(1, dao.updateOcrText("block-1", "Total 42.00"))
        assertEquals("Total 42.00", dao.getOcrBlocks("doc-1").single().text)
        assertEquals(0, dao.updateOcrText("missing-block", "x"))
    }

    @Test fun searchMatchesSanitizedFtsQuery() = runTest {
        dao.insertComplete(document("doc-1", "Acme Invoice", "hash-1"), emptyList(), emptyList())
        dao.insertComplete(document("doc-2", "Unrelated Receipt", "hash-2", vaulted = true), emptyList(), emptyList())
        dao.replaceFts(DocumentFtsEntity(documentId = "doc-1", title = "Acme Invoice", content = "Total 420.00 due September", classification = "INVOICE", keywords = "420.00"))
        dao.replaceFts(DocumentFtsEntity(documentId = "doc-2", title = "Unrelated Receipt", content = "grocery items", classification = "RECEIPT", keywords = ""))

        val results = dao.search("\"420.00\"").first()

        assertEquals(1, results.size)
        assertEquals("doc-1", results.first().id)
    }

    @Test fun replaceFtsRemovesPreviousEntryForDocument() = runTest {
        dao.insertComplete(document("doc-1", "Invoice", "hash-1"), emptyList(), emptyList())
        dao.replaceFts(DocumentFtsEntity(documentId = "doc-1", title = "Invoice", content = "first version", classification = "INVOICE", keywords = ""))
        dao.replaceFts(DocumentFtsEntity(documentId = "doc-1", title = "Invoice", content = "second version", classification = "INVOICE", keywords = ""))

        assertTrue(dao.search("\"first\"").first().isEmpty())
        assertEquals(1, dao.search("\"second\"").first().size)
    }
}

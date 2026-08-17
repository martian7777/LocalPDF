package com.localpdf.feature.library

import com.localpdf.core.data.DocumentRepository
import com.localpdf.core.data.DuplicateDocumentException
import com.localpdf.core.model.Document
import com.localpdf.core.model.DocumentPage
import com.localpdf.core.model.DocumentSort
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun document(id: String, title: String, updatedAtMillis: Long) = Document(
        id, title, "/data/$id.pdf", 100, 1, createdAt = Instant.ofEpochMilli(updatedAtMillis), updatedAt = Instant.ofEpochMilli(updatedAtMillis),
    )

    @Test fun sortChangedReordersDocuments() = runTest(dispatcher) {
        val repository = FakeDocumentRepository(listOf(document("a", "Beta", 200), document("b", "Alpha", 100)))
        val viewModel = LibraryViewModel(repository)
        val stateJob = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), viewModel.state.value.documents.map { it.id })

        viewModel.onAction(LibraryAction.SortChanged(DocumentSort.TITLE_ASC))
        advanceUntilIdle()
        assertEquals(listOf("b", "a"), viewModel.state.value.documents.map { it.id })

        stateJob.cancel()
    }

    @Test fun importSelectedSendsSuccessMessageAndClearsImportingFlag() = runTest(dispatcher) {
        val repository = FakeDocumentRepository(emptyList())
        val viewModel = LibraryViewModel(repository)
        val effects = mutableListOf<LibraryEffect>()
        val effectsJob = launch { viewModel.effects.collect(effects::add) }
        val stateJob = launch { viewModel.state.collect {} }

        viewModel.onAction(LibraryAction.ImportSelected("content://doc"))
        advanceUntilIdle()

        assertEquals(listOf("content://doc"), repository.importedUris)
        assertTrue(effects.any { it is LibraryEffect.Message && it.text == "Document imported" })
        assertEquals(false, viewModel.state.value.isImporting)

        effectsJob.cancel(); stateJob.cancel()
    }

    @Test fun importFailureSurfacesErrorMessage() = runTest(dispatcher) {
        val repository = FakeDocumentRepository(emptyList(), importResult = Result.failure(DuplicateDocumentException()))
        val viewModel = LibraryViewModel(repository)
        val effects = mutableListOf<LibraryEffect>()
        val effectsJob = launch { viewModel.effects.collect(effects::add) }

        viewModel.onAction(LibraryAction.ImportSelected("content://doc"))
        advanceUntilIdle()

        assertTrue(effects.any { it is LibraryEffect.Message && it.text.contains("already in your library") })
        effectsJob.cancel()
    }

    @Test fun deleteConfirmedDeletesPendingDocumentAndClearsSelection() = runTest(dispatcher) {
        val doc = document("a", "Beta", 100)
        val repository = FakeDocumentRepository(listOf(doc))
        val viewModel = LibraryViewModel(repository)
        val stateJob = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(LibraryAction.DeleteRequested(doc))
        advanceUntilIdle()
        assertEquals(doc, viewModel.state.value.pendingDelete)

        viewModel.onAction(LibraryAction.DeleteConfirmed)
        advanceUntilIdle()
        assertEquals(listOf("a"), repository.deletedIds)
        assertNull(viewModel.state.value.pendingDelete)

        stateJob.cancel()
    }

    @Test fun documentClickedEmitsOpenDocumentEffect() = runTest(dispatcher) {
        val viewModel = LibraryViewModel(FakeDocumentRepository(emptyList()))
        val effects = mutableListOf<LibraryEffect>()
        val effectsJob = launch { viewModel.effects.collect(effects::add) }

        viewModel.onAction(LibraryAction.DocumentClicked("doc-1"))
        advanceUntilIdle()

        assertEquals(listOf(LibraryEffect.OpenDocument("doc-1")), effects)
        effectsJob.cancel()
    }
}

private class FakeDocumentRepository(
    initial: List<Document>,
    private val importResult: Result<Document>? = null,
) : DocumentRepository {
    private val documents = MutableStateFlow(initial)
    val importedUris = mutableListOf<String>()
    val deletedIds = mutableListOf<String>()

    override fun observeDocuments(): Flow<List<Document>> = documents
    override fun observeDocument(id: String): Flow<Document?> = MutableStateFlow(documents.value.find { it.id == id })
    override fun observePages(documentId: String): Flow<List<DocumentPage>> = MutableStateFlow(emptyList())

    override suspend fun importDocument(sourceUri: String): Result<Document> {
        importedUris += sourceUri
        val result = importResult ?: Result.success(Document(sourceUri, "Imported", "/data/x.pdf", 10, 1, createdAt = Instant.now(), updatedAt = Instant.now()))
        result.getOrNull()?.let { documents.value = documents.value + it }
        return result
    }

    override suspend fun importCapturedPages(pagePaths: List<String>): Result<Document> = error("not used")
    override suspend fun setFavorite(id: String, favorite: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun rename(id: String, title: String): Result<Unit> = Result.success(Unit)
    override suspend fun correctOcr(documentId: String, blockId: String, text: String): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: String): Result<Unit> {
        deletedIds += id
        documents.value = documents.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun prepareShareCopy(id: String): Result<File> = error("not used")
}

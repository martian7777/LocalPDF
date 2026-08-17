package com.localpdf.feature.viewer

import com.localpdf.core.data.DocumentRepository
import com.localpdf.core.model.BoundingBox
import com.localpdf.core.model.Document
import com.localpdf.core.model.DocumentPage
import com.localpdf.core.model.OcrBlock
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val block = OcrBlock("block-1", "page-1", "Toatl 42.00", 0.4f, BoundingBox(0f, 0f, 1f, 1f))
    private val document = Document("doc-1", "Invoice", "/data/doc-1.pdf", 100, 1, createdAt = Instant.now(), updatedAt = Instant.now())
    private val page = DocumentPage("page-1", "doc-1", 0, null, 800, 1200, ocrBlocks = listOf(block))

    @Test fun selectAndDismissUpdateSelectedBlockInState() = runTest(dispatcher) {
        val viewModel = ViewerViewModel("doc-1", FakeDocumentRepository(document, listOf(page)))
        val stateJob = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(ViewerAction.SelectBlock(block))
        advanceUntilIdle()
        assertEquals(block, viewModel.state.value.selected)

        viewModel.onAction(ViewerAction.DismissCorrection)
        advanceUntilIdle()
        assertNull(viewModel.state.value.selected)

        stateJob.cancel()
    }

    @Test fun saveCorrectionUpdatesRepositoryAndClearsSelection() = runTest(dispatcher) {
        val repository = FakeDocumentRepository(document, listOf(page))
        val viewModel = ViewerViewModel("doc-1", repository)
        val stateJob = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onAction(ViewerAction.SelectBlock(block))
        viewModel.onAction(ViewerAction.SaveCorrection("Total 42.00"))
        advanceUntilIdle()

        assertEquals(listOf(Triple("doc-1", "block-1", "Total 42.00")), repository.corrections)
        assertNull(viewModel.state.value.selected)

        stateJob.cancel()
    }

    @Test fun saveCorrectionWithNoSelectionIsNoOp() = runTest(dispatcher) {
        val repository = FakeDocumentRepository(document, listOf(page))
        val viewModel = ViewerViewModel("doc-1", repository)

        viewModel.onAction(ViewerAction.SaveCorrection("ignored"))
        advanceUntilIdle()

        assertEquals(emptyList<Triple<String, String, String>>(), repository.corrections)
    }
}

private class FakeDocumentRepository(
    private val document: Document?,
    private val pages: List<DocumentPage>,
) : DocumentRepository {
    val corrections = mutableListOf<Triple<String, String, String>>()

    override fun observeDocuments(): Flow<List<Document>> = MutableStateFlow(emptyList())
    override fun observeDocument(id: String): Flow<Document?> = MutableStateFlow(document)
    override fun observePages(documentId: String): Flow<List<DocumentPage>> = MutableStateFlow(pages)
    override suspend fun importDocument(sourceUri: String): Result<Document> = error("not used")
    override suspend fun importCapturedPages(pagePaths: List<String>): Result<Document> = error("not used")
    override suspend fun setFavorite(id: String, favorite: Boolean): Result<Unit> = error("not used")
    override suspend fun rename(id: String, title: String): Result<Unit> = error("not used")

    override suspend fun correctOcr(documentId: String, blockId: String, text: String): Result<Unit> {
        corrections += Triple(documentId, blockId, text)
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> = error("not used")
    override suspend fun prepareShareCopy(id: String): Result<File> = error("not used")
}

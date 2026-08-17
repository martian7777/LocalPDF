package com.localpdf.feature.vault

import com.localpdf.core.model.Document
import com.localpdf.core.security.VaultRepository
import java.io.File
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun openInvokesOnReadyWithDecryptedCopyOnSuccess() = runTest(dispatcher) {
        val file = File("/data/vault-open/doc-1.pdf")
        val repository = FakeVaultRepository(openResult = Result.success(file))
        val viewModel = VaultViewModel(repository)
        var ready: File? = null

        viewModel.open("doc-1", { ready = it }, { fail("should not error: $it") })
        advanceUntilIdle()

        assertEquals(file, ready)
        assertEquals(listOf("doc-1"), repository.openedIds)
    }

    @Test fun openInvokesOnErrorWhenKeyIsInvalidated() = runTest(dispatcher) {
        val repository = FakeVaultRepository(openResult = Result.failure(IllegalStateException("Vault key was invalidated and this file cannot be recovered")))
        val viewModel = VaultViewModel(repository)
        var error: String? = null

        viewModel.open("doc-1", { fail("should not succeed") }, { error = it })
        advanceUntilIdle()

        assertEquals("Vault key was invalidated and this file cannot be recovered", error)
    }

    @Test fun restoreInvokesOnDoneAndRestoresDocument() = runTest(dispatcher) {
        val repository = FakeVaultRepository(restoreResult = Result.success(Unit))
        val viewModel = VaultViewModel(repository)
        var done = false

        viewModel.restore("doc-1", { done = true }, { fail("should not error: $it") })
        advanceUntilIdle()

        assertTrue(done)
        assertEquals(listOf("doc-1"), repository.restoredIds)
    }

    @Test fun documentsReflectsRepositoryVaultContents() = runTest(dispatcher) {
        val document = Document("doc-1", "Statement", "/data/vault/doc-1.lpv", 100, 1, createdAt = java.time.Instant.now(), updatedAt = java.time.Instant.now(), isVaulted = true)
        val viewModel = VaultViewModel(FakeVaultRepository(vaulted = listOf(document)))
        val stateJob = launch { viewModel.documents.collect {} }
        advanceUntilIdle()

        assertEquals(listOf(document), viewModel.documents.value)

        stateJob.cancel()
    }
}

private class FakeVaultRepository(
    vaulted: List<Document> = emptyList(),
    private val openResult: Result<File> = Result.success(File("/data/vault-open/x.pdf")),
    private val restoreResult: Result<Unit> = Result.success(Unit),
) : VaultRepository {
    private val documents = MutableStateFlow(vaulted)
    val openedIds = mutableListOf<String>()
    val restoredIds = mutableListOf<String>()

    override fun observeVault(): Flow<List<Document>> = documents
    override suspend fun moveToVault(documentId: String): Result<Unit> = Result.success(Unit)
    override suspend fun openAuthenticatedCopy(documentId: String): Result<File> {
        openedIds += documentId
        return openResult
    }
    override suspend fun restoreFromVault(documentId: String): Result<Unit> {
        restoredIds += documentId
        return restoreResult
    }
    override fun clearTemporaryFiles() {}
}

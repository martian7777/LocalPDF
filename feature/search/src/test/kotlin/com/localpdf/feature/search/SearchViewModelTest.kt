package com.localpdf.feature.search

import com.localpdf.core.data.SearchRepository
import com.localpdf.core.model.Document
import com.localpdf.core.model.SearchFilter
import com.localpdf.core.model.SearchResult
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun queryIsDebouncedBeforeSearching() = runTest(dispatcher) {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository)
        val stateJob = launch { viewModel.state.collect {} }

        viewModel.onAction(SearchAction.QueryChanged("invoice"))
        advanceTimeBy(100)
        runCurrent()
        assertEquals(0, repository.searchCallCount)

        advanceTimeBy(200)
        runCurrent()
        assertEquals(1, repository.searchCallCount)
        assertEquals("invoice", repository.lastQuery)
        assertTrue(viewModel.state.value.hasSearched)
        assertEquals(1, viewModel.state.value.results.size)

        stateJob.cancel()
    }

    @Test fun blankQueryNeverMarksHasSearched() = runTest(dispatcher) {
        val viewModel = SearchViewModel(FakeSearchRepository())
        val stateJob = launch { viewModel.state.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasSearched)
        assertTrue(viewModel.state.value.results.isEmpty())

        stateJob.cancel()
    }
}

private class FakeSearchRepository : SearchRepository {
    var searchCallCount = 0
    var lastQuery: String? = null

    override fun search(query: String, filter: SearchFilter): Flow<List<SearchResult>> {
        searchCallCount++
        lastQuery = query
        if (query.isBlank()) return flowOf(emptyList())
        val document = Document("doc-1", "Result for $query", "/data/doc-1.pdf", 10, 1, createdAt = Instant.now(), updatedAt = Instant.now())
        return flowOf(listOf(SearchResult(document, "…snippet…", 0)))
    }
}

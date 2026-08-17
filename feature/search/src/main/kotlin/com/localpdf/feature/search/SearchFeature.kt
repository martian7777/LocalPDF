package com.localpdf.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localpdf.core.data.SearchRepository
import com.localpdf.core.designsystem.GlassSurface
import com.localpdf.core.model.SearchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(val query: String = "", val results: List<SearchResult> = emptyList(), val hasSearched: Boolean = false)
sealed interface SearchAction { data class QueryChanged(val value: String) : SearchAction; data class ResultClicked(val documentId: String) : SearchAction }

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val state: StateFlow<SearchUiState> = query.debounce(250).flatMapLatest { value -> repository.search(value).map { value to it } }
        .map { (value, results) -> SearchUiState(value, results, value.isNotBlank()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())
    fun onAction(action: SearchAction) { if (action is SearchAction.QueryChanged) query.value = action.value }
    class Factory(private val repository: SearchRepository) : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = SearchViewModel(repository) as T }
}

@Composable
fun SearchScreen(state: SearchUiState, onAction: (SearchAction) -> Unit, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = state.query, onValueChange = { onAction(SearchAction.QueryChanged(it)) }, label = { Text("Search text, invoice number, or amount") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        when {
            !state.hasSearched -> Text("Search runs only against the private on-device index.", color = MaterialTheme.colorScheme.onSurface.copy(.65f))
            state.results.isEmpty() -> Text("No matching documents")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.results, key = { it.document.id }) { result ->
                    GlassSurface(Modifier.fillMaxWidth().clickable { onOpen(result.document.id) }) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(result.document.title, fontWeight = FontWeight.SemiBold)
                            Text(result.snippet, style = MaterialTheme.typography.bodyMedium)
                            Text(result.document.classification.name.replace('_', ' '), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

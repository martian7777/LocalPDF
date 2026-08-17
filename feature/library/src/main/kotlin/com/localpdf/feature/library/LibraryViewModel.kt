package com.localpdf.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localpdf.core.data.DocumentRepository
import com.localpdf.core.model.Document
import com.localpdf.core.model.DocumentSort
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val sort: DocumentSort = DocumentSort.UPDATED_DESC,
    val pendingDelete: Document? = null,
)

sealed interface LibraryAction {
    data object ImportClicked : LibraryAction
    data class ImportSelected(val uri: String) : LibraryAction
    data class FavoriteChanged(val id: String, val favorite: Boolean) : LibraryAction
    data class DeleteRequested(val document: Document) : LibraryAction
    data object DeleteDismissed : LibraryAction
    data object DeleteConfirmed : LibraryAction
    data class SortChanged(val sort: DocumentSort) : LibraryAction
    data class DocumentClicked(val id: String) : LibraryAction
}

sealed interface LibraryEffect {
    data object LaunchImportPicker : LibraryEffect
    data class OpenDocument(val id: String) : LibraryEffect
    data class Message(val text: String) : LibraryEffect
}

class LibraryViewModel(private val repository: DocumentRepository) : ViewModel() {
    private val importing = MutableStateFlow(false)
    private val sort = MutableStateFlow(DocumentSort.UPDATED_DESC)
    private val pendingDelete = MutableStateFlow<Document?>(null)
    private val effectsChannel = Channel<LibraryEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    val state: StateFlow<LibraryUiState> = combine(repository.observeDocuments(), importing, sort, pendingDelete) { documents, isImporting, selectedSort, deleting ->
        val sorted = when (selectedSort) {
            DocumentSort.UPDATED_DESC -> documents.sortedByDescending(Document::updatedAt)
            DocumentSort.CREATED_DESC -> documents.sortedByDescending(Document::createdAt)
            DocumentSort.TITLE_ASC -> documents.sortedBy { it.title.lowercase() }
            DocumentSort.SIZE_DESC -> documents.sortedByDescending(Document::fileSizeBytes)
        }
        LibraryUiState(sorted, isLoading = false, isImporting = isImporting, sort = selectedSort, pendingDelete = deleting)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onAction(action: LibraryAction) {
        when (action) {
            LibraryAction.ImportClicked -> effectsChannel.trySend(LibraryEffect.LaunchImportPicker)
            is LibraryAction.ImportSelected -> import(action.uri)
            is LibraryAction.FavoriteChanged -> viewModelScope.launch { repository.setFavorite(action.id, action.favorite).onFailure(::showError) }
            is LibraryAction.DeleteRequested -> pendingDelete.value = action.document
            LibraryAction.DeleteDismissed -> pendingDelete.value = null
            LibraryAction.DeleteConfirmed -> deletePending()
            is LibraryAction.SortChanged -> sort.value = action.sort
            is LibraryAction.DocumentClicked -> effectsChannel.trySend(LibraryEffect.OpenDocument(action.id))
        }
    }

    private fun import(uri: String) = viewModelScope.launch {
        if (importing.value) return@launch
        importing.value = true
        repository.importDocument(uri).onSuccess { effectsChannel.send(LibraryEffect.Message("Document imported")) }.onFailure(::showError)
        importing.value = false
    }

    private fun deletePending() = viewModelScope.launch {
        val document = pendingDelete.value ?: return@launch
        pendingDelete.value = null
        repository.delete(document.id).onFailure(::showError)
    }

    private fun showError(error: Throwable) {
        effectsChannel.trySend(LibraryEffect.Message(error.message ?: "Document operation failed"))
    }

    class Factory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LibraryViewModel(repository) as T
    }
}

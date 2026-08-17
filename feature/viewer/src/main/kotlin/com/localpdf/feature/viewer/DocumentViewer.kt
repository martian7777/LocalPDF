package com.localpdf.feature.viewer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localpdf.core.data.DocumentRepository
import com.localpdf.core.model.Document
import com.localpdf.core.model.DocumentPage
import com.localpdf.core.model.OcrBlock
import com.localpdf.feature.ocredit.OcrCorrectionDialog
import com.localpdf.feature.redaction.RedactionDialog
import com.localpdf.core.model.RedactionRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ViewerUiState(val document: Document? = null, val pages: List<DocumentPage> = emptyList(), val selected: OcrBlock? = null, val error: String? = null)
sealed interface ViewerAction { data class SelectBlock(val block: OcrBlock) : ViewerAction; data object DismissCorrection : ViewerAction; data class SaveCorrection(val text: String) : ViewerAction }

class ViewerViewModel(private val id: String, private val repository: DocumentRepository) : ViewModel() {
    private val selected = MutableStateFlow<OcrBlock?>(null)
    val state = combine(repository.observeDocument(id), repository.observePages(id), selected) { document, pages, block -> ViewerUiState(document, pages, block) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewerUiState())
    fun onAction(action: ViewerAction) { when (action) {
        is ViewerAction.SelectBlock -> selected.value = action.block
        ViewerAction.DismissCorrection -> selected.value = null
        is ViewerAction.SaveCorrection -> selected.value?.let { block -> viewModelScope.launch { repository.correctOcr(id, block.id, action.text); selected.value = null } }
    } }
    class Factory(private val id: String, private val repository: DocumentRepository) : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ViewerViewModel(id, repository) as T }
}

@Composable
fun DocumentViewerScreen(state: ViewerUiState, onAction: (ViewerAction) -> Unit, onBack: () -> Unit, onMoveToVault: (String) -> Unit, onCreateRedactedCopy: (String, List<RedactionRegion>) -> Unit, modifier: Modifier = Modifier) {
    var showRedaction by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to library") }
            Column(Modifier.weight(1f)) { Text(state.document?.title ?: "Loading document", style = MaterialTheme.typography.titleLarge); Text("Tap recognized text to correct it", style = MaterialTheme.typography.bodySmall) }
            state.document?.takeIf { !it.isVaulted }?.let { document -> Column { TextButton(onClick = { showRedaction = true }) { Text("Redact") }; TextButton(onClick = { onMoveToVault(document.id) }) { Text("Vault") } } }
        }
        Spacer(Modifier.height(12.dp))
        if (state.document == null) CircularProgressIndicator() else LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
            items(state.pages, key = { it.id }) { page -> PageCard(page, onAction) }
        }
    }
    state.selected?.let { block -> OcrCorrectionDialog(block, { onAction(ViewerAction.DismissCorrection) }, { onAction(ViewerAction.SaveCorrection(it)) }) }
    if (showRedaction) RedactionDialog(state.pages, { showRedaction = false }) { regions -> state.document?.let { onCreateRedactedCopy(it.id, regions) }; showRedaction = false }
}

@Composable private fun PageCard(page: DocumentPage, onAction: (ViewerAction) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val bitmap by produceState<android.graphics.Bitmap?>(null, page.imagePath) { value = withContext(Dispatchers.IO) { page.imagePath?.let(BitmapFactory::decodeFile) } }
        bitmap?.let { Image(it.asImageBitmap(), "Page ${page.pageIndex + 1}", Modifier.fillMaxWidth().aspectRatio(it.width.toFloat() / it.height), contentScale = ContentScale.Fit) }
        Text("Page ${page.pageIndex + 1}", style = MaterialTheme.typography.labelLarge)
        page.ocrBlocks.forEach { block -> Text(block.text, Modifier.fillMaxWidth().clickable { onAction(ViewerAction.SelectBlock(block)) }.padding(vertical = 6.dp), color = if (block.confidence < .75f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface) }
    } }
}

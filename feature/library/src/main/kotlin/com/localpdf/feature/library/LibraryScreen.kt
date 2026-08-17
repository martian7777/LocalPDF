package com.localpdf.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localpdf.core.designsystem.GlassSurface
import com.localpdf.core.model.Document
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    columns: Int,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.documents.isEmpty() -> LibraryEmpty(onImport = { onAction(LibraryAction.ImportClicked) }, Modifier.align(Alignment.Center))
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.documents, key = Document::id) { document ->
                    DocumentCard(
                        document,
                        onOpen = { onAction(LibraryAction.DocumentClicked(document.id)) },
                        onFavorite = { onAction(LibraryAction.FavoriteChanged(document.id, !document.isFavorite)) },
                        onDelete = { onAction(LibraryAction.DeleteRequested(document)) },
                    )
                }
            }
        }
        if (state.isImporting) {
            GlassSurface(Modifier.align(Alignment.BottomCenter).padding(20.dp)) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp)); Text("Importing securely…")
                }
            }
        }
    }
    state.pendingDelete?.let { document ->
        AlertDialog(
            onDismissRequest = { onAction(LibraryAction.DeleteDismissed) },
            title = { Text("Delete document?") },
            text = { Text("${document.title} and its local processing data will be permanently removed.") },
            confirmButton = { TextButton(onClick = { onAction(LibraryAction.DeleteConfirmed) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { onAction(LibraryAction.DeleteDismissed) }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LibraryEmpty(onImport: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Filled.Home, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Your private document workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Import an existing PDF or image, or scan a new document. Files remain on this device.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
            OutlinedButton(onClick = onImport) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Import file") }
        }
    }
}

@Composable
private fun DocumentCard(document: Document, onOpen: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Home, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(document.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${document.pageCount} page${if (document.pageCount == 1) "" else "s"} • ${formatSize(document.fileSizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
            Text(DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()).format(document.updatedAt), style = MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onFavorite) { Icon(Icons.Filled.Star, "Favorite", tint = if (document.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface.copy(alpha = .4f)) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete document", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

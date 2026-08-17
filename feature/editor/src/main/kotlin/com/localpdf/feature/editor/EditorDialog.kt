package com.localpdf.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.localpdf.core.model.DocumentPage

data class PageEditState(val pageId: String, val rotationDegrees: Int = 0, val deleted: Boolean = false)

@Composable
fun EditorDialog(pages: List<DocumentPage>, onDismiss: () -> Unit, onConfirm: (List<PageEditState>, String?) -> Unit) {
    val edits = remember(pages) { pages.sortedBy { it.pageIndex }.map { PageEditState(it.id) }.toMutableStateList() }
    var watermark by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit pages") },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rotate or remove pages, then save as a new copy. The original document is unchanged.")
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(edits, key = { it.pageId }) { edit ->
                        val index = edits.indexOfFirst { it.pageId == edit.pageId }
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Page ${index + 1} · ${edit.rotationDegrees}°",
                                Modifier.weight(1f),
                                textDecoration = if (edit.deleted) TextDecoration.LineThrough else null,
                            )
                            IconButton(onClick = { edits[index] = edit.copy(rotationDegrees = (edit.rotationDegrees + 90) % 360) }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Rotate page")
                            }
                            IconButton(onClick = { edits[index] = edit.copy(deleted = !edit.deleted) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove page")
                            }
                        }
                    }
                }
                OutlinedTextField(value = watermark, onValueChange = { watermark = it }, label = { Text("Watermark (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(enabled = edits.any { !it.deleted }, onClick = { onConfirm(edits.toList(), watermark.trim().ifBlank { null }) }) { Text("Save copy") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package com.localpdf.feature.redaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localpdf.core.model.*

@Composable fun RedactionDialog(pages: List<DocumentPage>, onDismiss: () -> Unit, onConfirm: (List<RedactionRegion>) -> Unit) {
    val candidates = remember(pages) { pages.flatMap { page -> page.ocrBlocks.map { page.pageIndex to it } } }
    val selected = remember { mutableStateListOf<String>() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Permanent redaction") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Select recognized text to burn permanently into pixels. A new flattened copy will be created; the original remains unchanged.")
        LazyColumn(Modifier.heightIn(max = 360.dp)) { items(candidates, key = { it.second.id }) { (page, block) -> Row(Modifier.fillMaxWidth().clickable { if (block.id in selected) selected.remove(block.id) else selected.add(block.id) }.padding(vertical = 6.dp)) { Checkbox(block.id in selected, onCheckedChange = null); Text("Page ${page + 1}: ${block.text}", Modifier.padding(start = 8.dp)) } } }
    } }, confirmButton = { Button(enabled = selected.isNotEmpty(), onClick = { onConfirm(candidates.filter { it.second.id in selected }.map { RedactionRegion(it.first, it.second.boundingBox) }) }) { Text("Create secure copy") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

package com.localpdf.feature.ocredit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.localpdf.core.model.OcrBlock

@Composable
fun OcrCorrectionDialog(block: OcrBlock, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember(block.id) { mutableStateOf(block.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Correct recognized text") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, supportingText = { Text("Confidence ${(block.confidence * 100).toInt()}% · stored only on this device") }) },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onSave(text) }) { Text("Save and rebuild PDF") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

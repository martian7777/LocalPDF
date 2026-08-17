package com.localpdf.feature.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.localpdf.core.cvscanner.DocumentEdgeDetector
import com.localpdf.core.cvscanner.DocumentQuad
import com.localpdf.core.cvscanner.PageFilter
import com.localpdf.core.cvscanner.PagePreprocessor
import com.localpdf.core.cvscanner.QuadPoint
import com.localpdf.core.cvscanner.ScanAnalysis
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CapturedPage(val id: String, val path: String)

@Composable
fun ScannerScreen(onClose: () -> Unit, onFinished: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember { DocumentEdgeDetector() }
    val pages = remember { mutableStateListOf<CapturedPage>() }
    var analysis by remember { mutableStateOf(ScanAnalysis(null, 0.0, false)) }
    var capturing by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var reviewFile by remember { mutableStateOf<File?>(null) }
    var reviewQuad by remember { mutableStateOf<DocumentQuad?>(null) }
    var selectedPageId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        controller.setImageAnalysisAnalyzer(analysisExecutor) { image -> analysis = detector.analyze(image) }
        onDispose { controller.clearImageAnalysisAnalyzer(); controller.unbind(); analysisExecutor.shutdownNow() }
    }

    val currentReviewFile = reviewFile
    val currentReviewQuad = reviewQuad
    if (currentReviewFile != null && currentReviewQuad != null) {
        ReviewStage(
            file = currentReviewFile,
            initialQuad = currentReviewQuad,
            processing = processing,
            onRetake = { currentReviewFile.delete(); reviewFile = null; reviewQuad = null },
            onConfirm = { quad, filter ->
                processing = true
                scope.launch {
                    val output = withContext(Dispatchers.Default) {
                        val fullBitmap = decodeUprightBitmap(currentReviewFile, Int.MAX_VALUE)
                        val processedBitmap = PagePreprocessor.process(fullBitmap, quad, filter)
                        fullBitmap.recycle()
                        val target = File(context.filesDir, "scan-sessions/${System.currentTimeMillis()}.jpg")
                        target.outputStream().use { processedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                        processedBitmap.recycle()
                        target
                    }
                    currentReviewFile.delete()
                    pages.add(CapturedPage(output.name, output.absolutePath))
                    processing = false
                    reviewFile = null
                    reviewQuad = null
                }
            },
        )
        return
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { PreviewView(it).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } }, modifier = Modifier.fillMaxSize())
        analysis.quad?.let { quad -> QuadOverlay(quad, Color(0xFF34D399)) }
        IconButton(onClick = onClose, Modifier.align(Alignment.TopStart).padding(20.dp).background(Color.Black.copy(.5f), CircleShape)) { Icon(Icons.Filled.Close, "Close scanner", tint = Color.White) }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.58f)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pages.isNotEmpty()) {
                PageTray(
                    pages = pages,
                    selectedId = selectedPageId,
                    onSelect = { id -> selectedPageId = if (selectedPageId == id) null else id },
                    onMoveLeft = { id -> moveCapturedPage(pages, id, -1) },
                    onMoveRight = { id -> moveCapturedPage(pages, id, 1) },
                    onRemove = { id ->
                        pages.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index -> File(pages[index].path).delete(); pages.removeAt(index) }
                        if (selectedPageId == id) selectedPageId = null
                    },
                )
            }
            Text(
                when {
                    analysis.lowLight -> "More light needed"
                    analysis.blurScore < 65 -> "Hold steady"
                    analysis.quad == null -> "Move the document into frame"
                    else -> "Document detected"
                },
                color = Color.White,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Text("${pages.size} pages", color = Color.White)
                IconButton(
                    enabled = !capturing,
                    onClick = {
                        capturing = true
                        val file = File(context.filesDir, "scan-sessions/${System.currentTimeMillis()}-raw.jpg").apply { parentFile?.mkdirs() }
                        val capturedQuad = analysis.quad
                        controller.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                    capturing = false
                                    reviewQuad = capturedQuad ?: DocumentQuad.inset()
                                    reviewFile = file
                                }
                                override fun onError(exception: ImageCaptureException) { file.delete(); capturing = false }
                            },
                        )
                    },
                    modifier = Modifier.size(72.dp).background(Color.White, CircleShape),
                ) { if (capturing) CircularProgressIndicator(Modifier.size(28.dp)) else Icon(Icons.Filled.Add, "Capture page", tint = Color.Black) }
                Button(enabled = pages.isNotEmpty(), onClick = { onFinished(pages.map(CapturedPage::path)) }) { Text("Done") }
            }
        }
    }
}

@Composable
private fun ReviewStage(file: File, initialQuad: DocumentQuad, processing: Boolean, onRetake: () -> Unit, onConfirm: (DocumentQuad, PageFilter) -> Unit) {
    var quad by remember(file) { mutableStateOf(initialQuad) }
    var filter by remember(file) { mutableStateOf(PageFilter.COLOR_ENHANCED) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val preview by produceState<Bitmap?>(null, file) { value = withContext(Dispatchers.IO) { decodeUprightBitmap(file, 1200) } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val bitmap = preview
        if (bitmap == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        } else {
            Box(
                Modifier.align(Alignment.Center).fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height).onSizeChanged { boxSize = it },
            ) {
                Image(bitmap.asImageBitmap(), "Captured page", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                QuadOverlay(quad, Color(0xFF34D399))
                CornerHandle("Top left", quad.topLeft, boxSize) { quad = quad.copy(topLeft = it) }
                CornerHandle("Top right", quad.topRight, boxSize) { quad = quad.copy(topRight = it) }
                CornerHandle("Bottom right", quad.bottomRight, boxSize) { quad = quad.copy(bottomRight = it) }
                CornerHandle("Bottom left", quad.bottomLeft, boxSize) { quad = quad.copy(bottomLeft = it) }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.6f)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Drag the corners to match the page edges", color = Color.White.copy(.8f), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PageFilter.entries.forEach { option -> FilterOption(option, selected = filter == option, onClick = { filter = option }) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRetake, enabled = !processing) { Text("Retake", color = Color.White) }
                Button(onClick = { onConfirm(quad, filter) }, enabled = !processing && preview != null) {
                    if (processing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Use photo")
                }
            }
        }
    }
}

@Composable
private fun QuadOverlay(quad: DocumentQuad, color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(quad.topLeft.x * size.width, quad.topLeft.y * size.height)
            lineTo(quad.topRight.x * size.width, quad.topRight.y * size.height)
            lineTo(quad.bottomRight.x * size.width, quad.bottomRight.y * size.height)
            lineTo(quad.bottomLeft.x * size.width, quad.bottomLeft.y * size.height)
            close()
        }
        drawPath(path, color, style = Stroke(4.dp.toPx()))
    }
}

@Composable
private fun CornerHandle(label: String, point: QuadPoint, boxSize: IntSize, onDrag: (QuadPoint) -> Unit) {
    // The touch/drag target is kept at the 44dp accessibility minimum even though the visible dot is smaller.
    Box(
        Modifier
            .offset { val radius = 22.dp.toPx(); IntOffset((point.x * boxSize.width - radius).toInt(), (point.y * boxSize.height - radius).toInt()) }
            .size(44.dp)
            .semantics { contentDescription = "$label crop corner, drag to adjust" }
            .pointerInput(boxSize) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (boxSize.width > 0 && boxSize.height > 0) {
                        onDrag(QuadPoint((point.x + dragAmount.x / boxSize.width).coerceIn(0f, 1f), (point.y + dragAmount.y / boxSize.height).coerceIn(0f, 1f)))
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(20.dp).background(Color.White, CircleShape).border(2.dp, Color(0xFF34D399), CircleShape))
    }
}

@Composable
private fun FilterOption(filter: PageFilter, selected: Boolean, onClick: () -> Unit) {
    val label = when (filter) {
        PageFilter.ORIGINAL -> "Original"
        PageFilter.COLOR_ENHANCED -> "Enhanced"
        PageFilter.GRAYSCALE -> "Grayscale"
        PageFilter.BLACK_AND_WHITE -> "B & W"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF34D399), unselectedColor = Color.White))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PageTray(pages: List<CapturedPage>, selectedId: String?, onSelect: (String) -> Unit, onMoveLeft: (String) -> Unit, onMoveRight: (String) -> Unit, onRemove: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(pages, key = { _, page -> page.id }) { index, page -> PageThumbnail(page, index, selected = page.id == selectedId, onClick = { onSelect(page.id) }) }
        }
        if (selectedId != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMoveLeft(selectedId) }) { Icon(Icons.Filled.KeyboardArrowLeft, "Move page earlier", tint = Color.White) }
                IconButton(onClick = { onRemove(selectedId) }) { Icon(Icons.Filled.Delete, "Remove page", tint = Color(0xFFF87171)) }
                IconButton(onClick = { onMoveRight(selectedId) }) { Icon(Icons.Filled.KeyboardArrowRight, "Move page later", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun PageThumbnail(page: CapturedPage, index: Int, selected: Boolean, onClick: () -> Unit) {
    val bitmap by produceState<Bitmap?>(null, page.path) { value = withContext(Dispatchers.IO) { decodeUprightBitmap(File(page.path), 200) } }
    Box(
        Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, if (selected) Color(0xFF34D399) else Color.White.copy(.3f), RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { Image(it.asImageBitmap(), "Page ${index + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } ?: CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
    }
}

private fun moveCapturedPage(pages: SnapshotStateList<CapturedPage>, id: String, delta: Int) {
    val index = pages.indexOfFirst { it.id == id }
    val target = index + delta
    if (index < 0 || target !in pages.indices) return
    pages.add(target, pages.removeAt(index))
}

private fun decodeUprightBitmap(file: File, maxDimension: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= maxDimension && bounds.outHeight / (sampleSize * 2) >= maxDimension) sampleSize *= 2
    val decoded = requireNotNull(BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sampleSize })) { "Captured page could not be decoded" }
    val rotation = exifRotationDegrees(file.absolutePath)
    if (rotation == 0) return decoded
    val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
    decoded.recycle()
    return rotated
}

private fun exifRotationDegrees(path: String): Int = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_180 -> 180
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}

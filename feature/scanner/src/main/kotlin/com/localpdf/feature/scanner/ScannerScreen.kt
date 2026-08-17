package com.localpdf.feature.scanner

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.localpdf.core.cvscanner.DocumentEdgeDetector
import com.localpdf.core.cvscanner.ScanAnalysis
import java.io.File
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(onClose: () -> Unit, onFinished: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember { DocumentEdgeDetector() }
    val pages = remember { mutableStateListOf<String>() }
    var analysis by remember { mutableStateOf(ScanAnalysis(null, 0.0, false)) }
    var capturing by remember { mutableStateOf(false) }

    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        controller.setImageAnalysisAnalyzer(analysisExecutor) { image -> analysis = detector.analyze(image) }
        onDispose { controller.clearImageAnalysisAnalyzer(); controller.unbind(); analysisExecutor.shutdownNow() }
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { PreviewView(it).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } }, modifier = Modifier.fillMaxSize())
        analysis.bounds?.let { bounds ->
            Canvas(Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(bounds.left * size.width, bounds.top * size.height); lineTo(bounds.right * size.width, bounds.top * size.height)
                    lineTo(bounds.right * size.width, bounds.bottom * size.height); lineTo(bounds.left * size.width, bounds.bottom * size.height); close()
                }
                drawPath(path, Color(0xFF34D399), style = Stroke(4.dp.toPx()))
            }
        }
        IconButton(onClick = onClose, Modifier.align(Alignment.TopStart).padding(20.dp).background(Color.Black.copy(.5f), CircleShape)) { Icon(Icons.Filled.Close, "Close scanner", tint = Color.White) }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.58f)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when { analysis.lowLight -> "More light needed"; analysis.blurScore < 65 -> "Hold steady"; analysis.bounds == null -> "Move the document into frame"; else -> "Document detected" }, color = Color.White)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Text("${pages.size} pages", color = Color.White)
                IconButton(
                    enabled = !capturing,
                    onClick = {
                        capturing = true
                        val file = File(context.filesDir, "scan-sessions/${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
                        controller.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) { pages += file.absolutePath; capturing = false }
                            override fun onError(exception: ImageCaptureException) { file.delete(); capturing = false }
                        })
                    },
                    modifier = Modifier.size(72.dp).background(Color.White, CircleShape),
                ) { if (capturing) CircularProgressIndicator(Modifier.size(28.dp)) else Icon(Icons.Filled.Add, "Capture page", tint = Color.Black) }
                Button(enabled = pages.isNotEmpty(), onClick = { onFinished(pages.toList()) }) { Text("Done") }
            }
        }
    }
}

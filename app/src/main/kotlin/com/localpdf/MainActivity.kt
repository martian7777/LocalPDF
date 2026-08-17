package com.localpdf

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.localpdf.core.designsystem.GlassSurface
import com.localpdf.core.designsystem.LocalPdfTheme

class MainActivity : ComponentActivity() {
    private var cameraGranted by mutableStateOf(false)
    private var cameraRequested by mutableStateOf(false)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        cameraRequested = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        cameraGranted = checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        setContent {
            LocalPdfTheme {
                LocalPdfApp(
                    cameraGranted = cameraGranted,
                    cameraRequested = cameraRequested,
                    onRequestCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName"),
                            ),
                        )
                    },
                )
            }
        }
    }
}

private enum class AppDestination(
    val label: String,
    val icon: ImageVector,
) {
    Library("Library", Icons.Outlined.FolderOpen),
    Search("Search", Icons.Outlined.Search),
    Vault("Vault", Icons.Outlined.Lock),
    Settings("Privacy", Icons.Outlined.Settings),
}

private enum class LayoutMode { Compact, Medium, Expanded }

@Composable
private fun LocalPdfApp(
    cameraGranted: Boolean,
    cameraRequested: Boolean,
    onRequestCamera: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val layoutMode = when {
        widthDp >= 840 -> LayoutMode.Expanded
        widthDp >= 600 -> LayoutMode.Medium
        else -> LayoutMode.Compact
    }
    var destination by remember { mutableStateOf(AppDestination.Library) }
    var scannerOpen by remember { mutableStateOf(false) }

    val background = Brush.radialGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
        ),
        radius = 1500f,
    )

    Box(Modifier.fillMaxSize().background(background)) {
        if (scannerOpen) {
            ScannerPlaceholder(
                cameraGranted = cameraGranted,
                cameraRequested = cameraRequested,
                onRequestCamera = onRequestCamera,
                onOpenSettings = onOpenSettings,
                onClose = { scannerOpen = false },
            )
        } else if (layoutMode == LayoutMode.Compact) {
            CompactShell(
                destination = destination,
                onDestination = { destination = it },
                onScan = { scannerOpen = true },
            )
        } else {
            WideShell(
                expanded = layoutMode == LayoutMode.Expanded,
                destination = destination,
                onDestination = { destination = it },
                onScan = { scannerOpen = true },
            )
        }
    }
}

@Composable
private fun CompactShell(
    destination: AppDestination,
    onDestination: (AppDestination) -> Unit,
    onScan: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)) {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == destination,
                        onClick = { onDestination(item) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (destination == AppDestination.Library) {
                FloatingActionButton(onClick = onScan) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "Scan document")
                }
            }
        },
    ) { padding ->
        DestinationContent(destination, Modifier.padding(padding), expanded = false, onScan = onScan)
    }
}

@Composable
private fun WideShell(
    expanded: Boolean,
    destination: AppDestination,
    onDestination: (AppDestination) -> Unit,
    onScan: () -> Unit,
) {
    Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            header = {
                Box(Modifier.padding(16.dp).size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Description, contentDescription = "LocalPDF", tint = Color.White)
                }
            },
        ) {
            Spacer(Modifier.weight(1f))
            AppDestination.entries.forEach { item ->
                NavigationRailItem(
                    selected = item == destination,
                    onClick = { onDestination(item) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.label) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
        DestinationContent(destination, Modifier.weight(1f), expanded = expanded, onScan = onScan)
    }
}

@Composable
private fun DestinationContent(
    destination: AppDestination,
    modifier: Modifier,
    expanded: Boolean,
    onScan: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = if (expanded) 40.dp else 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(destination.label, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        when (destination) {
            AppDestination.Library -> LibraryEmptyState(expanded, onScan)
            AppDestination.Search -> EmptyFeatureCard(Icons.Outlined.Search, "Search every page", "Your on-device full-text index will appear here after the first document is processed.")
            AppDestination.Vault -> EmptyFeatureCard(Icons.Outlined.Lock, "Private Vault", "Biometric-protected documents stay encrypted and available only on this device.")
            AppDestination.Settings -> PrivacyDashboard()
        }
    }
}

@Composable
private fun LibraryEmptyState(expanded: Boolean, onScan: () -> Unit) {
    val cardModifier = if (expanded) Modifier.fillMaxWidth(0.68f) else Modifier.fillMaxWidth()
    GlassSurface(cardModifier) {
        Column(
            Modifier.padding(if (expanded) 40.dp else 28.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Your private document workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Scan your first page. Processing, OCR, search, and storage remain on your device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            Button(onClick = onScan, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)) {
                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Scan document")
            }
        }
    }
}

@Composable
private fun EmptyFeatureCard(icon: ImageVector, title: String, detail: String) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun PrivacyDashboard() {
    GlassSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.PrivacyTip, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
            Text("Zero-egress by design", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Metric("Documents uploaded", "0")
            Metric("Third-party trackers", "0")
            Metric("Active cloud services", "None")
            Text("Core features are designed to work offline.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScannerPlaceholder(
    cameraGranted: Boolean,
    cameraRequested: Boolean,
    onRequestCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color(0xFF05070C)).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF818CF8))
            Text(
                when {
                    cameraGranted -> "Camera engine is the next MVP slice"
                    cameraRequested -> "Camera access is off"
                    else -> "Camera access is needed to scan"
                },
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                when {
                    cameraGranted -> "The permission boundary is ready. CameraX preview and edge detection will connect here without changing navigation."
                    cameraRequested -> "Enable camera access in system settings. Other app features remain available."
                    else -> "LocalPDF uses the camera only to capture documents. Images are not uploaded."
                },
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            if (!cameraGranted) {
                Button(onClick = if (cameraRequested) onOpenSettings else onRequestCamera) {
                    Text(if (cameraRequested) "Open settings" else "Allow camera")
                }
            }
            TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text("Back to library")
            }
        }
    }
}

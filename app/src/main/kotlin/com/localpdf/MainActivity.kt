package com.localpdf

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.localpdf.core.designsystem.GlassSurface
import com.localpdf.core.designsystem.LocalPdfTheme
import com.localpdf.core.data.OfflineDocumentRepository
import com.localpdf.core.data.OfflineSearchRepository
import com.localpdf.core.data.OfflineRedactionRepository
import com.localpdf.core.data.EditorPageEdit
import com.localpdf.core.data.EditorRepository
import com.localpdf.core.data.OfflineEditorRepository
import com.localpdf.feature.library.LibraryAction
import com.localpdf.feature.library.LibraryEffect
import com.localpdf.feature.library.LibraryScreen
import com.localpdf.feature.library.LibraryViewModel
import com.localpdf.feature.scanner.ScannerScreen
import com.localpdf.feature.search.SearchScreen
import com.localpdf.feature.search.SearchViewModel
import com.localpdf.feature.viewer.DocumentViewerScreen
import com.localpdf.feature.viewer.ViewerViewModel
import com.localpdf.core.security.KeystoreVaultRepository
import com.localpdf.core.security.VaultRepository
import com.localpdf.feature.vault.VaultAuthenticator
import com.localpdf.feature.vault.VaultScreen
import com.localpdf.feature.vault.VaultViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private var cameraGranted by mutableStateOf(false)
    private var cameraRequested by mutableStateOf(false)
    private lateinit var vaultRepository: VaultRepository

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
        val documentRepository = OfflineDocumentRepository.create(applicationContext)
        vaultRepository = KeystoreVaultRepository.create(applicationContext)
        val libraryFactory = LibraryViewModel.Factory(documentRepository)
        val searchFactory = SearchViewModel.Factory(OfflineSearchRepository.create(applicationContext))
        val redactionRepository = OfflineRedactionRepository.create(applicationContext)
        val editorRepository = OfflineEditorRepository.create(applicationContext)
        val vaultFactory = VaultViewModel.Factory(vaultRepository)
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
                    libraryFactory = libraryFactory,
                    documentRepository = documentRepository,
                    searchFactory = searchFactory,
                    vaultFactory = vaultFactory,
                    vaultRepository = vaultRepository,
                    redactionRepository = redactionRepository,
                    editorRepository = editorRepository,
                )
            }
        }
    }

    override fun onStop() { super.onStop(); if (::vaultRepository.isInitialized) vaultRepository.clearTemporaryFiles() }
}

private enum class AppDestination(
    val label: String,
    val icon: ImageVector,
) {
    Library("Library", Icons.Filled.Home),
    Search("Search", Icons.Filled.Search),
    Vault("Vault", Icons.Filled.Info),
    Settings("Privacy", Icons.Filled.Settings),
}

private enum class LayoutMode { Compact, Medium, Expanded }

@Composable
private fun LocalPdfApp(
    cameraGranted: Boolean,
    cameraRequested: Boolean,
    onRequestCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    libraryFactory: LibraryViewModel.Factory,
    documentRepository: OfflineDocumentRepository,
    searchFactory: SearchViewModel.Factory,
    vaultFactory: VaultViewModel.Factory,
    vaultRepository: VaultRepository,
    redactionRepository: com.localpdf.core.data.RedactionRepository,
    editorRepository: EditorRepository,
) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val layoutMode = when {
        widthDp >= 840 -> LayoutMode.Expanded
        widthDp >= 600 -> LayoutMode.Medium
        else -> LayoutMode.Compact
    }
    var destination by remember { mutableStateOf(AppDestination.Library) }
    var scannerOpen by remember { mutableStateOf(false) }
    var openDocumentId by remember { mutableStateOf<String?>(null) }
    val appScope = rememberCoroutineScope()

    val background = Brush.radialGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
        ),
        radius = 1500f,
    )

    Box(Modifier.fillMaxSize().background(background)) {
        if (openDocumentId != null) {
            val viewer: ViewerViewModel = viewModel(key = openDocumentId, factory = ViewerViewModel.Factory(openDocumentId!!, documentRepository))
            val viewerState by viewer.state.collectAsStateWithLifecycle()
            val activity = requireNotNull(LocalActivity.current) as FragmentActivity
            DocumentViewerScreen(
                viewerState,
                viewer::onAction,
                onBack = { openDocumentId = null },
                onMoveToVault = { id -> VaultAuthenticator.authenticate(activity, "Protect document", onSuccess = { appScope.launch { vaultRepository.moveToVault(id).onSuccess { openDocumentId = null; destination = AppDestination.Vault }.onFailure { Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show() } } }, onError = { Toast.makeText(activity, it, Toast.LENGTH_LONG).show() }) },
                onCreateRedactedCopy = { id, regions -> appScope.launch { redactionRepository.createPermanentCopy(id, regions).onSuccess { openDocumentId = it.id; Toast.makeText(activity, "Verified flattened copy created", Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show() } } },
                onCreateEditedCopy = { id, edits, watermark -> appScope.launch { editorRepository.createEditedCopy(id, edits.map { EditorPageEdit(it.pageId, it.rotationDegrees, it.deleted) }, watermark).onSuccess { openDocumentId = it.id; Toast.makeText(activity, "Edited copy created", Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show() } } },
                onShare = { id -> appScope.launch { documentRepository.prepareShareCopy(id).onSuccess { file -> val uri = androidx.core.content.FileProvider.getUriForFile(activity, "${activity.packageName}.files", file); val mime = if (file.extension == "pdf") "application/pdf" else "image/*"; val intent = Intent(Intent.ACTION_SEND).apply { type = mime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; activity.startActivity(Intent.createChooser(intent, "Share document")) }.onFailure { Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show() } } },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
            )
        } else if (scannerOpen) {
            if (cameraGranted) {
                ScannerScreen(
                    onClose = { scannerOpen = false },
                    onFinished = { pages ->
                        appScope.launch {
                            documentRepository.importCapturedPages(pages)
                            scannerOpen = false
                        }
                    },
                )
            } else ScannerPlaceholder(cameraGranted, cameraRequested, onRequestCamera, onOpenSettings) { scannerOpen = false }
        } else if (layoutMode == LayoutMode.Compact) {
            CompactShell(
                destination = destination,
                onDestination = { destination = it },
                onScan = { scannerOpen = true },
                libraryFactory = libraryFactory,
                searchFactory = searchFactory,
                onOpenDocument = { openDocumentId = it },
                vaultFactory = vaultFactory,
            )
        } else {
            WideShell(
                expanded = layoutMode == LayoutMode.Expanded,
                destination = destination,
                onDestination = { destination = it },
                onScan = { scannerOpen = true },
                libraryFactory = libraryFactory,
                searchFactory = searchFactory,
                onOpenDocument = { openDocumentId = it },
                vaultFactory = vaultFactory,
            )
        }
    }
}

@Composable
private fun CompactShell(
    destination: AppDestination,
    onDestination: (AppDestination) -> Unit,
    onScan: () -> Unit,
    libraryFactory: LibraryViewModel.Factory,
    searchFactory: SearchViewModel.Factory,
    onOpenDocument: (String) -> Unit,
    vaultFactory: VaultViewModel.Factory,
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
                    Icon(Icons.Filled.Add, contentDescription = "Scan document")
                }
            }
        },
    ) { padding ->
        DestinationContent(destination, Modifier.padding(padding), expanded = false, onScan = onScan, libraryFactory = libraryFactory, searchFactory = searchFactory, onOpenDocument = onOpenDocument, vaultFactory = vaultFactory)
    }
}

@Composable
private fun WideShell(
    expanded: Boolean,
    destination: AppDestination,
    onDestination: (AppDestination) -> Unit,
    onScan: () -> Unit,
    libraryFactory: LibraryViewModel.Factory,
    searchFactory: SearchViewModel.Factory,
    onOpenDocument: (String) -> Unit,
    vaultFactory: VaultViewModel.Factory,
) {
    Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            header = {
                Box(Modifier.padding(16.dp).size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Home, contentDescription = "LocalPDF", tint = Color.White)
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
        DestinationContent(destination, Modifier.weight(1f), expanded = expanded, onScan = onScan, libraryFactory = libraryFactory, searchFactory = searchFactory, onOpenDocument = onOpenDocument, vaultFactory = vaultFactory)
    }
}

@Composable
private fun DestinationContent(
    destination: AppDestination,
    modifier: Modifier,
    expanded: Boolean,
    onScan: () -> Unit,
    libraryFactory: LibraryViewModel.Factory,
    searchFactory: SearchViewModel.Factory,
    onOpenDocument: (String) -> Unit,
    vaultFactory: VaultViewModel.Factory,
) {
    Column(
        modifier = modifier.padding(horizontal = if (expanded) 40.dp else 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(destination.label, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        when (destination) {
            AppDestination.Library -> LibraryRoute(expanded, libraryFactory, onOpenDocument)
            AppDestination.Search -> SearchRoute(searchFactory, onOpenDocument)
            AppDestination.Vault -> VaultRoute(vaultFactory)
            AppDestination.Settings -> PrivacyDashboard()
        }
    }
}

@Composable private fun VaultRoute(factory: VaultViewModel.Factory) {
    val vm: VaultViewModel = viewModel(factory = factory); val documents by vm.documents.collectAsStateWithLifecycle(); val activity = requireNotNull(LocalActivity.current) as FragmentActivity
    fun auth(title: String, action: () -> Unit) = VaultAuthenticator.authenticate(activity, title, action) { Toast.makeText(activity, it, Toast.LENGTH_LONG).show() }
    VaultScreen(documents, onUnlock = { doc -> auth("Unlock ${doc.title}") { vm.open(doc.id, { Toast.makeText(activity, "Unlocked temporary copy created", Toast.LENGTH_SHORT).show() }, { Toast.makeText(activity, it, Toast.LENGTH_LONG).show() }) } }, onRestore = { doc -> auth("Restore ${doc.title}") { vm.restore(doc.id, {}, { Toast.makeText(activity, it, Toast.LENGTH_LONG).show() }) } })
}

@Composable
private fun SearchRoute(factory: SearchViewModel.Factory, onOpenDocument: (String) -> Unit) {
    val searchViewModel: SearchViewModel = viewModel(factory = factory)
    val state by searchViewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state, searchViewModel::onAction, onOpen = onOpenDocument)
}

@Composable
private fun LibraryRoute(expanded: Boolean, factory: LibraryViewModel.Factory, onOpenDocument: (String) -> Unit) {
    val viewModel: LibraryViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onAction(LibraryAction.ImportSelected(it.toString())) }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LibraryEffect.LaunchImportPicker -> importer.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp"))
                is LibraryEffect.Message -> Toast.makeText(context, effect.text, Toast.LENGTH_LONG).show()
                is LibraryEffect.OpenDocument -> onOpenDocument(effect.id)
            }
        }
    }
    LibraryScreen(state = state, columns = if (expanded) 3 else 1, onAction = viewModel::onAction, modifier = Modifier.fillMaxSize())
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
            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Your private document workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Scan your first page. Processing, OCR, search, and storage remain on your device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            Button(onClick = onScan, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
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
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
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
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF818CF8))
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

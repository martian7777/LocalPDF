package com.localpdf.feature.vault

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localpdf.core.designsystem.GlassSurface
import com.localpdf.core.model.Document
import com.localpdf.core.security.VaultRepository
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {
    val documents = repository.observeVault().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun open(id: String, onReady: (File) -> Unit, onError: (String) -> Unit) = viewModelScope.launch { repository.openAuthenticatedCopy(id).onSuccess(onReady).onFailure { onError(it.message ?: "Unable to open vault file") } }
    fun restore(id: String, onDone: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch { repository.restoreFromVault(id).onSuccess { onDone() }.onFailure { onError(it.message ?: "Unable to restore vault file") } }
    class Factory(private val repository: VaultRepository) : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = VaultViewModel(repository) as T }
}

object VaultAuthenticator {
    const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    fun authenticate(activity: FragmentActivity, title: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(code: Int, message: CharSequence) { if (code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_NEGATIVE_BUTTON) onError(message.toString()) }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle("Use a strong biometric or device screen lock").setAllowedAuthenticators(AUTHENTICATORS).build())
    }
}

@Composable fun VaultScreen(documents: List<Document>, onUnlock: (Document) -> Unit, onRestore: (Document) -> Unit, modifier: Modifier = Modifier) {
    if (documents.isEmpty()) GlassSurface(modifier.fillMaxWidth()) { Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Private Vault", style = MaterialTheme.typography.titleLarge); Text("No encrypted documents. Move a completed document here from its viewer.") } }
    else LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(documents, key = { it.id }) { document -> GlassSurface(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text(document.title, style = MaterialTheme.typography.titleMedium); Text("AES-256-GCM · device authentication required", style = MaterialTheme.typography.bodySmall); Row { TextButton(onClick = { onUnlock(document) }) { Text("Unlock") }; TextButton(onClick = { onRestore(document) }) { Text("Restore") } } } } } }
}

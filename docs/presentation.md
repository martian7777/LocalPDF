# Presentation Architecture — LocalPDF

## Presentation Pattern: Unidirectional Data Flow (UDF)

All features in LocalPDF adopt a strict Unidirectional Data Flow pattern powered by Jetpack Compose, Kotlin `StateFlow`, and sealed interface hierarchies for user actions and one-off side effects.

## State Modeling Structure

Every feature screen defines three core contracts:

1. **`UiState`** (Immutable data class): Holds the complete, deterministic snapshot of the screen UI.
2. **`UiAction`** (Sealed interface): Represents all possible user interactions (button clicks, gestures, text input).
3. **`UiEffect`** (Sealed interface): Represents one-off transient events (Navigation events, Toast messages, Biometric authentication triggers, System Share sheets).

```kotlin
// Example from :feature:viewer

data class DocumentViewerUiState(
    val isLoading: Boolean = true,
    val document: Document? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 0,
    val isOcrLayerVisible: Boolean = false,
    val selectedOcrBlock: OcrBlock? = null,
    val searchQuery: String = "",
    val searchMatches: List<SearchMatch> = emptyList(),
    val errorMessage: String? = null
)

sealed interface DocumentViewerUiAction {
    data class PageChanged(val pageIndex: Int) : DocumentViewerUiAction
    data class ToggleOcrLayer(val isVisible: Boolean) : DocumentViewerUiAction
    data class SelectOcrBlock(val block: OcrBlock?) : DocumentViewerUiAction
    data class SearchText(val query: String) : DocumentViewerUiAction
    data object ShareDocument : DocumentViewerUiAction
    data object OpenRedactionStudio : DocumentViewerUiAction
    data object OpenPdfTools : DocumentViewerUiAction
}

sealed interface DocumentViewerUiEffect {
    data class NavigateToRedaction(val documentId: String) : DocumentViewerUiEffect
    data class NavigateToEditor(val documentId: String) : DocumentViewerUiEffect
    data class LaunchSystemShare(val fileUri: Uri) : DocumentViewerUiEffect
    data class ShowSnackbar(val message: String) : DocumentViewerUiEffect
}
```

## ViewModel Guidelines

1. **State Exposure**: Expose `StateFlow<UiState>` via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InitialState)`.
2. **Single Public Entry Point**: All actions dispatched through a single `fun onAction(action: UiAction)` function with exhaustive `when` handling.
3. **No Direct Android Context/View references**: ViewModels must never hold references to `Context`, `Activity`, `View`, or `NavController`. Use injected Android application context or abstractions where needed.
4. **Lifecycle-Safe Effects**: Dispatch one-time side effects via `Channel<UiEffect>(Channel.BUFFERED)` exposed as a `Flow<UiEffect>` and consumed in Compose using `LaunchedEffectWithLifecycle`.

## Composable Contract & Previews

Every screen consists of a root stateful container and a stateless composable:

```kotlin
@Composable
fun DocumentViewerRoute(
    documentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRedaction: (String) -> Unit,
    viewModel: DocumentViewerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    ObserveAsEvents(viewModel.uiEffect) { effect ->
        when (effect) {
            is DocumentViewerUiEffect.NavigateToRedaction -> onNavigateToRedaction(effect.documentId)
            is DocumentViewerUiEffect.LaunchSystemShare -> { /* Launch share intent */ }
            is DocumentViewerUiEffect.ShowSnackbar -> { /* Show snackbar */ }
        }
    }

    DocumentViewerScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = onNavigateBack
    )
}

@Composable
fun DocumentViewerScreen(
    state: DocumentViewerUiState,
    onAction: (DocumentViewerUiAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pure stateless rendering based on state and emitting actions
}
```

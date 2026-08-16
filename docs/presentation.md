# Presentation Layer

## Responsibilities

Presentation should:

- expose UI state
- handle user actions
- coordinate navigation/effects at boundaries
- call domain/repository APIs through ViewModels
- transform domain data for rendering

## Preferred Pattern

```kotlin
data class FeatureUiState(
    val isLoading: Boolean = false,
    val items: List<ItemUiModel> = emptyList(),
    val error: UiText? = null
)

sealed interface FeatureAction {
    data object Retry : FeatureAction
    data class ItemClicked(val id: String) : FeatureAction
}
```

Composable:

```kotlin
@Composable
fun FeatureScreen(
    state: FeatureUiState,
    onAction: (FeatureAction) -> Unit
)
```

## State Rules

- immutable state
- `StateFlow` exposed publicly
- mutable flows remain private
- UI state should represent renderable truth
- avoid duplicate state ownership
- do not store large raw domain graphs in UI state if unnecessary

## Effects

Use one-time effects only for genuine one-shot behavior such as:

- navigation
- snackbar
- launching external activity
- permission request

Do not introduce an effect type by default.

## ViewModel Rules

ViewModels should not:

- know Compose internals
- hold Activity/Fragment references
- own unrelated global state
- perform direct UI rendering work

Use `viewModelScope`.

Respect cancellation.

## Process Death

Determine which state should survive:

- configuration change
- process death
- app restart

Persist user work where needed.

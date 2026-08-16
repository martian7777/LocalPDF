# Data Layer

## Project Data Sources

Fill for this application:

```text
Remote API: [YES/NO + TECHNOLOGY]
Local DB: [ROOM / OTHER / NONE]
Preferences: [DATASTORE / OTHER]
Files: [LOCAL / CLOUD / NONE]
Device APIs: [CAMERA / LOCATION / BLUETOOTH / OTHER]
```

## Repository Rules

Repositories should hide implementation details.

Example:

```kotlin
interface ItemRepository {
    fun observeItems(): Flow<List<Item>>
    suspend fun getItem(id: ItemId): Item?
    suspend fun save(item: Item)
}
```

## Model Boundaries

Prefer separate models where useful:

```text
DTO
 ↓
Domain
 ↓
UI
```

and:

```text
Entity
 ↓
Domain
```

Do not expose Room entities or network DTOs directly to UI unless intentionally justified.

## Source of Truth

Define per domain:

`[LOCAL DB / REMOTE / HYBRID]`

## Error Mapping

Map:

- HTTP errors
- serialization failures
- database failures
- offline conditions

into meaningful application/domain errors.

Do not surface raw infrastructure exceptions directly to UI.

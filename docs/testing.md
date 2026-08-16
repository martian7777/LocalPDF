# Testing Strategy & Quality Gates — LocalPDF

## Testing Pyramid

```text
               ▲
              / \
             /   \     UI & End-to-End Tests (Compose Test Rule, Robot Pattern)
            /     \
           /───────\   Integration Tests (Room FTS5, WorkManager, OpenCV Pipeline)
          /         \
         /───────────\ Unit Tests (Use Cases, ViewModels, Entity Extractors, Parsers)
```

## 1. Unit Tests (`:core:*`, `:feature:*`)

- **Entity & Regex Extractors**: Test invoice number regexes, date normalizers, currency converters, IBAN validators with extensive edge-case datasets (e.g. `InvoiceExtractorTest`, `ReceiptExtractorTest`).
- **Use Cases**: Test domain workflows with mocked repositories (`ScanAndProcessDocumentUseCaseTest`, `SearchDocumentsUseCaseTest`).
- **ViewModels**: Test MVI state transitions using Turbine and Coroutine test dispatchers (`ScannerViewModelTest`, `DocumentViewerViewModelTest`).

## 2. Integration Tests

- **Room Database & SQLite FTS5**: In-memory database testing verifying FTS5 full-text queries, phrase search, snippet highlight extraction, and schema migrations (`DocumentDaoTest`, `FtsSearchTest`, `DatabaseMigrationTest`).
- **OpenCV Computer Vision**: Tests verifying contour detection, perspective quad calculation, and deskew angle math on fixed test image fixtures (`DocumentEdgeDetectorTest`).
- **ONNX Model Inference**: Integration tests running quantized PaddleOCR and Document Classifier models on sample bitmap assets to assert deterministic inference outputs.

## 3. UI / Compose Tests

- Screen-level UI testing with `createAndroidComposeRule` following the **Robot Pattern**:
```kotlin
@Test
fun documentViewer_whenOcrToggled_showsBoundingBoxes() {
    documentViewerRobot {
        loadDocument(sampleDocument)
        toggleOcrLayer()
        assertBoundingBoxesVisible()
        clickFirstOcrBlock()
        assertOcrCorrectionDialogDisplayed()
    }
}
```

## Quality Gates & CI

- **`./gradlew test`**: All unit and Robolectric tests pass.
- **`./gradlew lint`**: Zero Android Lint errors or high-severity warnings.
- **`./gradlew detekt`**: Kotlin static code analysis passes.
- **LeakCanary**: Monitored in debug builds to prevent Activity/Bitmap memory leaks.

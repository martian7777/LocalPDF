# Data Architecture & Repositories — LocalPDF

## Repository Pattern & Contracts

The data layer implements domain repository interfaces, abstracting data sources (Room Database, File System, Native C++/ONNX ML Engines, and Android Keystore).

### Core Repository Contracts

```kotlin
interface DocumentRepository {
    fun observeDocuments(filter: DocumentFilter): Flow<List<Document>>
    fun observeDocumentById(documentId: String): Flow<Document?>
    suspend fun getDocumentById(documentId: String): Document?
    suspend fun saveDocument(document: Document, pages: List<DocumentPage>): Result<Document>
    suspend fun updateDocumentTitle(documentId: String, title: String): Result<Unit>
    suspend fun updateClassification(documentId: String, classification: DocumentClassification): Result<Unit>
    suspend fun setFavorite(documentId: String, isFavorite: Boolean): Result<Unit>
    suspend fun setVaulted(documentId: String, isVaulted: Boolean): Result<Unit>
    suspend fun deleteDocument(documentId: String): Result<Unit>
}

interface OcrRepository {
    suspend fun performOcrOnImage(imagePath: String): Result<List<OcrBlock>>
    suspend fun performOcrOnPdf(pdfPath: String, pageIndices: List<Int>): Result<Map<Int, List<OcrBlock>>>
    suspend fun updateOcrCorrection(blockId: String, correctedText: String): Result<Unit>
}

interface PdfRepository {
    suspend fun renderPageBitmap(pdfPath: String, pageIndex: Int, targetWidth: Int): Result<Bitmap>
    suspend fun createSearchablePdf(pages: List<Pair<Bitmap, List<OcrBlock>>>, outputPath: String): Result<File>
    suspend fun mergePdfs(pdfPaths: List<String>, outputPath: String): Result<File>
    suspend fun splitPdf(pdfPath: String, pageRanges: List<IntRange>, outputDirectory: String): Result<List<File>>
    suspend fun redactAndFlattenPdf(pdfPath: String, redactions: Map<Int, List<BoundingBox>>, outputPath: String): Result<File>
    suspend fun addWatermark(pdfPath: String, watermarkText: String, outputPath: String): Result<File>
    suspend fun compressPdf(pdfPath: String, qualityPercent: Int, outputPath: String): Result<File>
}

interface SearchRepository {
    fun searchFullText(query: String, filter: SearchFilter): Flow<List<SearchResult>>
}

interface VaultRepository {
    suspend fun moveToVault(documentId: String): Result<Unit>
    suspend fun restoreFromVault(documentId: String): Result<Unit>
    suspend fun getDecryptedFile(documentId: String): Result<File>
}
```

## Data Mapping Strategy

Data flows through explicit mapping layers:
```text
Room Entity / DB Row       ──────> Domain Model (e.g. DocumentEntity.toDomainModel())
ONNX Output Tensor Tensors ──────> Domain Model (e.g. TensorBuffer.toOcrBlocks())
CV Mat Contour Points      ──────> Domain Model (e.g. MatOfPoint2f.toQuadCorners())
Domain Model               ──────> Presentation UI Model (e.g. Document.toUiModel())
```
No Room `@Entity` or native JNI pointers ever leak into presentation composables or ViewModels.

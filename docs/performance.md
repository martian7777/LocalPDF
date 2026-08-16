# Performance & Memory Management — LocalPDF

## Performance Goals

- **Camera Preview**: Sustained 30/60 FPS during real-time edge detection overlay.
- **OCR Latency**: < 450ms per A4 page inference on Snapdragon 7/8 series or Dimensity equivalent; < 900ms on entry-level hardware.
- **PDF Page Rendering**: 60 FPS smooth scrolling in Compose viewer using tile/bitmap caching.
- **FTS5 Search Query**: < 30ms query execution across 5,000 indexed documents.
- **Zero Native Memory Leaks**: Predictable garbage collection and prompt native resource reclamation.

## Native & Image Memory Management

Document processing deals with high-resolution bitmaps (300 DPI A4 page = ~2480x3508 pixels = ~34MB uncompressed in `ARGB_8888`). Unmanaged bitmap allocations quickly trigger Out-Of-Memory (OOM) crashes.

### 1. OpenCV Mat Lifecycle
- Every OpenCV `Mat` created during contour detection or perspective transformation must be explicitly released in a `finally` block or wrapped in a scoped auto-closable helper:
```kotlin
inline fun <R> Mat.use(block: (Mat) -> R): R {
    try {
        return block(this)
    } finally {
        this.release()
    }
}
```

### 2. Bitmap Re-use & Pixel Format
- Use `Bitmap.Config.RGB_565` or `HARDWARE` bitmaps for read-only viewer rendering to halve memory consumption compared to `ARGB_8888`.
- Utilize an in-memory `LruCache<Int, Bitmap>` (max 3-5 rendered pages in memory simultaneously) in `:feature:viewer`.
- Recycle intermediate bitmaps immediately once written to disk.

### 3. ONNX Runtime Mobile Optimization
- **Execution Providers**: Initialize `OrtSession` with NNAPI (Android Neural Networks API) or XNNPACK execution providers where available, falling back to multi-threaded CPU.
- **Quantization**: Use INT8 quantized ONNX models (`ppocr_det_int8.onnx`, `ppocr_rec_int8.onnx`) reducing model binary footprint by ~70% and memory footprint by ~60% with negligible accuracy drop.
- **Threading**: Bind inference threads to background coroutine workers (`Dispatchers.Default`) with thread pool capped to `Runtime.getRuntime().availableProcessors().coerceAtMost(4)`.

## Jetpack Compose Rendering Efficiency

- Wrap zoomable/pannable PDF page canvases in `Modifier.graphicsLayer` to avoid recomposition loops during pinch-to-zoom gestures.
- Use `LazyColumn` / `LazyVerticalGrid` with stable keys (`key = { it.id }`) and `@Immutable` UI models.
- Avoid passing lambda instances that capture changing scope; pass method references or stable event handlers.

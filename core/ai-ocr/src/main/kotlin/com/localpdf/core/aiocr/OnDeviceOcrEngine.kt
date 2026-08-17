package com.localpdf.core.aiocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.localpdf.core.model.BoundingBox
import com.localpdf.core.model.OcrBlock
import java.io.Closeable
import java.nio.FloatBuffer
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class OnDeviceOcrEngine(private val context: Context) : Closeable {
    private val mutex = Mutex()
    private val environment by lazy { OrtEnvironment.getEnvironment() }
    private val dictionary by lazy { loadDictionary() }
    private val recognitionSessionLazy = lazy { createSession("models/rec/inference.onnx", REC_SHA) }
    private val detectionSessionLazy = lazy { createSession("models/det/inference.onnx", DET_SHA) }
    private val recognitionSession by recognitionSessionLazy
    private val detectionSession by detectionSessionLazy

    suspend fun recognizePage(imagePath: String, pageId: String): List<OcrBlock> = mutex.withLock {
        withContext(Dispatchers.Default) {
            val bitmap = requireNotNull(BitmapFactory.decodeFile(imagePath)) { "Unable to decode OCR page" }
            try { detectRegions(bitmap).mapNotNull { rect -> recognizeLine(bitmap, rect, pageId) } } finally { bitmap.recycle() }
        }
    }

    /** Prefers the shipped DBNet detector; falls back to classical morphology if the model output is unexpected. */
    private fun detectRegions(bitmap: Bitmap): List<Rect> = try {
        detectTextRegionsWithModel(bitmap).ifEmpty { detectLines(bitmap) }
    } catch (error: Exception) {
        detectLines(bitmap)
    }

    private fun detectTextRegionsWithModel(bitmap: Bitmap): List<Rect> {
        val stride = 32
        val resizeLong = 960
        val longSide = maxOf(bitmap.width, bitmap.height).toFloat()
        val scale = resizeLong / longSide
        val targetWidth = (((bitmap.width * scale).toInt().coerceAtLeast(stride) + stride - 1) / stride) * stride
        val targetHeight = (((bitmap.height * scale).toInt().coerceAtLeast(stride) + stride - 1) / stride) * stride
        val resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val input = FloatArray(3 * targetHeight * targetWidth)
        val pixels = IntArray(targetWidth * targetHeight)
        resized.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
        // NormalizeImage applies mean/std positionally to the BGR-ordered tensor produced by DecodeImage(img_mode: BGR).
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        for (y in 0 until targetHeight) for (x in 0 until targetWidth) {
            val pixel = pixels[y * targetWidth + x]
            val bgr = floatArrayOf((pixel and 0xff) / 255f, ((pixel shr 8) and 0xff) / 255f, ((pixel shr 16) and 0xff) / 255f)
            for (channel in 0..2) input[channel * targetHeight * targetWidth + y * targetWidth + x] = (bgr[channel] - mean[channel]) / std[channel]
        }
        if (resized !== bitmap) resized.recycle()

        OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), longArrayOf(1, 3, targetHeight.toLong(), targetWidth.toLong())).use { tensor ->
            detectionSession.run(mapOf(detectionSession.inputNames.first() to tensor)).use { output ->
                val probabilityMap = extractProbabilityMap(requireNotNull(output[0].value))
                return decodeProbabilityMap(probabilityMap, targetHeight, targetWidth, bitmap.width, bitmap.height)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractProbabilityMap(raw: Any): Array<FloatArray> = when (val outer = (raw as Array<*>)[0]) {
        is Array<*> -> when (outer[0]) {
            is Array<*> -> (outer as Array<Array<FloatArray>>)[0] // rank 4: [1][channels][H][W] -> take channel 0
            is FloatArray -> outer as Array<FloatArray> // rank 3: [1][H][W]
            else -> error("Unsupported detection output shape")
        }
        else -> error("Unsupported detection output shape")
    }

    private fun decodeProbabilityMap(probability: Array<FloatArray>, mapHeight: Int, mapWidth: Int, originalWidth: Int, originalHeight: Int): List<Rect> {
        val binary = Mat(mapHeight, mapWidth, CvType.CV_8UC1)
        val flatBytes = ByteArray(mapWidth * mapHeight)
        for (y in 0 until mapHeight) for (x in 0 until mapWidth) flatBytes[y * mapWidth + x] = if (probability[y][x] > DET_PROB_THRESHOLD) (-1).toByte() else 0
        binary.put(0, 0, flatBytes)
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        val scaleX = originalWidth.toFloat() / mapWidth
        val scaleY = originalHeight.toFloat() / mapHeight
        try {
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            return contours.mapNotNull { contour ->
                val box = Imgproc.boundingRect(contour)
                if (box.width < 3 || box.height < 3) return@mapNotNull null
                val score = boxScore(probability, box, mapWidth, mapHeight)
                if (score < DET_BOX_THRESHOLD) return@mapNotNull null
                unclipAndScale(box, mapWidth, mapHeight, scaleX, scaleY)
            }.sortedWith(compareBy<Rect> { it.y }.thenBy { it.x }).take(300)
        } finally { contours.forEach(Mat::release); hierarchy.release(); binary.release() }
    }

    private fun boxScore(probability: Array<FloatArray>, box: Rect, mapWidth: Int, mapHeight: Int): Float {
        var sum = 0f; var count = 0
        for (y in box.y until (box.y + box.height).coerceAtMost(mapHeight)) for (x in box.x until (box.x + box.width).coerceAtMost(mapWidth)) { sum += probability[y][x]; count++ }
        return if (count == 0) 0f else sum / count
    }

    /** Expands the box outward by DBPostProcess's unclip formula (area * ratio / perimeter) before rescaling to source pixels. */
    private fun unclipAndScale(box: Rect, mapWidth: Int, mapHeight: Int, scaleX: Float, scaleY: Float): Rect {
        val perimeter = 2.0 * (box.width + box.height)
        val area = box.width.toDouble() * box.height.toDouble()
        val distance = if (perimeter <= 0) 0.0 else area * DET_UNCLIP_RATIO / perimeter
        val left = (box.x - distance).coerceAtLeast(0.0)
        val top = (box.y - distance).coerceAtLeast(0.0)
        val right = (box.x + box.width + distance).coerceAtMost(mapWidth.toDouble())
        val bottom = (box.y + box.height + distance).coerceAtMost(mapHeight.toDouble())
        return Rect((left * scaleX).toInt(), (top * scaleY).toInt(), ((right - left) * scaleX).toInt().coerceAtLeast(1), ((bottom - top) * scaleY).toInt().coerceAtLeast(1))
    }

    private fun detectLines(bitmap: Bitmap): List<Rect> {
        val rgba = Mat(); val gray = Mat(); val binary = Mat(); val kernel = Mat(); val hierarchy = Mat(); val contours = mutableListOf<MatOfPoint>()
        return try {
            Utils.bitmapToMat(bitmap, rgba); Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)
            val kernelWidth = (bitmap.width / 35).coerceIn(12, 80)
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(kernelWidth.toDouble(), 3.0)).copyTo(kernel)
            Imgproc.morphologyEx(binary, binary, Imgproc.MORPH_CLOSE, kernel)
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            contours.map(Imgproc::boundingRect).filter { it.width > bitmap.width * .04 && it.height > 8 && it.height < bitmap.height * .2 }
                .sortedWith(compareBy<Rect> { it.y }.thenBy { it.x }).take(300)
        } finally { contours.forEach(Mat::release); hierarchy.release(); kernel.release(); binary.release(); gray.release(); rgba.release() }
    }

    private fun recognizeLine(source: Bitmap, rect: Rect, pageId: String): OcrBlock? {
        val safe = Rect(rect.x.coerceAtLeast(0), rect.y.coerceAtLeast(0), rect.width.coerceAtMost(source.width - rect.x), rect.height.coerceAtMost(source.height - rect.y))
        if (safe.width <= 0 || safe.height <= 0) return null
        val crop = Bitmap.createBitmap(source, safe.x, safe.y, safe.width, safe.height)
        val targetWidth = (48f * safe.width / safe.height).toInt().coerceIn(16, 320)
        val scaled = Bitmap.createScaledBitmap(crop, targetWidth, 48, true)
        val input = FloatArray(3 * 48 * 320)
        val pixels = IntArray(targetWidth * 48); scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, 48)
        for (y in 0 until 48) for (x in 0 until 320) for (channel in 0..2) {
            val value = if (x < targetWidth) { val pixel = pixels[y * targetWidth + x]; ((pixel shr (16 - channel * 8)) and 0xff) / 127.5f - 1f } else 0f
            input[channel * 48 * 320 + y * 320 + x] = value
        }
        crop.recycle(); if (scaled !== crop) scaled.recycle()
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), longArrayOf(1, 3, 48, 320)).use { tensor ->
            recognitionSession.run(mapOf(recognitionSession.inputNames.first() to tensor)).use { output ->
                @Suppress("UNCHECKED_CAST") val logits = output[0].value as Array<Array<FloatArray>>
                val decoded = decodeCtc(logits[0])
                if (decoded.first.isBlank() || decoded.second < .35f) return null
                return OcrBlock(UUID.randomUUID().toString(), pageId, decoded.first, decoded.second, BoundingBox(safe.x.toFloat() / source.width, safe.y.toFloat() / source.height, (safe.x + safe.width).toFloat() / source.width, (safe.y + safe.height).toFloat() / source.height))
            }
        }
    }

    private fun decodeCtc(steps: Array<FloatArray>): Pair<String, Float> {
        val text = StringBuilder(); var previous = -1; var confidence = 0f; var count = 0
        for (scores in steps) {
            val index = scores.indices.maxByOrNull(scores::get) ?: 0
            if (index != 0 && index != previous && index - 1 in dictionary.indices) { text.append(dictionary[index - 1]); confidence += scores[index]; count++ }
            previous = index
        }
        return text.toString().filter { it.code in 32..126 } to if (count == 0) 0f else confidence / count
    }

    private fun loadDictionary(): List<String> {
        val yaml = context.assets.open("models/rec/inference.yml").bufferedReader().readLines()
        val start = yaml.indexOfFirst { it.trim() == "character_dict:" }
        require(start >= 0) { "OCR dictionary missing" }
        return yaml.drop(start + 1).takeWhile { it.startsWith("  - ") }.map { it.removePrefix("  - ") }
    }

    private fun createSession(path: String, expectedHash: String): OrtSession {
        val bytes = context.assets.open(path).use { it.readBytes() }
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(actual == expectedHash) { "OCR model integrity check failed" }
        val options = OrtSession.SessionOptions().apply { setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceAtMost(4)); setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT) }
        return environment.createSession(bytes, options).also { options.close() }
    }

    override fun close() {
        if (recognitionSessionLazy.isInitialized()) recognitionSessionLazy.value.close()
        if (detectionSessionLazy.isInitialized()) detectionSessionLazy.value.close()
    }

    companion object {
        private const val REC_SHA = "da72dc72ca4dc220df0dfde68c1dedc31c58d3e76a25871122e5056227d50092"
        private const val DET_SHA = "a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d"
        private const val DET_PROB_THRESHOLD = 0.3f
        private const val DET_BOX_THRESHOLD = 0.6f
        private const val DET_UNCLIP_RATIO = 1.5
    }
}

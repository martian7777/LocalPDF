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
    private val recognitionSession by lazy { createSession("models/rec/inference.onnx", REC_SHA) }

    suspend fun recognizePage(imagePath: String, pageId: String): List<OcrBlock> = mutex.withLock {
        withContext(Dispatchers.Default) {
            val bitmap = requireNotNull(BitmapFactory.decodeFile(imagePath)) { "Unable to decode OCR page" }
            try { detectLines(bitmap).mapNotNull { rect -> recognizeLine(bitmap, rect, pageId) } } finally { bitmap.recycle() }
        }
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

    override fun close() { recognitionSession.close() }
    companion object { private const val REC_SHA = "da72dc72ca4dc220df0dfde68c1dedc31c58d3e76a25871122e5056227d50092" }
}

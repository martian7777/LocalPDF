package com.localpdf.core.cvscanner

import androidx.camera.core.ImageProxy
import com.localpdf.core.model.BoundingBox
import java.nio.ByteBuffer
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

data class ScanAnalysis(val bounds: BoundingBox?, val blurScore: Double, val lowLight: Boolean)

class DocumentEdgeDetector {
    fun analyze(image: ImageProxy): ScanAnalysis {
        val width = image.width
        val height = image.height
        val plane = image.planes[0]
        val gray = Mat(height, width, CvType.CV_8UC1)
        val packed = ByteArray(width * height)
        copyLuma(plane.buffer, packed, width, height, plane.rowStride, plane.pixelStride)
        gray.put(0, 0, packed)
        val blurred = Mat()
        val edges = Mat()
        val hierarchy = Mat()
        val contours = mutableListOf<MatOfPoint>()
        try {
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(blurred, edges, 75.0, 200.0)
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            val best = contours.asSequence().mapNotNull { contour ->
                val curve = MatOfPoint2f(*contour.toArray())
                val approx = MatOfPoint2f()
                try {
                    Imgproc.approxPolyDP(curve, approx, Imgproc.arcLength(curve, true) * 0.02, true)
                    val area = Imgproc.contourArea(approx)
                    if (approx.total() == 4L && area > width * height * .18) area to approx.toArray() else null
                } finally { curve.release(); approx.release() }
            }.maxByOrNull { it.first }
            val bounds = best?.second?.let { points ->
                BoundingBox(
                    points.minOf { it.x }.toFloat() / width,
                    points.minOf { it.y }.toFloat() / height,
                    points.maxOf { it.x }.toFloat() / width,
                    points.maxOf { it.y }.toFloat() / height,
                )
            }
            val laplacian = Mat()
            val mean = org.opencv.core.Core.mean(gray).`val`[0]
            val blurScore = try {
                Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
                val mu = Mat(); val sigma = Mat()
                try { org.opencv.core.Core.meanStdDev(laplacian, mu, sigma); sigma.get(0, 0)[0].let { it * it } } finally { mu.release(); sigma.release() }
            } finally { laplacian.release() }
            return ScanAnalysis(bounds, blurScore, mean < 55.0)
        } finally {
            contours.forEach(Mat::release); hierarchy.release(); edges.release(); blurred.release(); gray.release(); image.close()
        }
    }

    private fun copyLuma(buffer: ByteBuffer, output: ByteArray, width: Int, height: Int, rowStride: Int, pixelStride: Int) {
        buffer.rewind()
        for (row in 0 until height) for (column in 0 until width) {
            output[row * width + column] = buffer.get(row * rowStride + column * pixelStride)
        }
    }
}

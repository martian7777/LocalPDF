package com.localpdf.core.cvscanner

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

data class QuadPoint(val x: Float, val y: Float)

data class DocumentQuad(
    val topLeft: QuadPoint,
    val topRight: QuadPoint,
    val bottomRight: QuadPoint,
    val bottomLeft: QuadPoint,
) {
    companion object {
        fun inset(margin: Float = 0.08f) = DocumentQuad(
            QuadPoint(margin, margin),
            QuadPoint(1f - margin, margin),
            QuadPoint(1f - margin, 1f - margin),
            QuadPoint(margin, 1f - margin),
        )
    }
}

data class ScanAnalysis(val quad: DocumentQuad?, val blurScore: Double, val lowLight: Boolean)

class DocumentEdgeDetector {
    fun analyze(image: ImageProxy): ScanAnalysis {
        val rawWidth = image.width
        val rawHeight = image.height
        val plane = image.planes[0]
        val raw = Mat(rawHeight, rawWidth, CvType.CV_8UC1)
        val packed = ByteArray(rawWidth * rawHeight)
        copyLuma(plane.buffer, packed, rawWidth, rawHeight, plane.rowStride, plane.pixelStride)
        raw.put(0, 0, packed)
        // ImageAnalysis buffers are delivered in sensor orientation; rotate to the upright,
        // as-displayed frame so the returned quad lines up with PreviewView and the saved JPEG.
        val gray = rotateMat(raw, image.imageInfo.rotationDegrees)
        raw.release()
        val width = gray.cols()
        val height = gray.rows()
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
            val quad = best?.second?.let { points -> orderPoints(points, width, height) }
            val laplacian = Mat()
            val mean = Core.mean(gray).`val`[0]
            val blurScore = try {
                Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
                val mu = MatOfDouble(); val sigma = MatOfDouble()
                try { Core.meanStdDev(laplacian, mu, sigma); sigma.get(0, 0)[0].let { it * it } } finally { mu.release(); sigma.release() }
            } finally { laplacian.release() }
            return ScanAnalysis(quad, blurScore, mean < 55.0)
        } finally {
            contours.forEach(Mat::release); hierarchy.release(); edges.release(); blurred.release(); gray.release(); image.close()
        }
    }

    private fun rotateMat(source: Mat, rotationDegrees: Int): Mat {
        val normalized = ((rotationDegrees % 360) + 360) % 360
        if (normalized == 0) return source.clone()
        val rotated = Mat()
        val code = when (normalized) { 90 -> Core.ROTATE_90_CLOCKWISE; 180 -> Core.ROTATE_180; else -> Core.ROTATE_90_COUNTERCLOCKWISE }
        Core.rotate(source, rotated, code)
        return rotated
    }

    private fun orderPoints(points: Array<Point>, width: Int, height: Int): DocumentQuad {
        val sums = points.map { it.x + it.y }
        val diffs = points.map { it.x - it.y }
        val topLeft = points[sums.indices.minBy { sums[it] }]
        val bottomRight = points[sums.indices.maxBy { sums[it] }]
        val topRight = points[diffs.indices.maxBy { diffs[it] }]
        val bottomLeft = points[diffs.indices.minBy { diffs[it] }]
        fun norm(point: Point) = QuadPoint((point.x / width).toFloat(), (point.y / height).toFloat())
        return DocumentQuad(norm(topLeft), norm(topRight), norm(bottomRight), norm(bottomLeft))
    }

    private fun copyLuma(buffer: ByteBuffer, output: ByteArray, width: Int, height: Int, rowStride: Int, pixelStride: Int) {
        buffer.rewind()
        for (row in 0 until height) for (column in 0 until width) {
            output[row * width + column] = buffer.get(row * rowStride + column * pixelStride)
        }
    }
}

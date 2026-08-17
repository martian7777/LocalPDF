package com.localpdf.core.cvscanner

import android.graphics.Bitmap
import kotlin.math.sqrt
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

enum class PageFilter { ORIGINAL, COLOR_ENHANCED, GRAYSCALE, BLACK_AND_WHITE }

object PagePreprocessor {
    fun process(source: Bitmap, quad: DocumentQuad, filter: PageFilter): Bitmap {
        val width = source.width.toDouble()
        val height = source.height.toDouble()
        val srcPoints = MatOfPoint2f(
            Point(quad.topLeft.x * width, quad.topLeft.y * height),
            Point(quad.topRight.x * width, quad.topRight.y * height),
            Point(quad.bottomRight.x * width, quad.bottomRight.y * height),
            Point(quad.bottomLeft.x * width, quad.bottomLeft.y * height),
        )
        val outWidth = maxOf(distance(quad.topLeft, quad.topRight, width, height), distance(quad.bottomLeft, quad.bottomRight, width, height)).coerceAtLeast(1.0)
        val outHeight = maxOf(distance(quad.topLeft, quad.bottomLeft, width, height), distance(quad.topRight, quad.bottomRight, width, height)).coerceAtLeast(1.0)
        val dstPoints = MatOfPoint2f(Point(0.0, 0.0), Point(outWidth, 0.0), Point(outWidth, outHeight), Point(0.0, outHeight))

        val srcMat = Mat()
        val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val warped = Mat()
        val filtered: Mat
        val result: Bitmap
        try {
            Utils.bitmapToMat(source, srcMat)
            Imgproc.warpPerspective(srcMat, warped, transform, Size(outWidth, outHeight))
            filtered = applyFilter(warped, filter)
            result = Bitmap.createBitmap(filtered.cols(), filtered.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(filtered, result)
            filtered.release()
        } finally {
            srcMat.release(); transform.release(); warped.release(); srcPoints.release(); dstPoints.release()
        }
        return result
    }

    private fun distance(a: QuadPoint, b: QuadPoint, width: Double, height: Double): Double {
        val dx = (a.x - b.x) * width
        val dy = (a.y - b.y) * height
        return sqrt(dx * dx + dy * dy)
    }

    private fun applyFilter(input: Mat, filter: PageFilter): Mat = when (filter) {
        PageFilter.ORIGINAL -> input.clone()
        PageFilter.COLOR_ENHANCED -> Mat().also { input.convertTo(it, -1, 1.15, 8.0) }
        PageFilter.GRAYSCALE -> {
            val gray = Mat()
            Imgproc.cvtColor(input, gray, Imgproc.COLOR_RGBA2GRAY)
            val rgba = Mat()
            Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA)
            gray.release()
            rgba
        }
        PageFilter.BLACK_AND_WHITE -> {
            val gray = Mat()
            Imgproc.cvtColor(input, gray, Imgproc.COLOR_RGBA2GRAY)
            val thresholded = Mat()
            Imgproc.adaptiveThreshold(gray, thresholded, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 25, 15.0)
            val rgba = Mat()
            Imgproc.cvtColor(thresholded, rgba, Imgproc.COLOR_GRAY2RGBA)
            gray.release(); thresholded.release()
            rgba
        }
    }
}

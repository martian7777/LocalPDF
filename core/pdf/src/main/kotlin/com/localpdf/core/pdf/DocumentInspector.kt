package com.localpdf.core.pdf

import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.localpdf.core.model.OcrBlock
import com.localpdf.core.model.RedactionRegion

data class InspectedDocument(val pageCount: Int, val widthPx: Int, val heightPx: Int)

object DocumentInspector {
    fun inspect(file: File, mimeType: String): InspectedDocument = when (mimeType) {
        "application/pdf" -> inspectPdf(file)
        "image/jpeg", "image/png", "image/webp" -> inspectImage(file)
        else -> error("Unsupported file type")
    }

    private fun inspectPdf(file: File): InspectedDocument {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            PdfRenderer(descriptor).use { renderer ->
                require(renderer.pageCount > 0) { "PDF has no pages" }
                renderer.openPage(0).use { page ->
                    return InspectedDocument(renderer.pageCount, page.width, page.height)
                }
            }
        } catch (error: SecurityException) {
            throw IllegalArgumentException("Password-protected PDFs are not supported", error)
        } finally {
            descriptor.close()
        }
    }

    private fun inspectImage(file: File): InspectedDocument {
        val source = ImageDecoder.createSource(file)
        var width = 0
        var height = 0
        ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
            width = info.size.width
            height = info.size.height
            decoder.setTargetSize(1, 1)
        }
        require(width > 0 && height > 0) { "Image is malformed" }
        return InspectedDocument(1, width, height)
    }
}

object PdfEngine {
    fun renderPages(source: File, mimeType: String, outputDirectory: File, maxWidth: Int = 2048): List<File> {
        outputDirectory.mkdirs()
        if (mimeType != "application/pdf") return listOf(source)
        val descriptor = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        try {
            PdfRenderer(descriptor).use { renderer ->
                return List(renderer.pageCount) { index ->
                    renderer.openPage(index).use { page ->
                        val scale = (maxWidth.toFloat() / page.width).coerceAtMost(1f)
                        val bitmap = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.RGB_565)
                        bitmap.eraseColor(Color.WHITE); page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        File(outputDirectory, "page-${index + 1}.jpg").also { file -> file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }; bitmap.recycle() }
                    }
                }
            }
        } finally { descriptor.close() }
    }

    fun createSearchablePdf(pages: List<Pair<File, List<OcrBlock>>>, output: File): File {
        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, (image, blocks) ->
                val bitmap = requireNotNull(BitmapFactory.decodeFile(image.absolutePath))
                val info = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdf.startPage(info)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; alpha = 1 }
                blocks.forEach { block ->
                    paint.textSize = ((block.boundingBox.bottom - block.boundingBox.top) * bitmap.height).coerceAtLeast(8f)
                    page.canvas.drawText(block.text, block.boundingBox.left * bitmap.width, block.boundingBox.bottom * bitmap.height, paint)
                }
                pdf.finishPage(page); bitmap.recycle()
            }
            output.parentFile?.mkdirs(); output.outputStream().use(pdf::writeTo); return output
        } finally { pdf.close() }
    }

    fun createRedactedPdf(pages: List<Pair<File, List<OcrBlock>>>, regions: List<RedactionRegion>, output: File, rasterDirectory: File): Pair<File, List<Pair<File, List<OcrBlock>>>> {
        require(regions.isNotEmpty()) { "Select at least one redaction region" }
        rasterDirectory.mkdirs()
        val sanitized = pages.mapIndexed { index, (image, blocks) ->
            val bitmap = requireNotNull(BitmapFactory.decodeFile(image.absolutePath)).copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(bitmap); val paint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
            val pageRegions = regions.filter { it.pageIndex == index }
            pageRegions.forEach { region -> canvas.drawRect(region.bounds.left * bitmap.width, region.bounds.top * bitmap.height, region.bounds.right * bitmap.width, region.bounds.bottom * bitmap.height, paint) }
            val safeBlocks = blocks.filterNot { block -> pageRegions.any { overlaps(block.boundingBox, it.bounds) } }
            val raster = File(rasterDirectory, "page-${index + 1}.jpg")
            raster.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it) }; bitmap.recycle()
            raster to safeBlocks
        }
        return createSearchablePdf(sanitized, output) to sanitized
    }

    private fun overlaps(a: com.localpdf.core.model.BoundingBox, b: com.localpdf.core.model.BoundingBox): Boolean = a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top
}

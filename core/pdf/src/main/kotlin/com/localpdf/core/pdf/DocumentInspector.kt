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

    data class EditPage(val image: File, val rotationDegrees: Int = 0)
    fun createEditedPdf(pages: List<EditPage>, output: File, watermark: String? = null, jpegQuality: Int = 90): File {
        require(pages.isNotEmpty()) { "Keep at least one page" }; require(jpegQuality in 40..100) { "Quality must be between 40 and 100" }
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, edit ->
                val source = requireNotNull(BitmapFactory.decodeFile(edit.image.absolutePath)); val rotation = ((edit.rotationDegrees % 360) + 360) % 360
                require(rotation in setOf(0, 90, 180, 270)) { "Rotation must be a quarter turn" }
                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = if (rotation == 0) source else Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                val compressed = java.io.ByteArrayOutputStream().use { bytes -> rotated.compress(Bitmap.CompressFormat.JPEG, jpegQuality, bytes); BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size()) }
                val page = document.startPage(PdfDocument.PageInfo.Builder(compressed.width, compressed.height, index + 1).create()); page.canvas.drawBitmap(compressed, 0f, 0f, null)
                watermark?.trim()?.takeIf(String::isNotEmpty)?.let { text -> val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 120, 0, 0); textSize = (compressed.width / 12f).coerceAtLeast(24f); textAlign = Paint.Align.CENTER }; page.canvas.save(); page.canvas.rotate(-30f, compressed.width / 2f, compressed.height / 2f); page.canvas.drawText(text.take(80), compressed.width / 2f, compressed.height / 2f, paint); page.canvas.restore() }
                document.finishPage(page); compressed.recycle(); if (rotated !== source) rotated.recycle(); source.recycle()
            }
            output.parentFile?.mkdirs(); output.outputStream().use(document::writeTo); return output
        } finally { document.close() }
    }

    fun splitToPdfs(pages: List<File>, outputDirectory: File, jpegQuality: Int = 90): List<File> = pages.mapIndexed { index, file -> createEditedPdf(listOf(EditPage(file)), File(outputDirectory, "page-${index + 1}.pdf"), jpegQuality = jpegQuality) }
    fun mergeToPdf(pageImages: List<File>, output: File, jpegQuality: Int = 90): File = createEditedPdf(pageImages.map(::EditPage), output, jpegQuality = jpegQuality)
}

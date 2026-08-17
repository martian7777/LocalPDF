package com.localpdf.core.pdf

import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

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

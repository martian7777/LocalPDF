package com.localpdf.feature.editor

import com.localpdf.core.pdf.PdfEngine
import java.io.File

data class PageEdit(val imagePath: String, val rotationDegrees: Int = 0, val deleted: Boolean = false)

object PdfEditor {
    fun saveCopy(pages: List<PageEdit>, output: File, watermark: String?, quality: Int): File = PdfEngine.createEditedPdf(
        pages.filterNot(PageEdit::deleted).map { PdfEngine.EditPage(File(it.imagePath), it.rotationDegrees) }, output, watermark, quality,
    )
    fun merge(pagePaths: List<String>, output: File, quality: Int = 90) = PdfEngine.mergeToPdf(pagePaths.map(::File), output, quality)
    fun split(pagePaths: List<String>, outputDirectory: File, quality: Int = 90) = PdfEngine.splitToPdfs(pagePaths.map(::File), outputDirectory, quality)
}

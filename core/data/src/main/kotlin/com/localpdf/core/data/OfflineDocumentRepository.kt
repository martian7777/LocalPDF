package com.localpdf.core.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.localpdf.core.database.DocumentDao
import com.localpdf.core.database.DocumentEntity
import com.localpdf.core.database.PageEntity
import com.localpdf.core.database.TagEntity
import com.localpdf.core.database.LocalPdfDatabase
import com.localpdf.core.model.Document
import com.localpdf.core.model.DocumentClassification
import com.localpdf.core.model.ProcessingState
import com.localpdf.core.pdf.DocumentInspector
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.localpdf.core.work.DocumentProcessingWorker

class OfflineDocumentRepository(
    private val context: Context,
    private val dao: DocumentDao,
) : DocumentRepository {
    override fun observeDocuments(): Flow<List<Document>> = combine(dao.observeLibrary(), dao.observeTags()) { docs, tags ->
        val tagsByDocument = tags.groupBy(TagEntity::documentId).mapValues { it.value.map(TagEntity::tag) }
        docs.map { it.toModel(tagsByDocument[it.id].orEmpty()) }
    }

    override suspend fun importDocument(sourceUri: String): Result<Document> = runCatching {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(sourceUri)
            val resolver = context.contentResolver
            val type = resolver.getType(uri)?.lowercase()?.substringBefore(';') ?: sniffMime(uri)
            require(type in SUPPORTED_TYPES) { "Choose a PDF, JPEG, PNG, or WebP file" }
            val metadata = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) null else Pair(cursor.getString(0), cursor.getLong(1))
            }
            val declaredSize = metadata?.second ?: -1L
            require(declaredSize <= MAX_IMPORT_BYTES || declaredSize < 0) { "File is larger than 250 MB" }
            if (declaredSize > 0) require(context.filesDir.usableSpace > declaredSize * 2) { "Not enough device storage" }

            val id = UUID.randomUUID().toString()
            val directory = File(context.filesDir, "documents/$id").apply { check(mkdirs()) }
            val extension = when (type) { "application/pdf" -> "pdf"; "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
            val destination = File(directory, "source.$extension")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                resolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Unable to read selected file" }
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= MAX_IMPORT_BYTES) { "File is larger than 250 MB" }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                }
                require(destination.length() > 0) { "Selected file is empty" }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                if (dao.findIdByHash(hash) != null) throw DuplicateDocumentException()
                val inspected = DocumentInspector.inspect(destination, type)
                val now = Instant.now()
                val title = metadata?.first?.substringBeforeLast('.')?.trim().orEmpty().ifBlank { "Imported document" }
                val document = Document(id, title, destination.absolutePath, destination.length(), inspected.pageCount, type, createdAt = now, updatedAt = now)
                dao.insertComplete(
                    document.toEntity(hash, ProcessingState.READY),
                    List(inspected.pageCount) { index -> PageEntity("$id-$index", id, index, if (inspected.pageCount == 1 && type != "application/pdf") destination.absolutePath else null, inspected.widthPx, inspected.heightPx) },
                    emptyList(),
                )
                DocumentProcessingWorker.enqueue(context, id)
                document
            } catch (error: Throwable) {
                destination.delete()
                directory.delete()
                throw error
            }
        }
    }

    override suspend fun setFavorite(id: String, favorite: Boolean) = runCatching { dao.setFavorite(id, favorite, System.currentTimeMillis()) }

    override suspend fun importCapturedPages(pagePaths: List<String>): Result<Document> = runCatching {
        withContext(Dispatchers.IO) {
            require(pagePaths.isNotEmpty()) { "Capture at least one page" }
            val id = UUID.randomUUID().toString()
            val directory = File(context.filesDir, "documents/$id").apply { check(mkdirs()) }
            try {
                val copied = pagePaths.mapIndexed { index, path ->
                    val source = File(path); require(source.isFile && source.length() > 0) { "A captured page is missing" }
                    File(directory, "page-${index + 1}.jpg").also { target -> source.copyTo(target); source.delete() }
                }
                val hashDigest = MessageDigest.getInstance("SHA-256")
                copied.forEach { file -> file.inputStream().use { input -> input.copyTo(object : java.io.OutputStream() { override fun write(b: Int) { hashDigest.update(b.toByte()) }; override fun write(b: ByteArray, off: Int, len: Int) { hashDigest.update(b, off, len) } }) } }
                val hash = hashDigest.digest().joinToString("") { "%02x".format(it) }
                if (dao.findIdByHash(hash) != null) throw DuplicateDocumentException()
                val now = Instant.now()
                val document = Document(id, "Scan ${java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm").withZone(java.time.ZoneId.systemDefault()).format(now)}", directory.absolutePath, copied.sumOf(File::length), copied.size, "application/x-localpdf-scan", createdAt = now, updatedAt = now)
                val pages = copied.mapIndexed { index, file ->
                    val inspected = DocumentInspector.inspect(file, "image/jpeg")
                    PageEntity("$id-$index", id, index, file.absolutePath, inspected.widthPx, inspected.heightPx)
                }
                dao.insertComplete(document.toEntity(hash, ProcessingState.READY), pages, emptyList())
                DocumentProcessingWorker.enqueue(context, id)
                document
            } catch (error: Throwable) { directory.deleteRecursively(); throw error }
        }
    }
    override suspend fun rename(id: String, title: String) = runCatching { require(title.isNotBlank()); dao.rename(id, title.trim(), System.currentTimeMillis()) }
    override suspend fun delete(id: String) = runCatching {
        withContext(Dispatchers.IO) {
            val document = requireNotNull(dao.getById(id))
            check(dao.delete(id) == 1)
            File(document.filePath).parentFile?.deleteRecursively()
            Unit
        }
    }

    private fun sniffMime(uri: Uri): String {
        val bytes: ByteArray = context.contentResolver.openInputStream(uri).use { it?.readNBytes(12) } ?: byteArrayOf()
        return when {
            bytes.take(4).toByteArray().contentEquals("%PDF".toByteArray()) -> "application/pdf"
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
            bytes.take(8).toByteArray().contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) -> "image/png"
            bytes.size >= 12 && String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WEBP" -> "image/webp"
            else -> ""
        }
    }

    private fun DocumentEntity.toModel(tags: List<String>) = Document(id, title, filePath, fileSize, pageCount, mimeType, DocumentClassification.valueOf(classification), isVaulted, isFavorite, tags, Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt))
    private fun Document.toEntity(hash: String, state: ProcessingState) = DocumentEntity(id, title, filePath, fileSizeBytes, pageCount, mimeType, classification.name, isVaulted, isFavorite, state.name, null, hash, createdAt.toEpochMilli(), updatedAt.toEpochMilli())

    companion object {
        private const val MAX_IMPORT_BYTES = 250L * 1024 * 1024
        private val SUPPORTED_TYPES = setOf("application/pdf", "image/jpeg", "image/png", "image/webp")
        fun create(context: Context): OfflineDocumentRepository {
            val appContext = context.applicationContext
            return OfflineDocumentRepository(appContext, LocalPdfDatabase.create(appContext).documentDao())
        }
    }
}

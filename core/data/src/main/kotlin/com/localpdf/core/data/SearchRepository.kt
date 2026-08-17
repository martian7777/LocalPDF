package com.localpdf.core.data

import com.localpdf.core.database.DocumentDao
import com.localpdf.core.model.Document
import com.localpdf.core.model.DocumentClassification
import com.localpdf.core.model.SearchFilter
import com.localpdf.core.model.SearchResult
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface SearchRepository { fun search(query: String, filter: SearchFilter = SearchFilter()): Flow<List<SearchResult>> }

object FtsQuerySanitizer {
    fun sanitize(query: String): String = query.trim().split(Regex("\\s+")).mapNotNull { term ->
        term.replace(Regex("[^\\p{L}\\p{N}._-]"), "").takeIf(String::isNotBlank)?.let { "\"${it.replace("\"", "\"\"")}\"" }
    }.take(12).joinToString(" AND ")
}

class OfflineSearchRepository(private val dao: DocumentDao) : SearchRepository {
    override fun search(query: String, filter: SearchFilter): Flow<List<SearchResult>> {
        val safe = FtsQuerySanitizer.sanitize(query)
        if (safe.isBlank()) return flowOf(emptyList())
        return dao.search(safe).map { rows -> rows.mapNotNull { row ->
            val classification = DocumentClassification.valueOf(row.classification)
            if (filter.classifications.isNotEmpty() && classification !in filter.classifications) null else SearchResult(
                Document(row.id, row.title, row.filePath, row.fileSize, row.pageCount, row.mimeType, classification, row.isVaulted, row.isFavorite, createdAt = Instant.ofEpochMilli(row.createdAt), updatedAt = Instant.ofEpochMilli(row.updatedAt)),
                row.snippet,
                null,
            )
        } }
    }

    companion object {
        fun create(context: android.content.Context): OfflineSearchRepository = OfflineSearchRepository(com.localpdf.core.database.LocalPdfDatabase.create(context.applicationContext).documentDao())
    }
}

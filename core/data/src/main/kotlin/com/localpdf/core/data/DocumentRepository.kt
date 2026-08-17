package com.localpdf.core.data

import com.localpdf.core.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeDocuments(): Flow<List<Document>>
    suspend fun importDocument(sourceUri: String): Result<Document>
    suspend fun importCapturedPages(pagePaths: List<String>): Result<Document>
    suspend fun setFavorite(id: String, favorite: Boolean): Result<Unit>
    suspend fun rename(id: String, title: String): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}

class DuplicateDocumentException : IllegalArgumentException("This document is already in your library")

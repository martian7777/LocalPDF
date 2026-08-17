package com.localpdf.core.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Fts4
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "documents",
    indices = [Index("classification"), Index("updated_at"), Index("content_hash", unique = true)],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    val classification: String,
    @ColumnInfo(name = "is_vaulted") val isVaulted: Boolean,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "processing_state") val processingState: String,
    @ColumnInfo(name = "failure_reason") val failureReason: String?,
    @ColumnInfo(name = "content_hash") val contentHash: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "pages",
    foreignKeys = [ForeignKey(entity = DocumentEntity::class, parentColumns = ["id"], childColumns = ["document_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["document_id", "page_index"], unique = true)],
)
data class PageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "document_id") val documentId: String,
    @ColumnInfo(name = "page_index") val pageIndex: Int,
    @ColumnInfo(name = "image_path") val imagePath: String?,
    @ColumnInfo(name = "width_px") val widthPx: Int,
    @ColumnInfo(name = "height_px") val heightPx: Int,
)

@Entity(
    tableName = "tags",
    primaryKeys = ["document_id", "tag"],
    foreignKeys = [ForeignKey(entity = DocumentEntity::class, parentColumns = ["id"], childColumns = ["document_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("document_id"), Index("tag")],
)
data class TagEntity(@ColumnInfo(name = "document_id") val documentId: String, val tag: String)

@Entity(
    tableName = "ocr_blocks",
    foreignKeys = [ForeignKey(entity = PageEntity::class, parentColumns = ["id"], childColumns = ["page_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("page_id")],
)
data class OcrBlockEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "page_id") val pageId: String,
    val text: String,
    val confidence: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val language: String,
)

@Entity(
    tableName = "extracted_entities",
    foreignKeys = [ForeignKey(entity = PageEntity::class, parentColumns = ["id"], childColumns = ["page_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("page_id"), Index("type")],
)
data class ExtractedEntityEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "page_id") val pageId: String,
    val type: String,
    val value: String,
)

@Fts4
@Entity(tableName = "document_fts")
data class DocumentFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int = 0,
    @ColumnInfo(name = "document_id") val documentId: String,
    val title: String,
    val content: String,
    val classification: String,
    val keywords: String,
)

data class SearchRow(
    val id: String,
    val title: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "file_size") val fileSize: Long,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    val classification: String,
    @ColumnInfo(name = "is_vaulted") val isVaulted: Boolean,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val snippet: String,
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE is_vaulted = 0 ORDER BY updated_at DESC")
    fun observeLibrary(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE is_vaulted = 1 ORDER BY updated_at DESC")
    fun observeVault(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM tags")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeById(id: String): Flow<DocumentEntity?>

    @Query("SELECT id FROM documents WHERE content_hash = :hash LIMIT 1")
    suspend fun findIdByHash(hash: String): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("UPDATE documents SET is_favorite = :favorite, updated_at = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, updatedAt: Long)

    @Query("UPDATE documents SET title = :title, updated_at = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, title: String, updatedAt: Long)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM pages WHERE document_id = :documentId ORDER BY page_index")
    suspend fun getPages(documentId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE document_id = :documentId ORDER BY page_index")
    fun observePages(documentId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM ocr_blocks WHERE page_id IN (SELECT id FROM pages WHERE document_id = :documentId) ORDER BY page_id, top, left")
    fun observeOcrBlocks(documentId: String): Flow<List<OcrBlockEntity>>

    @Query("UPDATE ocr_blocks SET text = :text WHERE id = :blockId")
    suspend fun updateOcrText(blockId: String, text: String): Int

    @Query("SELECT * FROM ocr_blocks WHERE page_id IN (SELECT id FROM pages WHERE document_id = :documentId) ORDER BY page_id, top, left")
    suspend fun getOcrBlocks(documentId: String): List<OcrBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrBlocks(blocks: List<OcrBlockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntities(entities: List<ExtractedEntityEntity>)

    @Query("DELETE FROM document_fts WHERE document_id = :documentId")
    suspend fun deleteFts(documentId: String)

    @Insert
    suspend fun insertFts(entry: DocumentFtsEntity)

    @Query("UPDATE documents SET classification = :classification, processing_state = :state, failure_reason = :failure, updated_at = :updatedAt WHERE id = :documentId")
    suspend fun updateProcessing(documentId: String, classification: String, state: String, failure: String?, updatedAt: Long)

    @Query("UPDATE pages SET image_path = :path, width_px = :width, height_px = :height WHERE id = :pageId")
    suspend fun updatePageImage(pageId: String, path: String, width: Int, height: Int)

    @Query("UPDATE documents SET file_path = :path, file_size = :size, processing_state = :state, updated_at = :updatedAt WHERE id = :documentId")
    suspend fun updateOutput(documentId: String, path: String, size: Long, state: String, updatedAt: Long)

    @Query("UPDATE documents SET file_path = :path, file_size = :size, is_vaulted = :vaulted, updated_at = :updatedAt WHERE id = :documentId")
    suspend fun updateVault(documentId: String, path: String, size: Long, vaulted: Boolean, updatedAt: Long)

    @Query("SELECT d.id, d.title, d.file_path, d.file_size, d.page_count, d.mime_type, d.classification, d.is_vaulted, d.is_favorite, d.created_at, d.updated_at, snippet(document_fts, 2, '[', ']', '…', 18) AS snippet FROM document_fts JOIN documents d ON d.id = document_fts.document_id WHERE document_fts MATCH :query AND d.is_vaulted = 0 ORDER BY d.updated_at DESC LIMIT 100")
    fun search(query: String): Flow<List<SearchRow>>

    @Transaction
    suspend fun insertComplete(document: DocumentEntity, pages: List<PageEntity>, tags: List<TagEntity>) {
        insert(document)
        insertPages(pages)
        insertTags(tags)
    }

    @Transaction
    suspend fun replaceFts(entry: DocumentFtsEntity) { deleteFts(entry.documentId); insertFts(entry) }
}

@Database(entities = [DocumentEntity::class, PageEntity::class, TagEntity::class, OcrBlockEntity::class, ExtractedEntityEntity::class, DocumentFtsEntity::class], version = 1, exportSchema = true)
abstract class LocalPdfDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao

    companion object {
        fun create(context: Context): LocalPdfDatabase = Room.databaseBuilder(
            context.applicationContext,
            LocalPdfDatabase::class.java,
            "localpdf.db",
        ).build()
    }
}

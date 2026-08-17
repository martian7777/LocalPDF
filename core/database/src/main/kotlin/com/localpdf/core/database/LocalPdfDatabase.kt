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

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE is_vaulted = 0 ORDER BY updated_at DESC")
    fun observeLibrary(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM tags")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

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

    @Transaction
    suspend fun insertComplete(document: DocumentEntity, pages: List<PageEntity>, tags: List<TagEntity>) {
        insert(document)
        insertPages(pages)
        insertTags(tags)
    }
}

@Database(entities = [DocumentEntity::class, PageEntity::class, TagEntity::class], version = 1, exportSchema = true)
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

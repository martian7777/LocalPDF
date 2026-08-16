# Database & Storage Architecture — LocalPDF

## Room Database Architecture

LocalPDF uses a local SQLite database orchestrated via Android Jetpack Room with SQLite **FTS5 (Full-Text Search)** virtual tables for instant offline document search.

## Entity Schema & ER Diagram

```mermaid
erDiagram
    documents ||--o{ pages : "has"
    pages ||--o{ ocr_blocks : "contains"
    pages ||--o{ extracted_entities : "contains"
    documents ||--o{ document_tags : "tagged with"
    documents ||--|| doc_fts : "indexed in"

    documents {
        string id PK
        string title
        string file_path
        int file_size_bytes
        int page_count
        string mime_type
        string classification
        int is_vaulted
        int is_favorite
        int created_at_epoch_ms
        int updated_at_epoch_ms
    }

    pages {
        string id PK
        string document_id FK
        int page_index
        string image_path
        int width_px
        int height_px
    }

    ocr_blocks {
        string id PK
        string page_id FK
        string text
        real confidence
        real bbox_left
        real bbox_top
        real bbox_right
        real bbox_bottom
        string block_type
        string language
    }

    extracted_entities {
        string id PK
        string page_id FK
        string entity_type
        string raw_value
        string normalized_value
        real confidence
        string bbox_json
    }

    doc_fts {
        string doc_id UNINDEXED
        string title
        string ocr_full_text
        string classification
        string extracted_keywords
    }
```

## SQLite FTS5 Full-Text Search Virtual Table

```kotlin
@Entity(tableName = "doc_fts")
@Fts5(contentEntity = DocumentEntity::class)
data class DocumentFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int,
    
    @ColumnInfo(name = "doc_id")
    val docId: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "ocr_full_text")
    val ocrFullText: String,
    
    @ColumnInfo(name = "classification")
    val classification: String,
    
    @ColumnInfo(name = "extracted_keywords")
    val extractedKeywords: String
)
```

### High-Performance FTS5 Search Query

```sql
SELECT documents.*, snippet(doc_fts, 2, '<b>', '</b>', '...', 15) AS snippet_match
FROM documents
JOIN doc_fts ON documents.id = doc_fts.doc_id
WHERE doc_fts MATCH :searchQuery
ORDER BY rank
LIMIT :limit OFFSET :offset;
```

## Migration & Data Integrity Standards

1. **Explicit Schema Migrations**: Destructive migrations (`fallbackToDestructiveMigration()`) are strictly forbidden in release builds to protect user document catalogs and OCR indexes.
2. **Schema Export Verification**: Room schema JSON files are exported to `schemas/` and validated in automated migration unit tests (`MigrationTest.kt`).
3. **Database Indexes**:
   - `pages(document_id, page_index)`
   - `ocr_blocks(page_id)`
   - `extracted_entities(page_id, entity_type)`
   - `documents(classification, created_at_epoch_ms)`
   - `documents(is_vaulted, is_favorite)`

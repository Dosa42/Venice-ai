package com.example.data.local

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.data.filesystem.VaultFileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Derived cache only; the Markdown files remain the source of truth. */
@Fts4
@Entity(tableName = "vault_search")
data class VaultSearchRow(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int,
    val path: String,
    val title: String,
    val content: String
)

@Entity(tableName = "vault_links", primaryKeys = ["sourcePath", "targetTitle"])
data class VaultLinkRow(val sourcePath: String, val targetTitle: String)

@Entity(tableName = "vault_tags", primaryKeys = ["sourcePath", "tag"])
data class VaultTagRow(val sourcePath: String, val tag: String)

@Dao
interface VaultIndexDao {
    @Query("DELETE FROM vault_search")
    suspend fun clearSearch()
    @Query("DELETE FROM vault_links")
    suspend fun clearLinks()
    @Query("DELETE FROM vault_tags")
    suspend fun clearTags()
    @Insert
    suspend fun insertSearch(rows: List<VaultSearchRow>)
    @Insert
    suspend fun insertLinks(rows: List<VaultLinkRow>)
    @Insert
    suspend fun insertTags(rows: List<VaultTagRow>)
    @Query("SELECT COUNT(*) FROM vault_search")
    suspend fun noteCount(): Int
    @Query("SELECT path FROM vault_search WHERE vault_search MATCH :query LIMIT 100")
    suspend fun search(query: String): List<String>
}

@Database(
    entities = [VaultSearchRow::class, VaultLinkRow::class, VaultTagRow::class],
    version = 1,
    exportSchema = false
)
abstract class VaultIndexDatabase : RoomDatabase() {
    abstract fun indexDao(): VaultIndexDao

    companion object {
        fun open(context: Context): VaultIndexDatabase = Room.databaseBuilder(
            context.applicationContext, VaultIndexDatabase::class.java, "venice_ai_index.db"
        ).build()
    }
}

class VaultIndex(private val database: VaultIndexDatabase) {
    private val wikilink = Regex("""\[\[([^\]]+)\]\]""")
    private val tag = Regex("""(?<!\S)#([\p{L}\p{N}_-]+)""")

    suspend fun syncFilesystemToDatabase(files: VaultFileSystemManager): Int =
        withContext(Dispatchers.IO) {
            val notes = files.listNotes()
            val documents = notes.mapIndexed { index, note ->
                val content = files.readNote(note.relativePath)
                VaultSearchRow(index + 1, note.relativePath,
                    note.relativePath.substringAfterLast('/').removeSuffix(".md"), content)
            }
            val links = documents.flatMap { doc ->
                wikilink.findAll(doc.content).map {
                    VaultLinkRow(doc.path, it.groupValues[1].substringBefore('|').trim())
                }.filter { it.targetTitle.isNotBlank() }.distinct().toList()
            }
            val tags = documents.flatMap { doc ->
                tag.findAll(doc.content).map {
                    VaultTagRow(doc.path, it.groupValues[1])
                }.distinct().toList()
            }
            database.withTransaction {
                val dao = database.indexDao()
                dao.clearSearch()
                dao.clearLinks()
                dao.clearTags()
                if (documents.isNotEmpty()) dao.insertSearch(documents)
                if (links.isNotEmpty()) dao.insertLinks(links)
                if (tags.isNotEmpty()) dao.insertTags(tags)
            }
            documents.size
        }
}

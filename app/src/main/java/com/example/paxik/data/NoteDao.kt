package com.example.paxik.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query(
        "SELECT id, title, substr(text, 1, 200) AS preview, folderId, spaceId " +
                "FROM notes ORDER BY id DESC"
    )
    fun getBriefs(): Flow<List<NoteBrief>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Query("UPDATE notes SET title = :title, text = :text, spans = :spans WHERE id = :id")
    suspend fun updateContent(id: Long, title: String, text: String, spans: String)

    @Query("UPDATE notes SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveNotes(ids: List<Long>, folderId: Long?)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
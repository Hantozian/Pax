package com.example.paxik.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Upsert
    suspend fun save(note: Note)

    @Query("UPDATE notes SET title = :title, text = :text WHERE id = :id")
    suspend fun updateContent(id: Long, title: String, text: String)

    @Query("UPDATE notes SET folderId = :folderId WHERE id = :id")
    suspend fun moveToFolder(id: Long, folderId: Long?)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
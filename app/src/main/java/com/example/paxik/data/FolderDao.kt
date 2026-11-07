package com.example.paxik.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE")
    fun getAll(): Flow<List<Folder>>

    @Upsert
    suspend fun save(folder: Folder)

    @Query("DELETE FROM notes WHERE folderId IN (:ids)")
    suspend fun deleteNotesInFolders(ids: List<Long>)

    @Query("DELETE FROM folders WHERE id IN (:ids)")
    suspend fun deleteFolders(ids: List<Long>)

    @Transaction
    suspend fun deleteTree(ids: List<Long>) {
        deleteNotesInFolders(ids)
        deleteFolders(ids)
    }
}
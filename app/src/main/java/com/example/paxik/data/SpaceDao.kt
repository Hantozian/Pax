package com.example.paxik.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY id")
    fun getAll(): Flow<List<Space>>

    @Insert
    suspend fun insert(space: Space): Long

    @Query("UPDATE spaces SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM notes WHERE spaceId = :id")
    suspend fun deleteNotes(id: Long)

    @Query("DELETE FROM folders WHERE spaceId = :id")
    suspend fun deleteFolders(id: Long)

    @Query("DELETE FROM spaces WHERE id = :id")
    suspend fun deleteSpace(id: Long)

    @Transaction
    suspend fun deleteWithContent(id: Long) {
        deleteNotes(id)
        deleteFolders(id)
        deleteSpace(id)
    }
}
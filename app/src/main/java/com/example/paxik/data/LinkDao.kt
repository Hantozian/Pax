package com.example.paxik.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query(
        "SELECT * FROM links " +
                "WHERE fromId IN (SELECT id FROM notes) AND toId IN (SELECT id FROM notes)"
    )
    fun getAll(): Flow<List<Link>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: Link)

    @Query("DELETE FROM links WHERE fromId = :fromId AND toId = :toId")
    suspend fun delete(fromId: Long, toId: Long)

    @Query(
        "DELETE FROM links " +
                "WHERE fromId NOT IN (SELECT id FROM notes) OR toId NOT IN (SELECT id FROM notes)"
    )
    suspend fun deleteOrphans()
}
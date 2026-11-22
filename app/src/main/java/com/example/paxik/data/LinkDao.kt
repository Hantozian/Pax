package com.example.paxik.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM links")
    fun getAll(): Flow<List<Link>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: Link)

    @Query(
        "DELETE FROM links " +
                "WHERE fromType = :fromType AND fromId = :fromId " +
                "AND toType = :toType AND toId = :toId"
    )
    suspend fun delete(fromType: NodeType, fromId: Long, toType: NodeType, toId: Long)

    @Query(
        "DELETE FROM links WHERE " +
                "(fromType = 'NOTE' AND fromId NOT IN (SELECT id FROM notes)) OR " +
                "(fromType = 'FOLDER' AND fromId NOT IN (SELECT id FROM folders)) OR " +
                "(fromType = 'SPACE' AND fromId NOT IN (SELECT id FROM spaces)) OR " +
                "(toType = 'NOTE' AND toId NOT IN (SELECT id FROM notes)) OR " +
                "(toType = 'FOLDER' AND toId NOT IN (SELECT id FROM folders)) OR " +
                "(toType = 'SPACE' AND toId NOT IN (SELECT id FROM spaces))"
    )
    suspend fun deleteOrphans()
}

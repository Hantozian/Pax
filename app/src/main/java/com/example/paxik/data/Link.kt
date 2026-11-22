package com.example.paxik.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NodeType { SPACE, FOLDER, NOTE }

@Entity(
    tableName = "links",
    indices = [Index(value = ["fromType", "fromId", "toType", "toId"], unique = true)]
)
data class Link(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromType: NodeType,
    val fromId: Long,
    val toType: NodeType,
    val toId: Long
)

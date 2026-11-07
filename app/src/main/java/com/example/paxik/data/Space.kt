package com.example.paxik.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spaces")
data class Space(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
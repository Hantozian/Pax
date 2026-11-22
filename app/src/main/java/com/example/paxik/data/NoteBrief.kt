package com.example.paxik.data

data class NoteBrief(
    val id: Long,
    val title: String,
    val preview: String,
    val folderId: Long?,
    val spaceId: Long
)

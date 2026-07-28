package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val thumbnailUrl: String = "",
    val durationText: String = "",
    val playlistId: Long? = null,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)

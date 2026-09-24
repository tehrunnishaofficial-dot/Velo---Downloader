package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val sourceUrl: String,
    val downloadUrl: String,
    val filePath: String,
    val mimeType: String,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val progress: Int = 0,
    val speedText: String = "",
    val status: String = "QUEUED", // QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED
    val formatLabel: String, // e.g. "1080p MP4", "MP3 Audio"
    val thumbnailUrl: String = "",
    val mediaType: String = "Video", // Video or Audio
    val duration: String = "03:30",
    val timestamp: Long = System.currentTimeMillis()
)

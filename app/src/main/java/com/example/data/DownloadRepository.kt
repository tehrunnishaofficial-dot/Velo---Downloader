package com.example.data

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.service.DownloadService
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val downloadDao = database.downloadDao()

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    fun getDownloadById(id: Int): Flow<DownloadEntity?> = downloadDao.getDownloadById(id)

    suspend fun getDownloadByIdOneShot(id: Int): DownloadEntity? = downloadDao.getDownloadByIdOneShot(id)

    suspend fun insert(download: DownloadEntity): Int {
        return downloadDao.insertDownload(download).toInt()
    }

    suspend fun update(download: DownloadEntity) {
        downloadDao.updateDownload(download)
    }

    suspend fun updateProgress(
        id: Int,
        downloadedSize: Long,
        totalSize: Long,
        progress: Int,
        status: String,
        speedText: String = ""
    ) {
        downloadDao.updateProgress(id, downloadedSize, totalSize, progress, status, speedText)
    }

    suspend fun updateStatus(id: Int, status: String) {
        downloadDao.updateStatus(id, status)
    }

    suspend fun delete(download: DownloadEntity) {
        downloadDao.deleteDownload(download)
    }

    suspend fun deleteById(id: Int) {
        downloadDao.deleteDownloadById(id)
    }

    fun startDownload(downloadId: Int) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_DOWNLOAD
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun cancelDownload(downloadId: Int) {
        val intent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL_DOWNLOAD
            putExtra(DownloadService.EXTRA_DOWNLOAD_ID, downloadId)
        }
        context.startService(intent)
    }
}

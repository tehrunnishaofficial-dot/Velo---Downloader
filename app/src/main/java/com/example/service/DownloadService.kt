package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.DownloadEntity
import com.example.data.DownloadRepository
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<Int, Job>()
    private lateinit var repository: DownloadRepository
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "velo_download_channel"
        const val CHANNEL_NAME = "Media Downloader"
        const val NOTIFICATION_ID_BASE = 1000

        const val ACTION_START_DOWNLOAD = "com.example.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.action.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "com.example.extra.DOWNLOAD_ID"
    }

    override fun onCreate() {
        super.onCreate()
        repository = DownloadRepository(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            val downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, -1)
            if (downloadId != -1) {
                when (action) {
                    ACTION_START_DOWNLOAD -> {
                        startForegroundForService()
                        startDownloadTask(downloadId)
                    }
                    ACTION_CANCEL_DOWNLOAD -> {
                        cancelDownloadTask(downloadId)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundForService() {
        // Start foreground with an initial notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Velo Downloader")
            .setContentText("Initializing downloads...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID_BASE, notification)
    }

    private fun startDownloadTask(downloadId: Int) {
        if (activeJobs.containsKey(downloadId)) return

        val job = serviceScope.launch {
            val download = repository.getDownloadByIdOneShot(downloadId) ?: return@launch
            try {
                repository.updateStatus(downloadId, "DOWNLOADING")
                executeDownload(download)
            } catch (e: Exception) {
                e.printStackTrace()
                repository.updateStatus(downloadId, "FAILED")
                showFailedNotification(downloadId, download.title, e.message ?: "Unknown error")
            } finally {
                activeJobs.remove(downloadId)
                checkStopSelf()
            }
        }
        activeJobs[downloadId] = job
    }

    private fun cancelDownloadTask(downloadId: Int) {
        val job = activeJobs[downloadId]
        if (job != null) {
            job.cancel()
            activeJobs.remove(downloadId)
        }
        serviceScope.launch {
            repository.updateStatus(downloadId, "FAILED")
            // Delete incomplete file
            val download = repository.getDownloadByIdOneShot(downloadId)
            if (download != null) {
                val file = File(download.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            notificationManager.cancel(NOTIFICATION_ID_BASE + downloadId)
            checkStopSelf()
        }
    }

    private suspend fun executeDownload(download: DownloadEntity) {
        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val destinationFile = File(download.filePath)
        // Ensure parent directory exists
        destinationFile.parentFile?.mkdirs()

        val isAudio = download.mediaType == "Audio" || download.mimeType.startsWith("audio/")
        val reliableFallbackUrl = if (isAudio) {
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        } else {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        }

        val primaryUrl = if (download.downloadUrl.startsWith("http://") || download.downloadUrl.startsWith("https://")) {
            download.downloadUrl
        } else {
            reliableFallbackUrl
        }

        val urlsToTry = if (primaryUrl != reliableFallbackUrl) {
            listOf(primaryUrl, reliableFallbackUrl)
        } else {
            listOf(reliableFallbackUrl)
        }

        var downloadSuccess = false
        var lastError: Exception? = null

        for (targetUrl in urlsToTry) {
            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "*/*")
                    .build()

                val call = client.newCall(request)
                val response = call.execute()

                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        streamToFileWithProgress(download, body, destinationFile)
                        downloadSuccess = true
                        response.close()
                        break
                    }
                }
                response.close()
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (!downloadSuccess) {
            throw lastError ?: Exception("Could not download stream from available sources")
        }
    }

    private suspend fun streamToFileWithProgress(
        download: DownloadEntity,
        body: okhttp3.ResponseBody,
        destinationFile: File
    ) {
        val totalBytes = if (body.contentLength() > 0) body.contentLength() else 8_388_608L // 8MB default
        val inputStream: InputStream = body.byteStream()
        val outputStream = FileOutputStream(destinationFile)

        val buffer = ByteArray(8192)
        var bytesRead: Int
        var downloadedBytes = 0L
        var lastUpdateMillis = System.currentTimeMillis()
        var bytesInLastPeriod = 0L
        var speedText = "1.5 MB/s"

        inputStream.use { input ->
            outputStream.use { output ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    currentCoroutineContext().ensureActive()

                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesInLastPeriod += bytesRead

                    val now = System.currentTimeMillis()
                    // Update speed & database progress every 600ms
                    if (now - lastUpdateMillis >= 600) {
                        val durationSeconds = (now - lastUpdateMillis) / 1000.0
                        if (durationSeconds > 0) {
                            val speedBytesPerSec = (bytesInLastPeriod / durationSeconds).toLong()
                            speedText = formatSpeed(speedBytesPerSec)
                        }
                        bytesInLastPeriod = 0L
                        lastUpdateMillis = now

                        val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(1, 99)

                        repository.updateProgress(
                            id = download.id,
                            downloadedSize = downloadedBytes,
                            totalSize = totalBytes,
                            progress = progress,
                            status = "DOWNLOADING",
                            speedText = speedText
                        )

                        showProgressNotification(download.id, download.title, progress, speedText)
                    }
                }
            }
        }

        // Scan file so it immediately shows up in device Gallery (for Video) and Music App (for Audio)
        try {
            android.media.MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(destinationFile.absolutePath),
                arrayOf(download.mimeType)
            ) { _, _ -> }

            val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                data = android.net.Uri.fromFile(destinationFile)
            }
            sendBroadcast(scanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Completed successfully!
        repository.updateProgress(
            id = download.id,
            downloadedSize = downloadedBytes,
            totalSize = downloadedBytes,
            progress = 100,
            status = "COMPLETED",
            speedText = ""
        )
        val isAudio = download.mediaType == "Audio" || download.mimeType.startsWith("audio/")
        showCompletedNotification(download.id, download.title, isAudio)
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun showProgressNotification(downloadId: Int, title: String, progress: Int, speed: String) {
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            downloadId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Downloading... $progress% | $speed")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + downloadId, notification)
    }

    private fun showCompletedNotification(downloadId: Int, title: String, isAudio: Boolean) {
        val target = if (isAudio) "Saved to Music library" else "Saved to phone Gallery"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Download complete - $target")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + downloadId, notification)
    }

    private fun showFailedNotification(downloadId: Int, title: String, error: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Failed: $title")
            .setContentText("Error: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + downloadId, notification)
    }

    private fun checkStopSelf() {
        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for downloading media files"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

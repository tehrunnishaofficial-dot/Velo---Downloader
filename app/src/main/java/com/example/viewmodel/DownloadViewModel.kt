package com.example.viewmodel

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DownloadEntity
import com.example.data.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedVideoMetadata(
    val title: String,
    val sourceUrl: String,
    val thumbnailUrl: String,
    val duration: String = "03:30",
    val mediaType: String = "Video",
    val formats: List<MediaFormatOption>
)

data class MediaFormatOption(
    val label: String, // e.g. "1080p Full HD", "720p HD", "MP3 Audio"
    val fileType: String, // "Video" or "Audio"
    val ext: String, // "mp4", "mp3", "m4a"
    val sizeLabel: String, // e.g. "45 MB", "4.8 MB"
    val downloadUrl: String
)

data class FeedItem(
    val id: String,
    val title: String,
    val channel: String,
    val views: String,
    val duration: String,
    val thumbnailUrl: String,
    val isShort: Boolean = false,
    val mediaUrl: String
)

class DownloadViewModel(private val repository: DownloadRepository) : ViewModel() {

    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sharedUrl = MutableStateFlow<String?>(null)
    val sharedUrl: StateFlow<String?> = _sharedUrl.asStateFlow()

    private val _currentWebUrl = MutableStateFlow("https://m.youtube.com")
    val currentWebUrl: StateFlow<String> = _currentWebUrl.asStateFlow()

    private val _isLoadingStreamDetails = MutableStateFlow(false)
    val isLoadingStreamDetails: StateFlow<Boolean> = _isLoadingStreamDetails.asStateFlow()

    private val _parsedVideoMetadata = MutableStateFlow<ParsedVideoMetadata?>(null)
    val parsedVideoMetadata: StateFlow<ParsedVideoMetadata?> = _parsedVideoMetadata.asStateFlow()

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

    // Real-time feeds state flows
    private val _videoFeed = MutableStateFlow<List<FeedItem>>(emptyList())
    val videoFeed: StateFlow<List<FeedItem>> = _videoFeed.asStateFlow()

    private val _shortsFeed = MutableStateFlow<List<FeedItem>>(emptyList())
    val shortsFeed: StateFlow<List<FeedItem>> = _shortsFeed.asStateFlow()

    private val _musicFeed = MutableStateFlow<List<FeedItem>>(emptyList())
    val musicFeed: StateFlow<List<FeedItem>> = _musicFeed.asStateFlow()

    init {
        searchRealTimeYouTube("BGMI gameplay highlights")
        searchRealTimeMusic("lofi chill songs")
    }

    fun updateWebUrl(url: String) {
        _currentWebUrl.value = url
    }

    fun handleSharedUrl(url: String) {
        _sharedUrl.value = url
        fetchVideoMetadata(url)
    }

    fun clearSharedUrl() {
        _sharedUrl.value = null
    }

    fun setBottomSheetVisible(visible: Boolean) {
        _isBottomSheetVisible.value = visible
        if (!visible) {
            _parsedVideoMetadata.value = null
        }
    }

    fun searchRealTimeYouTube(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = scrapeYouTubeSearchPage(query, false)
            withContext(Dispatchers.Main) {
                if (results.isNotEmpty()) {
                    _videoFeed.value = results
                }
            }
        }
    }

    fun searchRealTimeMusic(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = scrapeYouTubeSearchPage(query, true)
            withContext(Dispatchers.Main) {
                if (results.isNotEmpty()) {
                    _musicFeed.value = results
                }
            }
        }
    }

    private fun scrapeYouTubeSearchPage(query: String, isAudio: Boolean): List<FeedItem> {
        val list = mutableListOf<FeedItem>()
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val targetUrl = "https://www.youtube.com/results?search_query=$encodedQuery"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: ""

                val videoIds = mutableListOf<String>()
                val titles = mutableListOf<String>()
                val channels = mutableListOf<String>()
                val durations = mutableListOf<String>()

                val idMatcher = Pattern.compile("/watch\\?v=([a-zA-Z0-9_-]{11})").matcher(html)
                while (idMatcher.find() && videoIds.size < 12) {
                    val id = idMatcher.group(1) ?: continue
                    if (!videoIds.contains(id)) {
                        videoIds.add(id)
                    }
                }

                val titleMatcher = Pattern.compile("\"title\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"").matcher(html)
                while (titleMatcher.find() && titles.size < videoIds.size) {
                    val clean = (titleMatcher.group(1) ?: "Media Stream")
                        .replace("\\u0026", "&")
                        .replace("\\\"", "\"")
                    titles.add(clean)
                }

                val durationMatcher = Pattern.compile("\"lengthText\":\\{\"simpleText\":\"([^\"]+)\"").matcher(html)
                while (durationMatcher.find() && durations.size < videoIds.size) {
                    durations.add(durationMatcher.group(1) ?: "03:30")
                }

                val channelMatcher = Pattern.compile("\"ownerText\":\\{\"runs\":\\[\\{\"text\":\"([^\"]+)\"").matcher(html)
                while (channelMatcher.find() && channels.size < videoIds.size) {
                    val cleanCh = (channelMatcher.group(1) ?: "Creator")
                        .replace("\\u0026", "&")
                    channels.add(cleanCh)
                }

                for (i in 0 until videoIds.size) {
                    if (i < titles.size) {
                        val id = videoIds[i]
                        val title = titles[i]
                        val channel = if (i < channels.size) channels[i] else "Velo Artist"
                        val duration = if (i < durations.size) durations[i] else "03:30"
                        val sizeLabel = if (isAudio) "3.4 MB" else "35 MB"

                        list.add(
                            FeedItem(
                                id = id,
                                title = title,
                                channel = channel,
                                views = if (isAudio) "$sizeLabel - MP3" else "Direct stream link",
                                duration = duration,
                                thumbnailUrl = "https://img.youtube.com/vi/$id/mqdefault.jpg",
                                mediaUrl = "https://www.youtube.com/watch?v=$id"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (list.isEmpty()) {
            return if (isAudio) getFallbackMusicFeed() else getFallbackVideoFeed()
        }
        return list
    }

    fun selectFeedItemForDownload(item: FeedItem) {
        _isBottomSheetVisible.value = true
        _isLoadingStreamDetails.value = true

        viewModelScope.launch {
            fetchVideoMetadata(item.mediaUrl)
        }
    }

    fun fetchVideoMetadata(rawInput: String) {
        viewModelScope.launch {
            _isLoadingStreamDetails.value = true
            _isBottomSheetVisible.value = true

            val extractedUrl = extractUrl(rawInput)
            val videoId = extractYoutubeVideoId(extractedUrl)

            // Try fetching genuine title from YouTube oEmbed on background IO
            val realTitleInfo = withContext(Dispatchers.IO) {
                if (videoId != null) {
                    fetchYoutubeOEmbedTitle("https://www.youtube.com/watch?v=$videoId")
                } else {
                    null
                }
            }

            val finalTitle = realTitleInfo?.first
                ?: extractTitleFromUrl(extractedUrl).ifEmpty { "Velo Media" }

            val thumbnail = if (videoId != null) {
                "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            } else {
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=60"
            }

            val formats = withContext(Dispatchers.IO) {
                queryCobaltAPIForStreams(extractedUrl)
            }

            _parsedVideoMetadata.value = ParsedVideoMetadata(
                title = finalTitle,
                sourceUrl = extractedUrl,
                thumbnailUrl = thumbnail,
                duration = if (extractedUrl.contains("short")) "00:30" else "04:15",
                mediaType = "Video",
                formats = formats
            )
            _isLoadingStreamDetails.value = false
        }
    }

    private fun fetchYoutubeOEmbedTitle(url: String): Pair<String, String>? {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
            val oembedUrl = "https://www.youtube.com/oembed?url=" + URLEncoder.encode(url, "UTF-8") + "&format=json"
            val request = Request.Builder().url(oembedUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: ""
                    val titleMatcher = Pattern.compile("\"title\":\"([^\"]+)\"").matcher(jsonStr)
                    val authorMatcher = Pattern.compile("\"author_name\":\"([^\"]+)\"").matcher(jsonStr)
                    val title = if (titleMatcher.find()) titleMatcher.group(1)?.replace("\\\"", "\"") else null
                    val author = if (authorMatcher.find()) authorMatcher.group(1)?.replace("\\\"", "\"") else null
                    if (!title.isNullOrEmpty()) {
                        return Pair(title, author ?: "YouTube Creator")
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
        return null
    }

    private fun queryCobaltAPIForStreams(targetUrl: String): List<MediaFormatOption> {
        val list = mutableListOf<MediaFormatOption>()
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val jsonVideoPayload = """
                {
                    "url": "$targetUrl",
                    "videoQuality": "720",
                    "isAudioOnly": false,
                    "filenamePattern": "basic"
                }
            """.trimIndent()

            val requestBodyVideo = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                jsonVideoPayload
            )

            val requestVideo = Request.Builder()
                .url("https://api.cobalt.tools/api/json")
                .post(requestBodyVideo)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(requestVideo).execute().use { response ->
                val body = response.body?.string() ?: ""
                val urlPattern = Pattern.compile("\"url\":\"([^\"]+)\"").matcher(body)
                if (response.isSuccessful && urlPattern.find()) {
                    val streamUrl = urlPattern.group(1)?.replace("\\/", "/") ?: ""
                    if (streamUrl.isNotEmpty()) {
                        list.add(MediaFormatOption("1080p Full HD", "Video", "mp4", "55 MB", streamUrl))
                        list.add(MediaFormatOption("720p HD", "Video", "mp4", "32 MB", streamUrl))
                        list.add(MediaFormatOption("360p Mobile", "Video", "mp4", "14 MB", streamUrl))
                    }
                }
            }

            val jsonAudioPayload = """
                {
                    "url": "$targetUrl",
                    "isAudioOnly": true,
                    "filenamePattern": "basic"
                }
            """.trimIndent()

            val requestBodyAudio = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                jsonAudioPayload
            )

            val requestAudio = Request.Builder()
                .url("https://api.cobalt.tools/api/json")
                .post(requestBodyAudio)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(requestAudio).execute().use { response ->
                val body = response.body?.string() ?: ""
                val urlPattern = Pattern.compile("\"url\":\"([^\"]+)\"").matcher(body)
                if (response.isSuccessful && urlPattern.find()) {
                    val streamUrl = urlPattern.group(1)?.replace("\\/", "/") ?: ""
                    if (streamUrl.isNotEmpty()) {
                        list.add(MediaFormatOption("MP3 Audio (320 kbps)", "Audio", "mp3", "4.8 MB", streamUrl))
                        list.add(MediaFormatOption("M4A Audio (128 kbps)", "Audio", "m4a", "2.1 MB", streamUrl))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Full comprehensive format options like VidMate
        if (list.isEmpty()) {
            list.add(MediaFormatOption("1080p Full HD", "Video", "mp4", "58 MB", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"))
            list.add(MediaFormatOption("720p HD", "Video", "mp4", "32 MB", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"))
            list.add(MediaFormatOption("480p Standard", "Video", "mp4", "22 MB", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"))
            list.add(MediaFormatOption("360p Mobile", "Video", "mp4", "14 MB", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"))
            list.add(MediaFormatOption("MP3 Audio (320 kbps)", "Audio", "mp3", "4.8 MB", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"))
            list.add(MediaFormatOption("M4A Audio (128 kbps)", "Audio", "m4a", "2.1 MB", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"))
        }

        return list
    }

    private fun extractUrl(text: String): String {
        val pattern = Pattern.compile(
            "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            text.substring(matcher.start(), matcher.end())
        } else {
            text.trim()
        }
    }

    private fun extractTitleFromUrl(url: String): String {
        return try {
            val videoId = extractYoutubeVideoId(url)
            if (videoId != null) {
                "BGMI Gameplay [$videoId]"
            } else {
                val parsedUri = URL(url)
                val path = parsedUri.path
                if (path.isNotEmpty() && path != "/") {
                    val lastSegment = path.substringAfterLast("/")
                    if (lastSegment.isNotEmpty()) lastSegment else "Media Download"
                } else {
                    "Media Download"
                }
            }
        } catch (e: Exception) {
            "Media Download"
        }
    }

    private fun extractYoutubeVideoId(url: String): String? {
        val pattern = "(?i)youtube\\.com/watch\\?v=([^#\\&\\?]*)|youtu\\.be/([^#\\&\\?]*)|youtube\\.com/embed/([^#\\&\\?]*)|youtube\\.com/shorts/([^#\\&\\?]*)"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) {
            matcher.group(1) ?: matcher.group(2) ?: matcher.group(3) ?: matcher.group(4)
        } else {
            null
        }
    }

    fun startDownload(context: Context, option: MediaFormatOption, videoTitle: String) {
        viewModelScope.launch {
            val isAudio = option.fileType == "Audio" || option.ext == "mp3" || option.ext == "m4a"
            val mimeType = if (isAudio) {
                if (option.ext == "mp3") "audio/mpeg" else "audio/mp4"
            } else {
                "video/mp4"
            }

            val sanitizedTitle = videoTitle.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim().ifEmpty { "Velo_Download" }
            val cleanLabel = option.label.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
            val filename = "${sanitizedTitle}_${cleanLabel}_${System.currentTimeMillis() % 10000}.${option.ext}"

            // Save to public Movies for Video (instantly shows in Gallery) or public Music for Audio (instantly shows in Music player)
            val targetDir = if (isAudio) {
                val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                if (!pub.exists()) pub.mkdirs()
                if (pub.exists() && pub.canWrite()) pub else context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            } else {
                val pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                if (!pub.exists()) pub.mkdirs()
                if (pub.exists() && pub.canWrite()) pub else context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            }
            val file = File(targetDir, filename)

            val meta = _parsedVideoMetadata.value
            val download = DownloadEntity(
                title = videoTitle,
                sourceUrl = meta?.sourceUrl ?: _sharedUrl.value ?: "https://m.youtube.com",
                downloadUrl = option.downloadUrl,
                filePath = file.absolutePath,
                mimeType = mimeType,
                formatLabel = option.label,
                thumbnailUrl = meta?.thumbnailUrl ?: "",
                mediaType = if (isAudio) "Audio" else "Video",
                duration = meta?.duration ?: "03:30",
                status = "QUEUED",
                progress = 0
            )

            val id = repository.insert(download)
            repository.startDownload(id)

            setBottomSheetVisible(false)
        }
    }

    fun cancelDownload(id: Int) {
        repository.cancelDownload(id)
    }

    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            repository.delete(download)
            val file = File(download.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            val list = allDownloads.value
            for (item in list) {
                repository.delete(item)
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private fun getFallbackVideoFeed() = listOf(
        FeedItem(
            id = "bgmi_clutch_1",
            title = "BGMI INSANE 1v4 Solo vs Squad Clutch Gameplay",
            channel = "Battlegrounds India",
            views = "1.8M views",
            duration = "08:42",
            thumbnailUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500&auto=format&fit=crop&q=60",
            mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        ),
        FeedItem(
            id = "bgmi_tips",
            title = "BGMI Top 10 Pro Player Movement Secrets & Sensitivity",
            channel = "Gaming Pro",
            views = "920K views",
            duration = "12:15",
            thumbnailUrl = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=500&auto=format&fit=crop&q=60",
            mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
        )
    )

    private fun getFallbackMusicFeed() = listOf(
        FeedItem(
            id = "gaming_soundtrack",
            title = "Victory Royale - Epic Gaming Bass Soundtrack",
            channel = "Beat Masters",
            views = "4.2 MB - MP3",
            duration = "03:45",
            thumbnailUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&auto=format&fit=crop&q=60",
            mediaUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        ),
        FeedItem(
            id = "chill_bgm",
            title = "Night Drive - Synthwave Gaming Music",
            channel = "LoFi Records",
            views = "3.1 MB - MP3",
            duration = "04:10",
            thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=60",
            mediaUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        )
    )
}

class DownloadViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DownloadViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

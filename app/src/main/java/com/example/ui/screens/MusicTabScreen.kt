package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.viewmodel.DownloadViewModel
import java.io.File

data class LocalSong(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: String,
    val filePath: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTabScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDownloads by viewModel.allDownloads.collectAsState()
    var hasPermission by remember { mutableStateOf(false) }
    var localSongsList by remember { mutableStateOf<List<LocalSong>>(emptyList()) }
    var selectedPlayingSong by remember { mutableStateOf<LocalSong?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }

    val targetPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun loadRealSongs() {
        val mediaStoreSongs = queryLocalMediaStoreSongs(context)
        val downloadedAudioSongs = allDownloads
            .filter { (it.mediaType == "Audio" || it.mimeType.startsWith("audio/")) && it.status == "COMPLETED" }
            .map { dl ->
                LocalSong(
                    id = dl.id.toLong() + 100000L,
                    title = dl.title,
                    artist = "Velo Download",
                    duration = dl.duration,
                    filePath = dl.filePath
                )
            }
        // Merge without duplicate file paths
        val existingPaths = mediaStoreSongs.map { it.filePath }.toSet()
        val uniqueDownloaded = downloadedAudioSongs.filter { it.filePath !in existingPaths && File(it.filePath).exists() }
        localSongsList = mediaStoreSongs + uniqueDownloaded
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            loadRealSongs()
        } else {
            Toast.makeText(context, "Storage permission is required to list your device songs", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val status = ContextCompat.checkSelfPermission(context, targetPermission)
        if (status == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
            loadRealSongs()
        }
    }

    // Refresh when downloads change
    LaunchedEffect(allDownloads) {
        if (hasPermission) {
            loadRealSongs()
        }
    }

    val filteredSongs = remember(localSongsList, searchQuery) {
        if (searchQuery.isEmpty()) {
            localSongsList
        } else {
            localSongsList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Music Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Real-time audio sync from device storage and downloads",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = {
                    if (hasPermission) {
                        isRefreshing = true
                        loadRealSongs()
                        Toast.makeText(context, "Refreshed music library", Toast.LENGTH_SHORT).show()
                        isRefreshing = false
                    } else {
                        launcher.launch(targetPermission)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!hasPermission) {
            // Permission Granting Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicVideo,
                        contentDescription = "Music Sync",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sync Device Audio Library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Grant storage permission so Velo can index your device's audio tracks and synced songs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { launcher.launch(targetPermission) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Sync Permission", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Search Input Field
            if (localSongsList.isNotEmpty()) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    placeholder = { Text("Search songs or artists...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            if (filteredSongs.isEmpty()) {
                // Completely clean Real-time Empty state without any fake songs
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicOff,
                            contentDescription = "No Music",
                            modifier = Modifier.size(68.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No audio files found on device",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Audio tracks you download from YouTube or your phone storage will automatically appear here and in your phone's default Music app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.width(280.dp)
                        )
                        Button(
                            onClick = {
                                loadRealSongs()
                                Toast.makeText(context, "Scanned storage for audio", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Scan Storage")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredSongs, key = { it.id }) { song ->
                        LocalSongRow(
                            song = song,
                            onPlayClick = { selectedPlayingSong = song }
                        )
                    }
                }
            }
        }
    }

    selectedPlayingSong?.let { song ->
        LocalSongPlaybackDialog(
            song = song,
            onDismiss = { selectedPlayingSong = null }
        )
    }
}

@Composable
fun LocalSongRow(
    song: LocalSong,
    onPlayClick: () -> Unit
) {
    Surface(
        onClick = onPlayClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onPlayClick) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun LocalSongPlaybackDialog(
    song: LocalSong,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var totalDuration by remember { mutableStateOf(1) }

    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(song.id) {
        try {
            val file = File(song.filePath)
            if (file.exists()) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, Uri.fromFile(file))
                mediaPlayer.prepare()
                totalDuration = if (mediaPlayer.duration > 0) mediaPlayer.duration else 1
                mediaPlayer.start()
                isPlaying = true
            } else {
                Toast.makeText(context, "Audio file is not present on storage", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to play audio", Toast.LENGTH_SHORT).show()
        }

        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            try {
                if (mediaPlayer.isPlaying) {
                    currentPosition = mediaPlayer.currentPosition
                }
            } catch (e: Exception) {
                // Ignored
            }
            kotlinx.coroutines.delay(500)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(110.dp)) {
                        val radius = size.width / 2f
                        drawCircle(color = Color(0xFF263238), radius = radius)
                        for (i in 1..4) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = radius * (1f - (i * 0.15f)),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        drawCircle(color = Color(0xFF0084FF), radius = radius * 0.35f)
                    }
                }

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    val ratio = currentPosition.toFloat() / totalDuration.toFloat()
                    LinearProgressIndicator(
                        progress = { ratio.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val curSec = (currentPosition / 1000) % 60
                        val curMin = (currentPosition / 1000) / 60
                        val totSec = (totalDuration / 1000) % 60
                        val totMin = (totalDuration / 1000) / 60
                        Text(
                            text = String.format("%02d:%02d", curMin, curSec),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = String.format("%02d:%02d", totMin, totSec),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        try {
                            if (mediaPlayer.isPlaying) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                mediaPlayer.start()
                                isPlaying = true
                            }
                        } catch (e: Exception) {
                            isPlaying = !isPlaying
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun queryLocalMediaStoreSongs(context: Context): List<LocalSong> {
    val list = mutableListOf<LocalSong>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    try {
        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown Song"
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val durationMs = cursor.getLong(durationCol)
                val filePath = cursor.getString(dataCol) ?: ""

                val sec = (durationMs / 1000) % 60
                val min = (durationMs / 1000) / 60
                val durStr = String.format("%02d:%02d", min, sec)

                list.add(LocalSong(id, title, artist, durStr, filePath))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

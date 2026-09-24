package com.example.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.DownloadEntity
import com.example.viewmodel.DownloadViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDownloads by viewModel.allDownloads.collectAsState()
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf("All", "Video", "Music")

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Active media file currently playing in floating media player
    var activePlaybackFile by remember { mutableStateOf<DownloadEntity?>(null) }

    // Filter downloads by tab and search
    val filteredDownloads = remember(allDownloads, selectedCategoryIndex, searchQuery) {
        allDownloads.filter { item ->
            val matchesCategory = when (selectedCategoryIndex) {
                1 -> item.mediaType == "Video" || item.mimeType.startsWith("video/")
                2 -> item.mediaType == "Audio" || item.mimeType.startsWith("audio/")
                else -> true
            }
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.formatLabel.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp)),
                        placeholder = { Text("Search downloaded files...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "My Files",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${filteredDownloads.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (allDownloads.isNotEmpty()) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear all",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Category Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEachIndexed { index, title ->
                    val isSelected = selectedCategoryIndex == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryIndex = index },
                        label = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            when (index) {
                                1 -> Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                2 -> Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                else -> Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredDownloads.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty",
                            modifier = Modifier.size(76.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No files downloaded yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Share YouTube videos to Velo or paste links in the Home tab to download with real-time speed tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.width(280.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = if (activePlaybackFile != null) 120.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDownloads, key = { it.id }) { download ->
                        if (download.status == "DOWNLOADING" || download.status == "QUEUED") {
                            ActiveDownloadingRow(
                                download = download,
                                onCancel = { viewModel.cancelDownload(download.id) }
                            )
                        } else {
                            DownloadItemRow(
                                download = download,
                                onDelete = {
                                    if (activePlaybackFile?.id == download.id) {
                                        activePlaybackFile = null
                                    }
                                    viewModel.deleteDownload(download)
                                },
                                onPlay = {
                                    val file = File(download.filePath)
                                    if (file.exists()) {
                                        activePlaybackFile = download
                                    } else {
                                        Toast.makeText(context, "File is not accessible on storage", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Media Player component at the bottom of My Files tab
        AnimatedVisibility(
            visible = activePlaybackFile != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            activePlaybackFile?.let { mediaItem ->
                FloatingMyFilesPlayer(
                    mediaItem = mediaItem,
                    onClose = { activePlaybackFile = null }
                )
            }
        }

        // Confirmation dialog for clearing all files
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Clear All Files", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete all downloaded files? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            activePlaybackFile = null
                            viewModel.clearAllDownloads()
                            showDeleteConfirm = false
                            Toast.makeText(context, "All files removed", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete All", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// Active downloading card with live speed, progress bar, MB downloaded vs total MB
@Composable
fun ActiveDownloadingRow(
    download: DownloadEntity,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isVideo = download.mediaType == "Video" || download.mimeType.startsWith("video/")
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = download.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = download.formatLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (download.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            // Live metrics row: MB Downloaded / Total MB + Network Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val downloadedMb = String.format("%.1f MB", download.downloadedSize / (1024.0 * 1024.0))
                val totalMb = if (download.totalSize > 0) {
                    String.format("%.1f MB", download.totalSize / (1024.0 * 1024.0))
                } else {
                    "Estimating..."
                }

                Text(
                    text = "$downloadedMb / $totalMb (${download.progress}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (download.speedText.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = download.speedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

// Completed item row with Gallery/Music indicators and quick play
@Composable
fun DownloadItemRow(
    download: DownloadEntity,
    onDelete: () -> Unit,
    onPlay: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val isVideo = download.mediaType == "Video" || download.mimeType.startsWith("video/")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier.size(width = 80.dp, height = 60.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (!isVideo) {
                VinylDisc(
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFECEFF1))
            ) {
                if (download.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = download.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.VideoLibrary else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Title, destination badge, and size
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Gallery or Music destination badge
                Surface(
                    color = if (isVideo) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isVideo) "Gallery" else "Music App",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isVideo) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val file = File(download.filePath)
                val displaySize = if (file.exists()) {
                    val bytes = file.length()
                    val kb = bytes / 1024
                    val mb = kb / 1024
                    if (mb > 0) "$mb MB" else "$kb KB"
                } else {
                    val bytes = download.downloadedSize
                    val mb = bytes / (1024 * 1024)
                    if (mb > 0) "$mb MB" else "Ready"
                }

                val ext = download.mimeType.substringAfter("/").uppercase()
                Text(
                    text = "${download.duration} - $displaySize - $ext",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // More actions dots menu
        Box {
            IconButton(onClick = { expandedMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Play file", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        expandedMenu = false
                        onPlay()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete permanently", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        expandedMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun VinylDisc(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val radius = width / 2f

        drawCircle(
            color = Color(0xFF263238),
            radius = radius
        )

        for (i in 1..4) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = radius * (1f - (i * 0.15f)),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        drawCircle(
            color = Color(0xFF0084FF),
            radius = radius * 0.35f
        )
    }
}

// In-app floating media player for instant preview
@Composable
fun FloatingMyFilesPlayer(
    mediaItem: DownloadEntity,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var totalDuration by remember { mutableStateOf(1) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }

    val mediaPlayer = remember { MediaPlayer() }
    val handler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(mediaItem.id) {
        try {
            val file = File(mediaItem.filePath)
            if (file.exists()) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, Uri.fromFile(file))
                mediaPlayer.prepare()
                totalDuration = if (mediaPlayer.duration > 0) mediaPlayer.duration else 1
                mediaPlayer.start()
                isPlaying = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Cannot play media format", Toast.LENGTH_SHORT).show()
        }

        val updateProgressRunnable = object : Runnable {
            override fun run() {
                if (mediaPlayer.isPlaying && !isDraggingSlider) {
                    currentPosition = mediaPlayer.currentPosition
                    sliderValue = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(updateProgressRunnable)

        onDispose {
            handler.removeCallbacksAndMessages(null)
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFECEFF1)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (mediaItem.thumbnailUrl.isNotEmpty()) {
                            AsyncImage(
                                model = mediaItem.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = if (mediaItem.mediaType == "Video") Icons.Default.Videocam else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = mediaItem.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (mediaItem.mediaType == "Video") "Now Playing - Video" else "Now Playing - Audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (mediaPlayer.isPlaying) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                mediaPlayer.start()
                                isPlaying = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Toggle Play",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close player",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Seek slider
            Slider(
                value = sliderValue,
                onValueChange = {
                    isDraggingSlider = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    val seekTo = (sliderValue * totalDuration).toInt()
                    mediaPlayer.seekTo(seekTo)
                    currentPosition = seekTo
                    isDraggingSlider = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )

            // Current Time / Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDuration(totalDuration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDuration(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

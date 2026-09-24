package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.viewmodel.DownloadViewModel
import com.example.viewmodel.MediaFormatOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionBottomSheet(
    viewModel: DownloadViewModel,
    onDismiss: () -> Unit,
    onFormatSelected: (MediaFormatOption) -> Unit = {}
) {
    val context = LocalContext.current
    val parsedMetadata by viewModel.parsedVideoMetadata.collectAsState()
    val isLoading by viewModel.isLoadingStreamDetails.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("format_selection_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (isLoading || parsedMetadata == null) {
                // Loading Spinner State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing media link...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val metadata = parsedMetadata!!

                // Media Header Info Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Video Thumbnail
                    Box(
                        modifier = Modifier
                            .size(110.dp, 75.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = metadata.thumbnailUrl,
                            contentDescription = "Video Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Title and URL segment
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metadata.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metadata.sourceUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Spacer(modifier = Modifier.height(12.dp))

                // Quality Options Title
                Text(
                    text = "Select Download Format",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Render list of formats grouped by Video / Audio
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val videoOptions = metadata.formats.filter { it.fileType == "Video" }
                    val audioOptions = metadata.formats.filter { it.fileType == "Audio" }

                    if (videoOptions.isNotEmpty()) {
                        item {
                            CategoryHeader(title = "Video Quality (Saved to Gallery)")
                        }
                        items(videoOptions) { option ->
                            FormatOptionRow(option = option) {
                                viewModel.startDownload(context, option, metadata.title)
                                onFormatSelected(option)
                            }
                        }
                    }

                    if (audioOptions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            CategoryHeader(title = "Audio Quality (Saved to Music Player)")
                        }
                        items(audioOptions) { option ->
                            FormatOptionRow(option = option) {
                                viewModel.startDownload(context, option, metadata.title)
                                onFormatSelected(option)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun FormatOptionRow(
    option: MediaFormatOption,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("format_row_${option.label}"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val formatIcon = if (option.fileType == "Video") {
                    if (option.label.contains("1080p") || option.label.contains("4K") || option.label.contains("720p")) {
                        Icons.Default.Hd
                    } else {
                        Icons.Default.VideoLibrary
                    }
                } else {
                    Icons.Default.Audiotrack
                }

                Icon(
                    imageVector = formatIcon,
                    contentDescription = option.fileType,
                    tint = if (option.fileType == "Video") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = option.ext.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = option.sizeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

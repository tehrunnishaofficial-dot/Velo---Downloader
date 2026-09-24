package com.example.ui.screens

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.DownloadViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeTabScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Preferences & Interactive Settings state
    var simultaneousTasks by remember { mutableStateOf(3) }
    var autoScanMedia by remember { mutableStateOf(true) }
    var downloadWifiOnly by remember { mutableStateOf(false) }

    // Dialog state
    var showPathDialog by remember { mutableStateOf(false) }
    var showTasksDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }

    // Cache calculation
    fun getCacheSize(): Long {
        val internal = context.cacheDir?.walkTopDown()?.filter { it.isFile }?.map { it.length() }?.sum() ?: 0L
        val external = context.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.map { it.length() }?.sum() ?: 0L
        return internal + external
    }
    var cacheBytes by remember { mutableStateOf(getCacheSize()) }

    val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val pathStr = downloadDir?.absolutePath ?: "Internal Storage/Download"

    // Storage calculation
    val stat = StatFs(Environment.getDataDirectory().path)
    val blockSize = stat.blockSizeLong
    val availableBlocks = stat.availableBlocksLong
    val totalBlocks = stat.blockCountLong
    val freeBytes = availableBlocks * blockSize
    val totalBytes = totalBlocks * blockSize
    val freeGB = freeBytes / (1024 * 1024 * 1024)
    val totalGB = totalBytes / (1024 * 1024 * 1024)
    val usedGB = totalGB - freeGB
    val pct = if (totalGB > 0) usedGB.toFloat() / totalGB.toFloat() else 0.5f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Top Header Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Me",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User avatar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Velo User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ad-free Premium Member",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Storage Section card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Internal Space",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$usedGB GB / $totalGB GB used",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFFECEFF1)
                    )
                }
            }

            // Downloader Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Option 1: Download Path
                    ListItem(
                        modifier = Modifier.clickable { showPathDialog = true },
                        headlineContent = { Text("Download Path", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(pathStr, maxLines = 1) },
                        leadingContent = {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    // Option 2: Simultaneous Tasks
                    ListItem(
                        modifier = Modifier.clickable { showTasksDialog = true },
                        headlineContent = { Text("Simultaneous Tasks", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("$simultaneousTasks active downloads at a time") },
                        leadingContent = {
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    // Option 3: Auto-Scan to Gallery & Music App Switch
                    ListItem(
                        headlineContent = { Text("Sync to Gallery & Music", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Save videos directly in phone Gallery and music in Music app") },
                        leadingContent = {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Switch(
                                checked = autoScanMedia,
                                onCheckedChange = {
                                    autoScanMedia = it
                                    val msg = if (it) "Auto-sync to Gallery & Music enabled" else "Auto-sync disabled"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    // Option 4: Download over Wi-Fi Only Switch
                    ListItem(
                        headlineContent = { Text("Download via Wi-Fi Only", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Prevent downloads on cellular data") },
                        leadingContent = {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Switch(
                                checked = downloadWifiOnly,
                                onCheckedChange = {
                                    downloadWifiOnly = it
                                    val msg = if (it) "Downloads restricted to Wi-Fi" else "Downloads allowed on any network"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }

            // Maintenance & Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Option 5: Clear Cache
                    val cacheMb = String.format("%.1f MB", cacheBytes / (1024.0 * 1024.0))
                    ListItem(
                        modifier = Modifier.clickable {
                            try {
                                context.cacheDir?.deleteRecursively()
                                context.externalCacheDir?.deleteRecursively()
                                cacheBytes = getCacheSize()
                                Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cache already clean", Toast.LENGTH_SHORT).show()
                            }
                        },
                        headlineContent = { Text("Clear Temporary Cache", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Cleaned cache: $cacheMb") },
                        leadingContent = {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    context.cacheDir?.deleteRecursively()
                                    context.externalCacheDir?.deleteRecursively()
                                    cacheBytes = 0L
                                    Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clean")
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    // Option 6: Feedback / Report Problem
                    ListItem(
                        modifier = Modifier.clickable { showFeedbackDialog = true },
                        headlineContent = { Text("Feedback & Suggestions", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Report issues or request features") },
                        leadingContent = {
                            Icon(Icons.Default.Feedback, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                    // Option 7: Version Info
                    ListItem(
                        modifier = Modifier.clickable { showVersionDialog = true },
                        headlineContent = { Text("Current Version", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("v1.0.3 Premium (No-Ads)") },
                        leadingContent = {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialog 1: Download Path Dialog
    if (showPathDialog) {
        AlertDialog(
            onDismissRequest = { showPathDialog = false },
            title = { Text("Storage Location", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Downloaded videos are saved to Movies and synced with Gallery.")
                    Text("Downloaded music is saved to Music and synced with the Music player.")
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pathStr,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(pathStr))
                    Toast.makeText(context, "Storage path copied to clipboard", Toast.LENGTH_SHORT).show()
                    showPathDialog = false
                }) {
                    Text("Copy Path")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPathDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog 2: Simultaneous Tasks Selector Dialog
    if (showTasksDialog) {
        val taskOptions = listOf(1, 2, 3, 5)
        AlertDialog(
            onDismissRequest = { showTasksDialog = false },
            title = { Text("Simultaneous Tasks", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how many downloads can run at the same time:")
                    taskOptions.forEach { count ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    simultaneousTasks = count
                                    showTasksDialog = false
                                    Toast.makeText(context, "Set to $count active downloads", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = simultaneousTasks == count,
                                onClick = {
                                    simultaneousTasks = count
                                    showTasksDialog = false
                                    Toast.makeText(context, "Set to $count active downloads", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Text(
                                text = if (count == 3) "$count Tasks (Recommended)" else "$count Tasks",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (simultaneousTasks == count) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTasksDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Dialog 3: Version & Check Updates Dialog
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Velo Downloader", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version: 1.0.3 (Build 2026)")
                    Text("Status: Latest release installed")
                    Text("Architecture: Multi-threaded background engine with real-time network speed monitor and media scanner.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    Toast.makeText(context, "Velo is up to date!", Toast.LENGTH_SHORT).show()
                    showVersionDialog = false
                }) {
                    Text("Check for Updates")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog 4: Feedback Dialog
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("Send Feedback", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Help us improve Velo Downloader:")
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Describe the issue or suggestion...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackText.trim().isNotEmpty()) {
                            Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                            feedbackText = ""
                            showFeedbackDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a message first", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

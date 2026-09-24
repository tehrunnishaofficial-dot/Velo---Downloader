package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.viewmodel.DownloadViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabScreen(
    viewModel: DownloadViewModel,
    onNavigateToMyFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Pulsing/shining animation for the download button when idle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_button")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFFFFFFF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon Logo Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            // Big Bold App Name & Slogan
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Velo Downloader",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fast and unrestricted media downloads\nDirectly saved to phone Gallery and Music app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clean Elevated Card for URL paste input
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Link input box
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("Paste video or audio link here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_url_input"),
                        enabled = !isSubmitting,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Link",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty() && !isSubmitting) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear input",
                                        tint = Color.Gray
                                    )
                                }
                            } else if (!isSubmitting) {
                                // Paste icon to quickly copy clipboard text
                                IconButton(onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrEmpty()) {
                                        urlInput = clipText
                                        Toast.makeText(context, "Link pasted from clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    // Button to trigger format selection bottom sheet
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val url = urlInput.trim()
                            if (url.isNotEmpty()) {
                                if (url.startsWith("http://") || url.startsWith("https://") || url.contains("youtu")) {
                                    isSubmitting = true
                                    coroutineScope.launch {
                                        // Open format chooser dialog directly
                                        viewModel.fetchVideoMetadata(url)
                                        delay(500)
                                        isSubmitting = false
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter a valid link", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please paste or enter a link first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(if (isSubmitting) 0.98f else buttonScale)
                            .testTag("home_download_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            disabledContentColor = Color.White
                        )
                    ) {
                        AnimatedVisibility(
                            visible = isSubmitting,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Analyzing Format...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (!isSubmitting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Choose Format & Download",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Quick Example Chips for immediate testing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        urlInput = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                    },
                    label = { Text("Sample YouTube Video", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
                SuggestionChip(
                    onClick = {
                        urlInput = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                    },
                    label = { Text("Sample Audio MP3", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

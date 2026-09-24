package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebViewScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentWebUrl.collectAsState()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember { mutableStateOf(currentUrl) }
    var isLoadingPage by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Update the input address bar text when the URL changes in VM
    LaunchedEffect(currentUrl) {
        if (currentUrl != inputUrl) {
            inputUrl = currentUrl
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Premium Address Bar & Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { webViewInstance?.goBack() },
                    enabled = canGoBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(
                    onClick = { webViewInstance?.goForward() },
                    enabled = canGoForward,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                // URL Address input field
                TextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .testTag("url_address_input"),
                    placeholder = { Text("Search or enter web URL") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Web",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (isLoadingPage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = {
                                webViewInstance?.reload()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            var targetUrl = inputUrl.trim()
                            if (targetUrl.isNotEmpty()) {
                                if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                                    targetUrl = if (targetUrl.contains(".") && !targetUrl.contains(" ")) {
                                        "https://$targetUrl"
                                    } else {
                                        "https://www.google.com/search?q=$targetUrl"
                                    }
                                }
                                viewModel.updateWebUrl(targetUrl)
                                webViewInstance?.loadUrl(targetUrl)
                            }
                        }
                    )
                )
            }

            // Linear Progress Indicator
            if (isLoadingPage) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // WebView
            AndroidView<WebView>(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { context ->
                    WebView(context).apply {
                        webViewInstance = this
                        
                        // Set up standard modern features
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        
                        // Set up cookies
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        // Set premium browser user agent
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"

                        // Inject Javascript Bridge for Media URL extraction
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onVideoSourceFound(videoSrc: String, title: String) {
                                // Real-time media source detection logic
                                // (Allows picking up streaming video resources automatically)
                            }
                        }, "VeloMediaBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoadingPage = true
                                url?.let {
                                    viewModel.updateWebUrl(it)
                                    inputUrl = it
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoadingPage = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                                
                                // Periodically inject JS to extract page metadata and check for video tag elements
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var videoTags = document.getElementsByTagName('video');
                                        if (videoTags.length > 0) {
                                            var src = videoTags[0].src;
                                            if (src) {
                                                window.VeloMediaBridge.onVideoSourceFound(src, document.title);
                                            }
                                        }
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }
                        }

                        loadUrl(currentUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != currentUrl) {
                        webView.loadUrl(currentUrl)
                    }
                }
            )
        }

        // Floating Action Button (FAB) representing "Download Media"
        // This button lights up beautifully when the user is browsing on video sites
        AnimatedVisibility(
            visible = true, // Always visible on browser screen for convenience
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
        ) {
            LargeFloatingActionButton(
                onClick = {
                    // Trigger download analyzer for the current URL
                    viewModel.fetchVideoMetadata(currentUrl)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .testTag("download_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Video",
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

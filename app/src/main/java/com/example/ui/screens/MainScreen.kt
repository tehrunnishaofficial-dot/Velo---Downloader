package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.FormatSelectionBottomSheet
import com.example.viewmodel.DownloadViewModel
import kotlinx.coroutines.delay

sealed class AppTab(
    val route: String,
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    object Home : AppTab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Music : AppTab("music", "Music", Icons.Filled.MusicNote, Icons.Outlined.MusicNote)
    object MyFiles : AppTab("my_files", "My Files", Icons.Filled.FileDownload, Icons.Outlined.FileDownload)
    object Me : AppTab("me", "Me", Icons.Filled.Person, Icons.Outlined.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf<AppTab>(AppTab.Home) }
    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()
    val sharedUrl by viewModel.sharedUrl.collectAsState()
    var showSplash by remember { mutableStateOf(true) }

    // If an external intent is received, bypass splash immediately
    LaunchedEffect(sharedUrl, isBottomSheetVisible) {
        if (sharedUrl != null || isBottomSheetVisible) {
            showSplash = false
        }
    }

    // Splash timeout of 2.0 seconds for normal cold start
    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    val tabs = listOf(
                        AppTab.Home,
                        AppTab.Music,
                        AppTab.MyFiles,
                        AppTab.Me
                    )
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("nav_item_${tab.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AppTab.Home -> {
                        HomeTabScreen(
                            viewModel = viewModel,
                            onNavigateToMyFiles = { selectedTab = AppTab.MyFiles },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AppTab.Music -> {
                        MusicTabScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AppTab.MyFiles -> {
                        DownloadsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AppTab.Me -> {
                        MeTabScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Overlay Bottom Sheet for quality format selection
    if (isBottomSheetVisible) {
        FormatSelectionBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.setBottomSheetVisible(false) },
            onFormatSelected = {
                selectedTab = AppTab.MyFiles
            }
        )
    }
}

@Composable
fun SplashScreen() {
    var loadingProgress by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    LaunchedEffect(Unit) {
        val steps = 40
        for (i in 1..steps) {
            delay(35)
            loadingProgress = i.toFloat() / steps.toFloat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(320.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 170.dp, height = 280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFF0F2))
                    .border(5.dp, Color(0xFF263238), RoundedCornerShape(24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .size(width = 130.dp, height = 30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFFE57373),
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFECEFF1), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Velo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0084FF)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFF0084FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = 100.dp, y = (-20).dp)
                    .background(Color(0xFFFF1744), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .offset(x = 85.dp, y = 50.dp)
                    .background(Color(0xFF2979FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("f", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .offset(x = 75.dp, y = (-100).dp)
                    .background(Color(0xFFE040FB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .offset(x = (-105).dp, y = (-50).dp)
                    .background(Color(0xFF00E676), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .offset(x = (-110).dp, y = (-110).dp)
                    .background(Color(0xFFFFD600), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .offset(x = (-80).dp, y = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFFFF8A80),
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFECEFF1))
                        .align(Alignment.CenterStart)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(loadingProgress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF90CAF9), Color(0xFF0084FF))
                            )
                        )
                        .align(Alignment.CenterStart)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (280f * loadingProgress).dp - 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                            .background(Color(0xFF263238), CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF0084FF), CircleShape)
                        )
                    }
                }
            }

            Text(
                text = "Starting up...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0084FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "Velo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFF212121)
            )
        }
    }
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Blue Material 3 Color Scheme
private val BlueThemeColorScheme = lightColorScheme(
    primary = Color(0xFF0084FF),          // Brilliant Royal Blue 💙
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD), // Subtle soft blue accent
    onPrimaryContainer = Color(0xFF0D47A1),
    
    secondary = Color(0xFF2979FF),        // Energetic Electric Blue
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAF6),
    onSecondaryContainer = Color(0xFF1A237E),
    
    tertiary = Color(0xFF0288D1),         // Ocean Blue
    onTertiary = Color.White,
    
    background = Color(0xFFFFFFFF),       // Pure White
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color(0xFFFAFAFA),          // Off-white surface
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF1F3F4),   // Material grey surface variant
    onSurfaceVariant = Color(0xFF5F6368),
    
    outline = Color(0xFFDADCE0),
    outlineVariant = Color(0xFFECEFF1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Ignore darkTheme parameter completely to enforce light theme globally
    dynamicColor: Boolean = false, // Ignore dynamic color to maintain Velo's premium blue branding!
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BlueThemeColorScheme,
        typography = Typography,
        content = content
    )
}

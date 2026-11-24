package com.puretube.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF0033), // Pure YouTube Vibrant Red
    onPrimary = Color.White,
    primaryContainer = Color(0xFF330B0F),
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = Color(0xFF3EA6FF), // YouTube Accent Blue
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E2E42),
    onSecondaryContainer = Color(0xFFD2E4FF),
    tertiary = Color(0xFF2BA640), // YouTube Success Green
    background = Color(0xFF0F0F0F), // True YouTube Dark background
    surface = Color(0xFF0F0F0F),
    surfaceVariant = Color(0xFF272727), // YouTube Pill / Elevated Surface
    onBackground = Color(0xFFF1F1F1),
    onSurface = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF383838)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF0000), // YouTube Red
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF065FD4), // YouTube Blue
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3FD),
    onSecondaryContainer = Color(0xFF041E49),
    tertiary = Color(0xFF0F9D58),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F2F2),
    onBackground = Color(0xFF0F0F0F),
    onSurface = Color(0xFF0F0F0F),
    onSurfaceVariant = Color(0xFF606060),
    outline = Color(0xFFE5E5E5)
)

@Composable
fun PureTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false so YouTube branding is vivid, bright and professional
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

package com.example.ui.theme

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
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = GoogleBlueDark,
    onPrimaryContainer = GoogleBlueLight,
    secondary = GoogleGreen,
    onSecondary = Color.White,
    secondaryContainer = GoogleGreenDark,
    onSecondaryContainer = GoogleGreenLight,
    tertiary = GoogleYellow,
    onTertiary = Color.Black,
    tertiaryContainer = GoogleYellowDark,
    onTertiaryContainer = GoogleYellowLight,
    error = GoogleRed,
    onError = Color.White,
    background = GoogleBackgroundDark,
    onBackground = GoogleTextPrimaryDark,
    surface = GoogleSurfaceDark,
    onSurface = GoogleTextPrimaryDark,
    surfaceVariant = GoogleCardDark,
    onSurfaceVariant = GoogleTextSecondaryDark,
    outline = GoogleCardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = GoogleBlueLight,
    onPrimaryContainer = GoogleBlueDark,
    secondary = GoogleGreen,
    onSecondary = Color.White,
    secondaryContainer = GoogleGreenLight,
    onSecondaryContainer = GoogleGreenDark,
    tertiary = GoogleYellowDark,
    onTertiary = Color.White,
    tertiaryContainer = GoogleYellowLight,
    onTertiaryContainer = Color(0xFF5A3E00),
    error = GoogleRed,
    onError = Color.White,
    background = GoogleBackgroundLight,
    onBackground = GoogleTextPrimaryLight,
    surface = GoogleSurfaceLight,
    onSurface = GoogleTextPrimaryLight,
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = GoogleTextSecondaryLight,
    outline = GoogleCardBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep clean brand Google aesthetic by default
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
        typography = Typography,
        content = content
    )
}

@Composable
fun ImtiazLifestyleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

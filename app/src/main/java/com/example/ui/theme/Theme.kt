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

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = CleanPrimary,
    onPrimary = Color.White,
    primaryContainer = CleanPrimaryContainer,
    onPrimaryContainer = CleanOnPrimaryContainer,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = CleanBackground,
    surface = CleanSurface,
    onBackground = CleanOnBackground,
    onSurface = CleanOnBackground,
    surfaceVariant = CleanSurfaceVariant,
    onSurfaceVariant = CleanOnSurfaceVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Default to light Clean Minimalism theme
  dynamicColor: Boolean = false, // Guarantee the Clean Minimalism theme colors are used
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

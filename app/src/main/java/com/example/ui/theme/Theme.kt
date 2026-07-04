package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val CustomDarkColorScheme = darkColorScheme(
  primary = Indigo500,
  secondary = Emerald500,
  tertiary = Rose500,
  background = Slate900,
  surface = Slate800,
  surfaceVariant = Slate700,
  onPrimary = androidx.compose.ui.graphics.Color.White,
  onSecondary = androidx.compose.ui.graphics.Color.White,
  onBackground = Slate200,
  onSurface = androidx.compose.ui.graphics.Color.White,
  outline = Slate600
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode as specified by theme
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve design system
  content: @Composable () -> Unit,
) {
  val colorScheme = CustomDarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

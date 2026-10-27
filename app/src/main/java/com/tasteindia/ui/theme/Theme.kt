package com.tasteindia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Forest = Color(0xFF173F35)
val Saffron = Color(0xFFE79532)
val Ivory = Color(0xFFFAF7F0)
val Clay = Color(0xFF943D28)

private val Light = lightColorScheme(
    primary = Forest, onPrimary = Color.White, primaryContainer = Color(0xFFD0E9DE), onPrimaryContainer = Color(0xFF09271F),
    secondary = Saffron, onSecondary = Color(0xFF392100), secondaryContainer = Color(0xFFFFDDB1), onSecondaryContainer = Color(0xFF2D1900),
    tertiary = Clay, onTertiary = Color.White, background = Ivory, onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFFFCF7), onSurface = Color(0xFF1A1C19), surfaceVariant = Color(0xFFE7E3DB),
    outline = Color(0xFF777870), error = Color(0xFFBA1A1A)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA6D1BE), onPrimary = Color(0xFF0C372C), primaryContainer = Color(0xFF285548), onPrimaryContainer = Color(0xFFC1EDDA),
    secondary = Color(0xFFF4BD73), onSecondary = Color(0xFF432C07), secondaryContainer = Color(0xFF604316), onSecondaryContainer = Color(0xFFFFDDB1),
    tertiary = Color(0xFFFFB5A1), onTertiary = Color(0xFF5D190B), background = Color(0xFF111512), onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF171B18), onSurface = Color(0xFFE1E3DE), surfaceVariant = Color(0xFF414844), outline = Color(0xFF919A94)
)

@Composable
fun TasteIndiaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Typography(), content = content)
}

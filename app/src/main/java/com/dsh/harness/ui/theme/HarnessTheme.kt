package com.dsh.harness.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 主题模式（与 Web 端"跟随系统"对应）。 */
enum class ThemeMode { Light, Dark, System }

/**
 * 应用语义色 token。集中暴露给界面层，避免硬编码颜色。
 * 参考 Web 端浅色：偏白背景 + 蓝色品牌色 + 中性灰文本。
 */
data class HarnessColors(
    val brand: Color,
    val brandContainer: Color,
    val onBrand: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val purple: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val sidebar: Color,
    val sidebarBorder: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val outline: Color,
    val codeBlock: Color,
    val codeBlockText: Color,
    val toolEdit: Color,
    val toolRead: Color,
    val toolBash: Color,
    val toolThink: Color,
    val toolFail: Color
)

val LocalHarnessColors = staticCompositionLocalOf<HarnessColors> {
    error("HarnessColors not provided")
}

/** 浅色配色。 */
val LightHarnessColors = HarnessColors(
    brand = Color(0xFF4D6BFE),
    brandContainer = Color(0xFFE2E9FF),
    onBrand = Color(0xFFFFFFFF),
    accent = Color(0xFF7BD0FC),
    success = Color(0xFF22C55E),
    warning = Color(0xFFF59E0B),
    danger = Color(0xFFEF4444),
    purple = Color(0xFFA855F7),
    background = Color(0xFFF6F8FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F5FF),
    surfaceElevated = Color(0xFFFFFFFF),
    sidebar = Color(0xFFF5F7FF),
    sidebarBorder = Color(0xFFE5E7EB),
    primaryText = Color(0xFF0F172A),
    secondaryText = Color(0xFF475569),
    tertiaryText = Color(0xFF94A3B8),
    outline = Color(0xFFE2E8F0),
    codeBlock = Color(0xFFF1F5F9),
    codeBlockText = Color(0xFF0F172A),
    toolEdit = Color(0xFF4D6BFE),
    toolRead = Color(0xFF22C55E),
    toolBash = Color(0xFFF59E0B),
    toolThink = Color(0xFFA855F7),
    toolFail = Color(0xFFEF4444)
)

/** 深色配色。 */
val DarkHarnessColors = HarnessColors(
    brand = Color(0xFF6B85FF),
    brandContainer = Color(0xFF1F2540),
    onBrand = Color(0xFFFFFFFF),
    accent = Color(0xFF7BD0FC),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    purple = Color(0xFFC084FC),
    background = Color(0xFF0B0E1A),
    surface = Color(0xFF141A2B),
    surfaceVariant = Color(0xFF1C2233),
    surfaceElevated = Color(0xFF1E2536),
    sidebar = Color(0xFF0E1322),
    sidebarBorder = Color(0xFF22293C),
    primaryText = Color(0xFFF1F5F9),
    secondaryText = Color(0xFF94A3B8),
    tertiaryText = Color(0xFF64748B),
    outline = Color(0xFF22293C),
    codeBlock = Color(0xFF131A2C),
    codeBlockText = Color(0xFFE2E8F0),
    toolEdit = Color(0xFF6B85FF),
    toolRead = Color(0xFF4ADE80),
    toolBash = Color(0xFFFBBF24),
    toolThink = Color(0xFFC084FC),
    toolFail = Color(0xFFF87171)
)

@Composable
@ReadOnlyComposable
fun harnessColors(): HarnessColors = LocalHarnessColors.current

/** 字体类型。 */
val HarnessTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

/** 整套主题入口。 */
@Composable
fun HarnessTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colors = if (isDark) DarkHarnessColors else LightHarnessColors
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = colors.brand,
            onPrimary = colors.onBrand,
            primaryContainer = colors.brandContainer,
            onPrimaryContainer = colors.primaryText,
            secondary = colors.purple,
            background = colors.background,
            onBackground = colors.primaryText,
            surface = colors.surface,
            onSurface = colors.primaryText,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.secondaryText,
            outline = colors.outline,
            error = colors.danger
        )
    } else {
        lightColorScheme(
            primary = colors.brand,
            onPrimary = colors.onBrand,
            primaryContainer = colors.brandContainer,
            onPrimaryContainer = colors.primaryText,
            secondary = colors.purple,
            background = colors.background,
            onBackground = colors.primaryText,
            surface = colors.surface,
            onSurface = colors.primaryText,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.secondaryText,
            outline = colors.outline,
            error = colors.danger
        )
    }
    CompositionLocalProvider(LocalHarnessColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HarnessTypography,
            content = content
        )
    }
}

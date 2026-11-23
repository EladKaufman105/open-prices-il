package il.openprices.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import il.openprices.R

val Noto = FontFamily(
    Font(R.font.noto_hebrew_regular, FontWeight.Normal),
    Font(R.font.noto_hebrew_medium, FontWeight.Medium),
    Font(R.font.noto_hebrew_semibold, FontWeight.SemiBold),
    Font(R.font.noto_hebrew_bold, FontWeight.Bold),
)

private val Emerald = Color(0xFF0E9F5A)
private val EmeraldBright = Color(0xFF1EC773)

private val Light = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFAE6),
    onPrimaryContainer = Color(0xFF05603A),
    secondary = Color(0xFF475467),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF1F4),
    onSecondaryContainer = Color(0xFF1D2939),
    tertiary = Color(0xFFD92D20),
    tertiaryContainer = Color(0xFFFEE4E2),
    onTertiaryContainer = Color(0xFF912018),
    background = Color(0xFFF3F5F4),
    onBackground = Color(0xFF101828),
    surface = Color(0xFFF3F5F4),
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFF2F4F7),
    onSurfaceVariant = Color(0xFF667085),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF2F4F7),
    surfaceContainerHigh = Color(0xFFEAECF0),
    outline = Color(0xFFD0D5DD),
    outlineVariant = Color(0xFFEAECF0),
)

private val Dark = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = Color(0xFF002C17),
    primaryContainer = Color(0xFF0B3D26),
    onPrimaryContainer = Color(0xFFA6F4C5),
    secondary = Color(0xFFB4BCC8),
    onSecondary = Color(0xFF1D2939),
    secondaryContainer = Color(0xFF252B33),
    onSecondaryContainer = Color(0xFFE4E7EC),
    tertiary = Color(0xFFF97066),
    tertiaryContainer = Color(0xFF55160C),
    onTertiaryContainer = Color(0xFFFECDCA),
    background = Color(0xFF0E1211),
    onBackground = Color(0xFFE9ECEB),
    surface = Color(0xFF0E1211),
    onSurface = Color(0xFFE9ECEB),
    surfaceVariant = Color(0xFF1C2220),
    onSurfaceVariant = Color(0xFF98A2B3),
    surfaceContainerLowest = Color(0xFF181E1C),
    surfaceContainerLow = Color(0xFF1C2321),
    surfaceContainer = Color(0xFF222A27),
    surfaceContainerHigh = Color(0xFF2A322F),
    outline = Color(0xFF3A423F),
    outlineVariant = Color(0xFF2A312E),
)

private fun t(size: Int, weight: FontWeight, line: Int) =
    TextStyle(fontFamily = Noto, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private val AppTypography = Typography(
    displaySmall = t(32, FontWeight.SemiBold, 40),
    headlineMedium = t(26, FontWeight.SemiBold, 34),
    headlineSmall = t(22, FontWeight.SemiBold, 30),
    titleLarge = t(20, FontWeight.SemiBold, 27),
    titleMedium = t(16, FontWeight.Medium, 22),
    titleSmall = t(14, FontWeight.Medium, 20),
    bodyLarge = t(16, FontWeight.Normal, 22),
    bodyMedium = t(14, FontWeight.Normal, 20),
    bodySmall = t(12, FontWeight.Normal, 16),
    labelLarge = t(14, FontWeight.Medium, 20),
    labelMedium = t(12, FontWeight.Medium, 16),
    labelSmall = t(11, FontWeight.Medium, 14),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Accent used for "best price" highlights; slightly different in dark mode. */
data class Extra(val best: Color, val bestContainer: Color, val promo: Color, val promoContainer: Color)

val LocalExtra = staticCompositionLocalOf {
    Extra(Emerald, Color(0xFFDCFAE6), Color(0xFFD92D20), Color(0xFFFEE4E2))
}

@Composable
fun AppTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val extra = if (dark) Extra(Color(0xFF3CCB7F), Color(0xFF123323), Color(0xFFFDA29B), Color(0xFF2D1A18))
    else Extra(Color(0xFF067647), Color(0xFFDCFAE6), Color(0xFFB42318), Color(0xFFFEF3F2))
    CompositionLocalProvider(LocalExtra provides extra) {
        MaterialTheme(
            colorScheme = if (dark) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

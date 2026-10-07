package au.edu.jcu.assessment.utilityapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = OnMintGreen,
    primaryContainer = DarkGreenContainer,
    onPrimaryContainer = OnDarkGreenContainer,
    secondary = Color(0xFFB1CDBA),
    onSecondary = Color(0xFF20362A),
    secondaryContainer = Color(0xFF354D3E),
    onSecondaryContainer = Color(0xFFD3E8DA),
    tertiary = Color(0xFF9FCFCE),
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF1E5050),
    onTertiaryContainer = Color(0xFFBBEBEA),
    background = Color(0xFF101813),
    onBackground = Color(0xFFE1EBE3),
    surface = Color(0xFF101813),
    onSurface = Color(0xFFE1EBE3),
    surfaceVariant = Color(0xFF3D4940),
    onSurfaceVariant = Color(0xFFBFCBC1),
    surfaceContainerLowest = Color(0xFF0B120D),
    surfaceContainerLow = Color(0xFF19221B),
    surfaceContainer = Color(0xFF1D271F),
    surfaceContainerHigh = Color(0xFF28332A),
    surfaceContainerHighest = Color(0xFF333E35),
    outline = Color(0xFF89978C),
    outlineVariant = Color(0xFF3D4940)
)

private val LightColorScheme = lightColorScheme(
    primary = FootballGreen,
    onPrimary = OnFootballGreen,
    primaryContainer = LightGreenContainer,
    onPrimaryContainer = OnLightGreenContainer,
    secondary = Color(0xFF496456),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1EEE5),
    onSecondaryContainer = Color(0xFF243E2D),
    tertiary = Color(0xFF286A69),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC7EAE7),
    onTertiaryContainer = Color(0xFF153C3A),
    background = Color(0xFFF4F8F5),
    onBackground = Color(0xFF15231A),
    surface = Color(0xFFFAFCFA),
    onSurface = Color(0xFF15231A),
    surfaceVariant = Color(0xFFE1EAE3),
    onSurfaceVariant = Color(0xFF46564B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F6F1),
    surfaceContainer = Color(0xFFEBF2ED),
    surfaceContainerHigh = Color(0xFFE5EEE8),
    surfaceContainerHighest = Color(0xFFDDE8E0),
    outline = Color(0xFF718478),
    outlineVariant = Color(0xFFCDDAD0)
)

@Composable
fun MobileTech_UtilityAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep the app palette consistent instead of taking colors from the phone wallpaper.
    dynamicColor: Boolean = false,
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
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}

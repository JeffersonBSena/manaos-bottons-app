package br.com.armarinhomanaos.bottons.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = AppColor.Primary, onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE7F3), onPrimaryContainer = AppColor.Navy,
    secondary = AppColor.Orange, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3DF), onSecondaryContainer = AppColor.OrangeDark,
    background = Color(0xFFF2F2F7), onBackground = Color(0xFF1C2733),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1C2733),
    surfaceVariant = Color(0xFFF3F4F6), onSurfaceVariant = Color(0xFF5A6875),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7F7FA), surfaceContainerHigh = Color(0xFFEDEFF2),
    outline = Color(0xFFD8DEE6), outlineVariant = Color(0xFFE4E6EA),
    error = AppColor.Red, onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = AppColor.Primary, onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1F2E), onPrimaryContainer = Color(0xFFF9A8D4),
    secondary = AppColor.Orange, onSecondary = Color.White,
    secondaryContainer = Color(0xFF3E2E18), onSecondaryContainer = Color(0xFFFDE68A),
    background = Color(0xFF000000), onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E), onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E), onSurfaceVariant = Color(0xFFAEAEB2),
    surfaceContainer = Color(0xFF1C1C1E), surfaceContainerLow = Color(0xFF141416), surfaceContainerHigh = Color(0xFF2C2C2E),
    outline = Color(0xFF48484A), outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF6B62), onError = Color.White,
)

@Composable
fun BottonsTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    remember { AparenciaStore.iniciar(ctx) }
    val sistemaEscuro = isSystemInDarkTheme()
    val darkTheme = when (AparenciaStore.atual) {
        Aparencia.SISTEMA -> sistemaEscuro
        Aparencia.CLARO -> false
        Aparencia.ESCURO -> true
    }
    if (AppColor.escuro != darkTheme) AppColor.escuro = darkTheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { w ->
                WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(w, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = AppTypography, content = content)
}

package kz.cranels.expensetracker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary600,
    onPrimary = White,
    primaryContainer = Primary50,
    onPrimaryContainer = Primary700,
    background = Gray50, // Using a light gray for the main background
    onBackground = Gray900,
    surface = Gray50, // Set surface to the same light gray for a unified look
    onSurface = Gray900,
    surfaceVariant = White, // Use white for cards, text fields, etc.
    onSurfaceVariant = Gray700,
    outline = Gray300
)

@Composable
fun ExpenseTrackerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

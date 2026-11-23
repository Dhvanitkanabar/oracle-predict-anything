package com.example.oraclepredictanything.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────────────────
//  COLOR SCHEMES
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary                = primaryDark,
    onPrimary              = onPrimaryDark,
    primaryContainer       = primaryContainerDark,
    onPrimaryContainer     = onPrimaryContainerDark,

    secondary              = secondaryDark,
    onSecondary            = onSecondaryDark,
    secondaryContainer     = secondaryContainerDark,
    onSecondaryContainer   = onSecondaryContainerDark,

    tertiary               = tertiaryDark,
    onTertiary             = onTertiaryDark,
    tertiaryContainer      = tertiaryContainerDark,
    onTertiaryContainer    = onTertiaryContainerDark,

    error                  = errorDark,
    onError                = onErrorDark,
    errorContainer         = errorContainerDark,
    onErrorContainer       = onErrorContainerDark,

    background             = backgroundDark,
    onBackground           = onBackgroundDark,

    surface                = surfaceDark,
    onSurface              = onSurfaceDark,
    surfaceVariant         = surfaceVariantDark,
    onSurfaceVariant       = onSurfaceVariantDark,

    surfaceTint            = surfaceTintDark,
    inverseSurface         = inverseSurfaceDark,
    inverseOnSurface       = inverseOnSurfaceDark,
    inversePrimary         = inversePrimaryDark,

    outline                = outlineDark,
    outlineVariant         = outlineVariantDark,

    scrim                  = scrimDark,

    surfaceBright          = surfaceBrightDark,
    surfaceDim             = surfaceDimDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow    = surfaceContainerLowDark,
    surfaceContainer       = surfaceContainerDark,
    surfaceContainerHigh   = surfaceContainerHighDark,
    surfaceContainerHighest= surfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary                = primaryLight,
    onPrimary              = onPrimaryLight,
    primaryContainer       = primaryContainerLight,
    onPrimaryContainer     = onPrimaryContainerLight,

    secondary              = secondaryLight,
    onSecondary            = onSecondaryLight,
    secondaryContainer     = secondaryContainerLight,
    onSecondaryContainer   = onSecondaryContainerLight,

    tertiary               = tertiaryLight,
    onTertiary             = onTertiaryLight,
    tertiaryContainer      = tertiaryContainerLight,
    onTertiaryContainer    = onTertiaryContainerLight,

    error                  = errorLight,
    onError                = onErrorLight,
    errorContainer         = errorContainerLight,
    onErrorContainer       = onErrorContainerLight,

    background             = backgroundLight,
    onBackground           = onBackgroundLight,

    surface                = surfaceLight,
    onSurface              = onSurfaceLight,
    surfaceVariant         = surfaceVariantLight,
    onSurfaceVariant       = onSurfaceVariantLight,

    surfaceTint            = surfaceTintLight,
    inverseSurface         = inverseSurfaceLight,
    inverseOnSurface       = inverseOnSurfaceLight,
    inversePrimary         = inversePrimaryLight,

    outline                = outlineLight,
    outlineVariant         = outlineVariantLight,

    scrim                  = scrimLight,

    surfaceBright          = surfaceBrightLight,
    surfaceDim             = surfaceDimLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow    = surfaceContainerLowLight,
    surfaceContainer       = surfaceContainerLight,
    surfaceContainerHigh   = surfaceContainerHighLight,
    surfaceContainerHighest= surfaceContainerHighestLight
)

// ─────────────────────────────────────────────────────────────────────────────
//  THEME COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Root theme for Oracle: Predict Anything.
 *
 * @param darkTheme      Whether to apply the dark colour scheme.
 *                       Defaults to the system setting; dark mode is the hero.
 * @param dynamicColor   Always [false] — dynamic colour is intentionally
 *                       disabled so the brand palette is preserved on all
 *                       devices, including Android 12+.
 * @param content        The composable subtree to theme.
 */
@Composable
fun OraclePredictAnythingTheme(
    darkTheme: Boolean    = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,          // kept for API compatibility; always false
    content: @Composable () -> Unit
) {
    // Dynamic colour is intentionally suppressed — brand palette takes priority.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Synchronise the status-bar colour with the chosen scheme's background.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = Shapes,
        content     = content
    )
}

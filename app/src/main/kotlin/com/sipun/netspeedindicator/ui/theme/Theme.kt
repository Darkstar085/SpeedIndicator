package com.sipun.netspeedindicator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.sipun.netspeedindicator.R
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme

private val LocalAppDialogColorScheme = compositionLocalOf<ColorScheme> { lightColorScheme() }

private data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

/**
 * Static accent palettes, index 0 = Blue (the default look) through Rose.
 * Accent 0 in preferences means dynamic (wallpaper) colors instead.
 */
private val lightAccents = listOf(
    AccentPalette(Color(0xFF2563EB), Color(0xFFFFFFFF), Color(0xFFDBEAFE), Color(0xFF1E3A8A)),
    AccentPalette(Color(0xFF059669), Color(0xFFFFFFFF), Color(0xFFD1FAE5), Color(0xFF064E3B)),
    AccentPalette(Color(0xFF7C3AED), Color(0xFFFFFFFF), Color(0xFFEDE9FE), Color(0xFF4C1D95)),
    AccentPalette(Color(0xFFD97706), Color(0xFFFFFFFF), Color(0xFFFEF3C7), Color(0xFF78350F)),
    AccentPalette(Color(0xFFE11D48), Color(0xFFFFFFFF), Color(0xFFFFE4E6), Color(0xFF881337))
)

private val darkAccents = listOf(
    AccentPalette(Color(0xFF3B82F6), Color(0xFFFFFFFF), Color(0xFF1E3A8A), Color(0xFFDBEAFE)),
    AccentPalette(Color(0xFF34D399), Color(0xFF04392B), Color(0xFF064E3B), Color(0xFFA7F3D0)),
    AccentPalette(Color(0xFFA78BFA), Color(0xFF2E1065), Color(0xFF4C1D95), Color(0xFFDDD6FE)),
    AccentPalette(Color(0xFFFBBF24), Color(0xFF451A03), Color(0xFF78350F), Color(0xFFFDE68A)),
    AccentPalette(Color(0xFFFB7185), Color(0xFF4C0519), Color(0xFF881337), Color(0xFFFFE4E6))
)

@Composable
fun AppDialogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LocalAppDialogColorScheme.current,
        typography = Typography,
        content = content
    )
}

@Composable
fun NetSpeedIndicatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    accent: Int = 0,
    content: @Composable () -> Unit
) {
    val accentPalette = (if (darkTheme || pureBlack) darkAccents else lightAccents)
        .getOrNull(accent - 1)

    val staticColorScheme = when {
        pureBlack -> darkColorScheme(
            primary = accentPalette?.primary ?: colorResource(R.color.primary_dark),
            onPrimary = accentPalette?.onPrimary ?: colorResource(R.color.on_primary_dark),
            primaryContainer = Color(0xFF101010),
            onPrimaryContainer = accentPalette?.onPrimaryContainer
                ?: colorResource(R.color.on_primary_container_dark),
            secondary = colorResource(R.color.secondary_dark),
            onSecondary = colorResource(R.color.on_secondary_dark),
            secondaryContainer = Color(0xFF121212),
            onSecondaryContainer = Color.White,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF121212),
            onSurfaceVariant = colorResource(R.color.on_surface_variant_dark),
            outline = Color(0xFF2A2A2A),
            outlineVariant = Color(0xFF1A1A1A)
        )

        darkTheme -> darkColorScheme(
            primary = accentPalette?.primary ?: colorResource(R.color.primary_dark),
            onPrimary = accentPalette?.onPrimary ?: colorResource(R.color.on_primary_dark),
            primaryContainer = accentPalette?.primaryContainer
                ?: colorResource(R.color.primary_container_dark),
            onPrimaryContainer = accentPalette?.onPrimaryContainer
                ?: colorResource(R.color.on_primary_container_dark),
            secondary = colorResource(R.color.secondary_dark),
            onSecondary = colorResource(R.color.on_secondary_dark),
            secondaryContainer = colorResource(R.color.secondary_container_dark),
            onSecondaryContainer = colorResource(R.color.on_secondary_container_dark),
            background = colorResource(R.color.background_dark),
            onBackground = colorResource(R.color.on_background_dark),
            surface = colorResource(R.color.surface_dark),
            onSurface = colorResource(R.color.on_surface_dark),
            surfaceVariant = colorResource(R.color.surface_variant_dark),
            onSurfaceVariant = colorResource(R.color.on_surface_variant_dark),
            outline = colorResource(R.color.outline_dark),
            outlineVariant = colorResource(R.color.outline_variant_dark)
        )

        else -> lightColorScheme(
            primary = accentPalette?.primary ?: colorResource(R.color.primary_light),
            onPrimary = accentPalette?.onPrimary ?: colorResource(R.color.on_primary_light),
            primaryContainer = accentPalette?.primaryContainer
                ?: colorResource(R.color.primary_container_light),
            onPrimaryContainer = accentPalette?.onPrimaryContainer
                ?: colorResource(R.color.on_primary_container_light),
            secondary = colorResource(R.color.secondary_light),
            onSecondary = colorResource(R.color.on_secondary_light),
            secondaryContainer = colorResource(R.color.secondary_container_light),
            onSecondaryContainer = colorResource(R.color.on_secondary_container_light),
            background = colorResource(R.color.background_light),
            onBackground = colorResource(R.color.on_background_light),
            surface = colorResource(R.color.surface_light),
            onSurface = colorResource(R.color.on_surface_light),
            surfaceVariant = colorResource(R.color.surface_variant_light),
            onSurfaceVariant = colorResource(R.color.on_surface_variant_light),
            outline = colorResource(R.color.outline_light),
            outlineVariant = colorResource(R.color.outline_variant_light)
        )
    }

    val colorScheme = when {
        dynamicColor && accent == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val dynamicScheme =
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (pureBlack) dynamicScheme.copy(
                background = Color.Black,
                onBackground = Color.White,
                surface = Color.Black,
                onSurface = Color.White,
                surfaceVariant = Color(0xFF121212)
            ) else dynamicScheme
        }

        else -> staticColorScheme
    }

    val dimensions = rememberAppDimensions()
    CompositionLocalProvider(
        LocalAppDialogColorScheme provides if (pureBlack && dynamicColor && accent == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) colorScheme else staticColorScheme,
        LocalDimensions provides dimensions,
        LocalGlassDarkTheme provides (darkTheme || pureBlack)
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !darkTheme && !pureBlack
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !darkTheme && !pureBlack
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
}

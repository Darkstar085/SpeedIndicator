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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.data.preferences.AccentGradient
import com.sipun.netspeedindicator.data.preferences.AccentStyle
import io.github.itshedimisawi.vitro.LocalGlassDarkTheme

private val LocalAppDialogColorScheme = compositionLocalOf<ColorScheme> { lightColorScheme() }

val LocalAccentContainerBrush = compositionLocalOf {
    Brush.horizontalGradient(listOf(Color(0xFF42A5F5).copy(alpha = 0.18f), Color(0xFF42A5F5).copy(alpha = 0.18f)))
}

val LocalAccentBrush = compositionLocalOf {
    Brush.horizontalGradient(listOf(Color(0xFF42A5F5), Color(0xFF42A5F5)))
}

internal fun accentGradientColors(gradient: AccentGradient): List<Color> = when (gradient) {
    AccentGradient.SUNSET -> listOf(Color(0xFF667EEA), Color(0xFF764BA2))
    AccentGradient.OCEAN -> listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
    AccentGradient.AURORA -> listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
    AccentGradient.PURPLE_HAZE -> listOf(Color(0xFF8E44AD), Color(0xFFF8B500))
    AccentGradient.PEACH -> listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B))
    AccentGradient.EMERALD -> listOf(Color(0xFF11998E), Color(0xFF38EF7D))
    AccentGradient.CHERRY -> listOf(Color(0xFFFF2D55), Color(0xFFFFA600))
    AccentGradient.ROYAL -> listOf(Color(0xFF56CCF2), Color(0xFF2F80ED))
    AccentGradient.AMBER -> listOf(Color(0xFFFF6EC7), Color(0xFFFF9A44))
    AccentGradient.MINT -> listOf(Color(0xFF0F2027), Color(0xFF203A43))
    AccentGradient.ROSE -> listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB))
    AccentGradient.INDIGO -> listOf(Color(0xFFFF9966), Color(0xFFFF5E62))
    AccentGradient.CUSTOM -> listOf(Color(0xFFE92DE2), Color(0xFF4A00E0))
}

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
    dynamicColor: Boolean = false,
    pureBlack: Boolean = false,
    customColor: Int = 0xFF42A5F5.toInt(),
    accentStyle: AccentStyle = AccentStyle.SOLID,
    accentGradient: AccentGradient = AccentGradient.SUNSET,
    customGradientStart: Int = 0xFFE92DE2.toInt(),
    customGradientEnd: Int = 0xFF4A00E0.toInt(),
    content: @Composable () -> Unit
) {
    val gradientColors = if (accentGradient == AccentGradient.CUSTOM) {
        listOf(Color(customGradientStart), Color(customGradientEnd))
    } else {
        accentGradientColors(accentGradient)
    }
    val dynamicScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null
    val accentColor = when {
        dynamicScheme != null -> dynamicScheme.primary
        accentStyle == AccentStyle.GRADIENT -> gradientColors.first()
        else -> Color(customColor)
    }

    val staticColorScheme = when {
        pureBlack -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF101010),
            onPrimaryContainer = accentColor,
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
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.35f),
            onPrimaryContainer = accentColor,
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
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.12f),
            onPrimaryContainer = accentColor,
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

    val colorScheme = dynamicScheme?.let { scheme ->
        if (pureBlack) scheme.copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF121212)
        ) else scheme
    } ?: staticColorScheme
    val accentContainerBrush = when {
        dynamicScheme != null -> Brush.horizontalGradient(
            listOf(dynamicScheme.primary.copy(alpha = 0.18f), dynamicScheme.tertiary.copy(alpha = 0.18f))
        )
        accentStyle == AccentStyle.GRADIENT -> Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.18f) })
        else -> Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.18f), accentColor.copy(alpha = 0.18f)))
    }
    val accentBrush = when {
        dynamicScheme != null -> Brush.horizontalGradient(
            listOf(dynamicScheme.primary, dynamicScheme.tertiary)
        )
        accentStyle == AccentStyle.GRADIENT -> Brush.horizontalGradient(gradientColors)
        else -> Brush.horizontalGradient(listOf(accentColor, accentColor))
    }

    val dimensions = rememberAppDimensions()
    CompositionLocalProvider(
        LocalAccentBrush provides accentBrush,
        LocalAccentContainerBrush provides accentContainerBrush,
        LocalAppDialogColorScheme provides colorScheme,
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

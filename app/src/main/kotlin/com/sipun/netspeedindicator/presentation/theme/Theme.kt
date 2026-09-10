package com.sipun.netspeedindicator.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.sipun.netspeedindicator.R

@Composable
fun NetSpeedIndicatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val staticColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colorResource(R.color.primary_dark),
            onPrimary = colorResource(R.color.on_primary_dark),
            primaryContainer = colorResource(R.color.primary_container_dark),
            onPrimaryContainer = colorResource(R.color.on_primary_container_dark),
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
    } else {
        lightColorScheme(
            primary = colorResource(R.color.primary_light),
            onPrimary = colorResource(R.color.on_primary_light),
            primaryContainer = colorResource(R.color.primary_container_light),
            onPrimaryContainer = colorResource(R.color.on_primary_container_light),
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
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> staticColorScheme
    }

    val dimensions = rememberAppDimensions()

    CompositionLocalProvider(
        LocalDimensions provides dimensions
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

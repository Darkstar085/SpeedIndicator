package com.sipun.netspeedindicator.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.ui.navigation.MainRoute
import com.sipun.netspeedindicator.ui.theme.LocalAccentContainerBrush
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect

@Composable
fun AppBottomNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    backdrop: GlassBackdrop? = null
) {
    val items = listOf(
        AppBottomNavItem.Home,
        AppBottomNavItem.History,
        AppBottomNavItem.AppDataUsage,
        AppBottomNavItem.Settings
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val glassConfig = if (isDark) {
        Glass.Regular.copy(
            blurRadius = 26.dp,
            refractionHeight = 18.dp,
            refractionAmount = 34.dp,
            brightness = 0.16f,
            highlightIntensity = 1.55f
        ).interactive()
    } else {
        Glass.Regular.copy(
            blurRadius = 26.dp,
            refractionHeight = 18.dp,
            refractionAmount = 30.dp,
            brightness = 0.30f,
            highlightIntensity = 1.45f
        ).interactive()
    }

    val pillColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.40f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
    }

    val borderStroke = if (isDark) {
        BorderStroke(
            width = 1.3.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f)
                )
            )
        )
    } else {
        BorderStroke(
            width = 1.3.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.98f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                )
            )
        )
    }

    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .offset(y = 1.dp)
                .then(
                    if (backdrop != null) {
                        Modifier.glassEffect(
                            backdrop = backdrop,
                            glass = glassConfig,
                            shape = RoundedCornerShape(34.dp)
                        )
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(34.dp),
            color = pillColor,
            tonalElevation = 0.dp,
            border = borderStroke
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentDestination?.hasRoute(item.route::class) == true
                    AppBottomNavigationItem(
                        modifier = Modifier.weight(1f),
                        item = item,
                        isSelected = isSelected
                    ) {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationItem(
    modifier: Modifier = Modifier,
    item: AppBottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale_animation"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val color = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDark) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        }
    }

    val indicatorModifier = if (isSelected) {
        val indicatorBorderBrush = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
            )
        }
        Modifier
            .background(LocalAccentContainerBrush.current, RoundedCornerShape(28.dp))
            .border(1.dp, indicatorBorderBrush, RoundedCornerShape(28.dp))
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .then(indicatorModifier)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = interactionSource,
                    onClick = onClick
                )
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = stringResource(item.label),
                tint = color,
                modifier = Modifier.size(25.dp)
            )
            Text(
                text = stringResource(item.label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = color,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

sealed class AppBottomNavItem(
    val label: Int,
    val route: MainRoute,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home :
        AppBottomNavItem(R.string.home, MainRoute.Home, Icons.Filled.Home, Icons.Outlined.Home)

    object History : AppBottomNavItem(
        R.string.history,
        MainRoute.History,
        Icons.Filled.History,
        Icons.Outlined.History
    )

    object AppDataUsage : AppBottomNavItem(
        R.string.app_data_usage,
        MainRoute.AppDataUsage,
        Icons.Filled.BarChart,
        Icons.Outlined.BarChart
    )

    object Settings : AppBottomNavItem(
        R.string.settings,
        MainRoute.Settings,
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    )
}

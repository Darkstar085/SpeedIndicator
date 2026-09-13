package com.sipun.netspeedindicator.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.ui.navigation.ScreenRoute

@Composable
fun AppBottomNavigation(navController: NavHostController, containerColor: Color = MaterialTheme.colorScheme.surface) {
    val items = listOf(AppBottomNavItem.Home, AppBottomNavItem.History, AppBottomNavItem.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    Box(Modifier.fillMaxWidth().background(containerColor).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)).align(Alignment.TopCenter))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route::class.qualifiedName
                AppBottomNavigationItem(item = item, isSelected = isSelected) {
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

@Composable
fun AppBottomNavigationItem(modifier: Modifier = Modifier, item: AppBottomNavItem, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.01f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "scale_animation")
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick).padding(top = 3.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(Modifier.size(width = 52.dp, height = 30.dp).clip(RoundedCornerShape(15.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent), contentAlignment = Alignment.Center) {
            Icon(imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon, contentDescription = stringResource(item.label), tint = color, modifier = Modifier.size(23.dp))
        }
        Text(text = stringResource(item.label), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = color, style = MaterialTheme.typography.bodySmall)
    }
}

sealed class AppBottomNavItem(val label: Int, val route: ScreenRoute, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : AppBottomNavItem(R.string.home, ScreenRoute.Home, Icons.Filled.Home, Icons.Outlined.Home)
    object History : AppBottomNavItem(R.string.history, ScreenRoute.History, Icons.Filled.History, Icons.Outlined.History)
    object Settings : AppBottomNavItem(R.string.settings, ScreenRoute.Settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}
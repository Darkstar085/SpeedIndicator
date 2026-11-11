package com.sipun.netspeedindicator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

private val AppCardShape = RoundedCornerShape(22.dp)

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f

    val cardFill = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )
    }

    val cardBorder = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.90f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    }

    Column(
        modifier
            .background(cardFill, shape = AppCardShape)
            .border(width = 1.dp, brush = cardBorder, shape = AppCardShape),
        content = content
    )
}

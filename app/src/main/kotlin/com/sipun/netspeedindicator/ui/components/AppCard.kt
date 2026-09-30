package com.sipun.netspeedindicator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

private val AppCardShape = RoundedCornerShape(22.dp)

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.075f),
                AppCardShape
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
                AppCardShape
            ),
        content = content
    )
}

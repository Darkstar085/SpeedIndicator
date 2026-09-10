package com.sipun.netspeedindicator.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sipun.netspeedindicator.presentation.theme.dimens

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    title: String = "Dashboard",
    subTitle: String = "Real-time Monitor",
    showTrailingIcon: Boolean = false,
    trailingIcon: ImageVector = Icons.Default.PowerSettingsNew,
    onTrailingIconClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimens.horizontalPadding,
                vertical = dimens.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium
                    .copy(color = MaterialTheme.colorScheme.onBackground)
            )
            Text(
                text = subTitle,
                style = MaterialTheme.typography.bodyMedium
                    .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        if (showTrailingIcon) {
            Icon(
                modifier = Modifier
                    .size(dimens.vectorImageSize)
                    .clip(CircleShape)
                    .clickable { onTrailingIconClick.invoke() }
                    .padding(2.dp),
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
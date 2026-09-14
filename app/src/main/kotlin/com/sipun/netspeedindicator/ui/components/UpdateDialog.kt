package com.sipun.netspeedindicator.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.update.AppUpdate

@Composable
fun UpdateDialog(
    update: AppUpdate,
    appIcon: ImageBitmap,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                bitmap = appIcon,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(64.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.update_available),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.update_version_available, update.version),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        UpdateInfoRow(Icons.Default.NewReleases, stringResource(R.string.version), update.version)
                        UpdateInfoRow(Icons.Default.Inventory2, stringResource(R.string.download_size), formatSize(update.size))
                    }
                }
                if (update.notes.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.whats_new), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        update.notes.lineSequence().take(3).forEach { note ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownload,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.size(7.dp))
                Text(stringResource(R.string.download_update), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(R.string.later), fontWeight = FontWeight.Medium)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    )
}

@Composable
private fun UpdateInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes < 1024L * 1024L -> "%.0f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}

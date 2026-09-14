package com.sipun.netspeedindicator.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    onDownload: () -> Unit,
    onInstall: () -> Unit = {},
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                bitmap = appIcon,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(58.dp)
            )
        },
        title = {
            Text(
                text = stringResource(if (isDownloaded) R.string.update_downloaded else R.string.update_available),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Column(
                        Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        UpdateInfoRow(Icons.Default.NewReleases, stringResource(R.string.version), update.version)
                        UpdateInfoRow(Icons.Default.Inventory2, stringResource(R.string.download_size), formatSize(update.size))
                        update.releaseDate?.let { date ->
                            UpdateInfoRow(Icons.Default.NewReleases, stringResource(R.string.released), date)
                        }
                    }
                }
                if (update.notes.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(stringResource(R.string.whats_new), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 128.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            update.notes.lineSequence().filter { it.isNotBlank() }.forEach { note ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(
                                        note.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.9f).height(42.dp),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text(stringResource(R.string.later), fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = if (isDownloaded) onInstall else onDownload,
                    enabled = !isDownloading,
                    modifier = Modifier.weight(1.25f).height(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        stringResource(
                            when {
                                isDownloaded -> R.string.install_update
                                isDownloading -> R.string.downloading_update
                                else -> R.string.download_update
                            }
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    )
}

@Composable
private fun UpdateInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes < 1024L * 1024L -> "%.0f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}

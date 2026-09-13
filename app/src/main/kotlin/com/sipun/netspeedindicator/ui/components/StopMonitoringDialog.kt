package com.sipun.netspeedindicator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import com.sipun.netspeedindicator.ui.theme.OutfitFontFamily

@Composable
fun StopMonitoringDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = modifier.fillMaxWidth(0.9f).wrapContentHeight(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(id = R.drawable.ic_notifications_off), null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.stop_monitoring), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.stop_monitoring_desc), style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp) }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1.2f).height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), contentColor = MaterialTheme.colorScheme.onSurface)) { Text(stringResource(R.string.stop_service), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun StopMonitoringDialogPreview() {
    NetSpeedIndicatorTheme(darkTheme = true) { StopMonitoringDialog(onDismiss = {}, onConfirm = {}) }
}
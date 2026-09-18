package com.aisupercleaner.ultimate.presentation.permissions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.permissions.PermissionStatus
import com.aisupercleaner.ultimate.core.permissions.PermissionType

@Composable
fun PermissionCard(
    type: PermissionType,
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val (statusColor, statusText, statusIcon) = when (status) {
        PermissionStatus.GRANTED -> Triple(Color(0xFF2E7D32), stringResource(R.string.perm_granted), Icons.Rounded.CheckCircle)
        PermissionStatus.DENIED -> Triple(colors.error, stringResource(R.string.perm_denied), Icons.Rounded.ErrorOutline)
        PermissionStatus.PERMANENTLY_DENIED -> Triple(colors.error, stringResource(R.string.perm_blocked), Icons.Rounded.Block)
        PermissionStatus.NOT_REQUESTED -> Triple(colors.primary, stringResource(R.string.perm_needed), Icons.Rounded.Info)
        PermissionStatus.NOT_APPLICABLE -> Triple(colors.onSurfaceVariant, stringResource(R.string.perm_not_applicable), Icons.Rounded.RemoveCircleOutline)
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(type.icon(), contentDescription = null, tint = colors.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(type.titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (type.isCritical) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = colors.errorContainer, shape = CircleShape) {
                            Text(stringResource(R.string.perm_required), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = colors.onErrorContainer)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(type.descriptionRes), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.width(8.dp))
            when (status) {
                PermissionStatus.GRANTED, PermissionStatus.NOT_APPLICABLE -> Unit
                PermissionStatus.PERMANENTLY_DENIED -> FilledTonalButton(onClick = onOpenSettings, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text(stringResource(R.string.perm_open_settings))
                }
                else -> Button(onClick = onRequest, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Text(stringResource(R.string.perm_grant))
                }
            }
        }
    }
}

private fun PermissionType.icon(): ImageVector = when (this) {
    PermissionType.STORAGE -> Icons.Rounded.FolderOpen
    PermissionType.ALL_FILES -> Icons.Rounded.AdminPanelSettings
    PermissionType.NOTIFICATIONS -> Icons.Rounded.Notifications
    PermissionType.BIOMETRIC -> Icons.Rounded.Fingerprint
}

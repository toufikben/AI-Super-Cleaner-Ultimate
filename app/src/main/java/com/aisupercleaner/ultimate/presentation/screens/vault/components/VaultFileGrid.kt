package com.aisupercleaner.ultimate.presentation.screens.vault.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.data.vault.VaultFile

@Composable
fun VaultFileGrid(
    files: List<VaultFile>,
    onFileClick: (VaultFile) -> Unit,
    onFileLongClick: (VaultFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(files, key = { it.id }) { file ->
            VaultFileTile(file = file, onClick = { onFileClick(file) }, onLongClick = { onFileLongClick(file) })
        }
    }
}

@Composable
private fun VaultFileTile(file: VaultFile, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().weight(1f).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when {
                    file.isImage -> Icons.Rounded.Image
                    file.isVideo -> Icons.Rounded.Videocam
                    else -> Icons.Rounded.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(6.dp)) {
            Column {
                Text(file.originalName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(file.displaySize, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

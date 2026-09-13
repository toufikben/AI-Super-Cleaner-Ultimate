package com.aisupercleaner.ultimate.presentation.screens.junk.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter

@Composable
fun ConfirmCleanupDialog(count: Int, bytes: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_cleanup_title)) },
        text = { Text(stringResource(R.string.confirm_cleanup_message, count, Formatter.formatBytes(bytes))) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

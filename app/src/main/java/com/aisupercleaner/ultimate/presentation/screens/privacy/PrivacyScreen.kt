package com.aisupercleaner.ultimate.presentation.screens.privacy

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.data.privacy.AppPrivacyInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit = {}, viewModel: PrivacyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = viewModel::scan) { Icon(Icons.Rounded.Refresh, contentDescription = null) } },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.highRiskCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                ),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (state.highRiskCount > 0) Icons.Rounded.Warning else Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = if (state.highRiskCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (state.highRiskCount > 0) stringResource(R.string.privacy_alert, state.highRiskCount) else stringResource(R.string.privacy_all_clean),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(R.string.privacy_apps_count, state.apps.size), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                PrivacyUiState.Filter.entries.forEachIndexed { idx, f ->
                    SegmentedButton(
                        selected = f == state.filter,
                        onClick = { viewModel.setFilter(f) },
                        shape = SegmentedButtonDefaults.itemShape(idx, PrivacyUiState.Filter.entries.size),
                        label = {
                            Text(when (f) {
                                PrivacyUiState.Filter.ALL -> stringResource(R.string.privacy_filter_all)
                                PrivacyUiState.Filter.HIGH -> stringResource(R.string.privacy_filter_high)
                                PrivacyUiState.Filter.MEDIUM_PLUS -> stringResource(R.string.privacy_filter_medium)
                            })
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.filtered, key = { it.packageName }) { app ->
                    AppPrivacyCard(app) {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${app.packageName}")
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPrivacyCard(app: AppPrivacyInfo, onClick: () -> Unit) {
    val riskColor = when (app.riskLevel) {
        AppPrivacyInfo.RiskLevel.HIGH -> Color(0xFFC62828)
        AppPrivacyInfo.RiskLevel.MEDIUM -> Color(0xFFE65100)
        AppPrivacyInfo.RiskLevel.LOW -> Color(0xFF1565C0)
        AppPrivacyInfo.RiskLevel.SAFE -> Color(0xFF2E7D32)
    }
    val riskText = when (app.riskLevel) {
        AppPrivacyInfo.RiskLevel.HIGH -> stringResource(R.string.risk_high)
        AppPrivacyInfo.RiskLevel.MEDIUM -> stringResource(R.string.risk_medium)
        AppPrivacyInfo.RiskLevel.LOW -> stringResource(R.string.risk_low)
        AppPrivacyInfo.RiskLevel.SAFE -> stringResource(R.string.risk_safe)
    }

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                app.icon?.let { drawable ->
                    runCatching {
                        Image(
                            bitmap = drawable.toBitmap(96, 96).asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(CircleShape),
                        )
                    }
                } ?: Icon(Icons.Rounded.Apps, contentDescription = null)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (app.dangerousPermissions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.privacy_perms_count, app.dangerousPermissions.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(color = riskColor.copy(alpha = 0.15f), shape = CircleShape) {
                Text(riskText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = riskColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

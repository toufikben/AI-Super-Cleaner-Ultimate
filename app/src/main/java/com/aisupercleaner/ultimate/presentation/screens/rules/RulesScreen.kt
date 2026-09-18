package com.aisupercleaner.ultimate.presentation.screens.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter
import com.aisupercleaner.ultimate.data.rules.CleanupRule
import com.aisupercleaner.ultimate.data.rules.RuleEvaluator
import com.aisupercleaner.ultimate.presentation.screens.rules.components.RuleEditorScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(onBack: () -> Unit = {}, viewModel: RulesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.phase == RulesUiState.Phase.EDITOR) {
        state.editingRule?.let { rule ->
            RuleEditorScreen(initial = rule, onSave = viewModel::saveRule, onCancel = viewModel::cancelEdit)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rules_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
                actions = {
                    if (state.hasRules && state.phase == RulesUiState.Phase.LIST) {
                        IconButton(onClick = viewModel::evaluate) { Icon(Icons.Rounded.PlayArrow, contentDescription = null) }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.phase == RulesUiState.Phase.LIST) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::startNewRule,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.rules_new)) },
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.phase) {
                RulesUiState.Phase.LIST -> RuleListView(state.rules, viewModel::toggleRule, viewModel::startEdit, viewModel::deleteRule)
                RulesUiState.Phase.EVALUATING -> EvaluatingView(state.progress, state.progressPath)
                RulesUiState.Phase.REPORT -> ReportView(state.report, viewModel::backToList, viewModel::applyAction)
                RulesUiState.Phase.ERROR -> ErrorView(state.errorMessage ?: "", viewModel::dismissError)
                else -> Unit
            }
        }
    }
}

@Composable
private fun RuleListView(rules: List<CleanupRule>, onToggle: (String, Boolean) -> Unit, onEdit: (CleanupRule) -> Unit, onDelete: (String) -> Unit) {
    if (rules.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Rule, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.rules_empty), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.rules_empty_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(rules, key = { it.id }) { rule -> RuleCard(rule, onToggle, onEdit, onDelete) }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun RuleCard(rule: CleanupRule, onToggle: (String, Boolean) -> Unit, onEdit: (CleanupRule) -> Unit, onDelete: (String) -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(rule.name.ifBlank { stringResource(R.string.rules_unnamed) }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (rule.description.isNotBlank()) Text(rule.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = rule.enabled, onCheckedChange = { onToggle(rule.id, it) })
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.rules_logic, if (rule.logic == CleanupRule.Logic.AND) stringResource(R.string.rules_logic_all) else stringResource(R.string.rules_logic_any)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            rule.conditions.forEach { condition -> Text("• " + RuleEvaluator.describe(condition), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onEdit(rule) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.rules_edit))
                }
                OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.rules_delete))
                }
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.rules_delete_title)) },
            text = { Text(stringResource(R.string.rules_delete_message, rule.name)) },
            confirmButton = { TextButton(onClick = { onDelete(rule.id); showDelete = false }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun EvaluatingView(scanned: Int, path: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.rules_evaluating), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.rules_scanned_count, scanned), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (path.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

@Composable
private fun ReportView(report: com.aisupercleaner.ultimate.data.rules.RuleEngineReport?, onBack: () -> Unit, onApply: (CleanupRule, List<String>) -> Unit) {
    if (report == null) return
    if (report.matches.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.rules_no_results), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text(stringResource(R.string.rules_back)) }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.rules_results_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.rules_results_summary, report.totalMatchedFiles, Formatter.formatBytes(report.totalMatchedBytes)))
                }
            }
        }
        items(report.matches, key = { it.rule.id }) { match ->
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text(match.rule.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.rules_match_summary, match.count, Formatter.formatBytes(match.totalBytes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onApply(match.rule, match.matchedFiles.map { it.path }) },
                            modifier = Modifier.weight(1f),
                            enabled = match.rule.action != CleanupRule.Action.SUGGEST,
                        ) {
                            Text(when (match.rule.action) {
                                CleanupRule.Action.SUGGEST -> stringResource(R.string.rules_action_suggest)
                                CleanupRule.Action.DELETE -> stringResource(R.string.rules_action_delete)
                                CleanupRule.Action.SHRED -> stringResource(R.string.rules_action_shred)
                                CleanupRule.Action.MOVE_TO_VAULT -> stringResource(R.string.rules_action_vault)
                            })
                        }
                    }
                }
            }
        }
        item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.rules_back_to_list)) } }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun ErrorView(message: String, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.rules_error_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
    }
}

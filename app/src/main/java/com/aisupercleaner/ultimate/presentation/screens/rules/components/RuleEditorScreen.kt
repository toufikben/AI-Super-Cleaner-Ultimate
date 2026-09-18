package com.aisupercleaner.ultimate.presentation.screens.rules.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.data.rules.CleanupRule
import com.aisupercleaner.ultimate.data.rules.Condition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(initial: CleanupRule, onSave: (CleanupRule) -> Unit, onCancel: () -> Unit) {
    var rule by remember { mutableStateOf(initial) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (initial.name.isBlank()) R.string.rule_editor_new_title else R.string.rule_editor_edit_title)) },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { onSave(rule) }, enabled = rule.name.isNotBlank() && rule.conditions.isNotEmpty()) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(value = rule.name, onValueChange = { rule = rule.copy(name = it) }, label = { Text(stringResource(R.string.rule_editor_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = rule.description, onValueChange = { rule = rule.copy(description = it) }, label = { Text(stringResource(R.string.rule_editor_description)) }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

            Text(stringResource(R.string.rules_logic_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CleanupRule.Logic.entries.forEachIndexed { idx, logic ->
                    SegmentedButton(
                        selected = rule.logic == logic,
                        onClick = { rule = rule.copy(logic = logic) },
                        shape = SegmentedButtonDefaults.itemShape(idx, 2),
                        label = { Text(stringResource(if (logic == CleanupRule.Logic.AND) R.string.rules_logic_all_and else R.string.rules_logic_any_or)) },
                    )
                }
            }

            var showAdd by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_conditions), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, contentDescription = null) }
                AddConditionMenu(expanded = showAdd, onDismiss = { showAdd = false }, onPick = { rule = rule.copy(conditions = rule.conditions + it); showAdd = false })
            }

            rule.conditions.forEachIndexed { index, condition ->
                ConditionEditor(
                    condition = condition,
                    onChange = { newCond -> rule = rule.copy(conditions = rule.conditions.toMutableList().also { it[index] = newCond }) },
                    onRemove = { rule = rule.copy(conditions = rule.conditions.filterIndexed { i, _ -> i != index }) },
                )
            }

            Text(stringResource(R.string.rules_action_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            CleanupRule.Action.entries.forEach { action ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = rule.action == action, onClick = { rule = rule.copy(action = action) })
                    Text(
                        when (action) {
                            CleanupRule.Action.SUGGEST -> stringResource(R.string.rule_action_suggest_full)
                            CleanupRule.Action.MOVE_TO_VAULT -> stringResource(R.string.rule_action_move_vault)
                            CleanupRule.Action.SHRED -> stringResource(R.string.rule_action_shred_full)
                            CleanupRule.Action.DELETE -> stringResource(R.string.rule_action_delete_full)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AddConditionMenu(expanded: Boolean, onDismiss: () -> Unit, onPick: (Condition) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        listOf(
            stringResource(R.string.condition_add_size) to Condition.Size(valueMb = 100, op = Condition.Op.GREATER_THAN),
            stringResource(R.string.condition_add_age) to Condition.Age(valueDays = 30, op = Condition.Op.GREATER_THAN),
            stringResource(R.string.condition_add_extension) to Condition.Extension(values = listOf("mp4"), op = Condition.Op.IN),
            stringResource(R.string.condition_add_path) to Condition.PathCondition(field = Condition.Field.PATH_CONTAINS, value = "", op = Condition.Op.CONTAINS),
            stringResource(R.string.condition_add_name) to Condition.NameCondition(field = Condition.Field.NAME_CONTAINS, value = "", op = Condition.Op.CONTAINS),
        ).forEach { (label, condition) ->
            DropdownMenuItem(text = { Text(label) }, onClick = { onPick(condition) })
        }
    }
}

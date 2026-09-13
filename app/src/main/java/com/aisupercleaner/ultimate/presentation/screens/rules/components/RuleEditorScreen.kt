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
import com.aisupercleaner.ultimate.data.rules.CleanupRule
import com.aisupercleaner.ultimate.data.rules.Condition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(initial: CleanupRule, onSave: (CleanupRule) -> Unit, onCancel: () -> Unit) {
    var rule by remember { mutableStateOf(initial) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial.name.isBlank()) "قاعدة جديدة" else "تعديل قاعدة") },
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
            OutlinedTextField(value = rule.name, onValueChange = { rule = rule.copy(name = it) }, label = { Text("اسم القاعدة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = rule.description, onValueChange = { rule = rule.copy(description = it) }, label = { Text("وصف (اختياري)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

            Text("المنطق", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CleanupRule.Logic.entries.forEachIndexed { idx, logic ->
                    SegmentedButton(
                        selected = rule.logic == logic,
                        onClick = { rule = rule.copy(logic = logic) },
                        shape = SegmentedButtonDefaults.itemShape(idx, 2),
                        label = { Text(if (logic == CleanupRule.Logic.AND) "كل الشروط (AND)" else "أي شرط (OR)") },
                    )
                }
            }

            var showAdd by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("الشروط", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
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

            Text("الإجراء", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            CleanupRule.Action.entries.forEach { action ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = rule.action == action, onClick = { rule = rule.copy(action = action) })
                    Text(
                        when (action) {
                            CleanupRule.Action.SUGGEST -> "اقترح فقط (لا تحذف تلقائيًا)"
                            CleanupRule.Action.MOVE_TO_VAULT -> "انقل إلى الخزنة"
                            CleanupRule.Action.SHRED -> "احذف بشكل آمن (Shredder)"
                            CleanupRule.Action.DELETE -> "احذف مباشرة"
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
            "الحجم" to Condition.Size(valueMb = 100, op = Condition.Op.GREATER_THAN),
            "العمر" to Condition.Age(valueDays = 30, op = Condition.Op.GREATER_THAN),
            "الامتداد" to Condition.Extension(values = listOf("mp4"), op = Condition.Op.IN),
            "المسار يحتوي" to Condition.PathCondition(field = Condition.Field.PATH_CONTAINS, value = "", op = Condition.Op.CONTAINS),
            "الاسم يحتوي" to Condition.NameCondition(field = Condition.Field.NAME_CONTAINS, value = "", op = Condition.Op.CONTAINS),
        ).forEach { (label, condition) ->
            DropdownMenuItem(text = { Text(label) }, onClick = { onPick(condition) })
        }
    }
}

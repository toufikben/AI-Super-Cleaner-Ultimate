package com.aisupercleaner.ultimate.presentation.screens.rules.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.data.rules.Condition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionEditor(condition: Condition, onChange: (Condition) -> Unit, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (condition.field) {
                        Condition.Field.SIZE -> "الحجم"
                        Condition.Field.AGE -> "العمر"
                        Condition.Field.EXTENSION -> "النوع"
                        Condition.Field.PATH_CONTAINS, Condition.Field.PATH_STARTS -> "المسار"
                        Condition.Field.NAME_CONTAINS, Condition.Field.NAME_MATCHES -> "الاسم"
                        Condition.Field.MIME_TYPE -> "MIME"
                        Condition.Field.IS_IN_GALLERY -> "المعرض"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) { Icon(Icons.Rounded.Delete, contentDescription = null) }
            }
            Spacer(Modifier.height(8.dp))
            when (condition) {
                is Condition.Size -> SizeEditor(condition, onChange)
                is Condition.Age -> AgeEditor(condition, onChange)
                is Condition.Extension -> ExtensionEditor(condition, onChange)
                is Condition.PathCondition -> PathEditor(condition, onChange)
                is Condition.NameCondition -> NameEditor(condition, onChange)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeEditor(c: Condition.Size, onChange: (Condition) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OperatorDropdown(c.op) { onChange(c.copy(op = it)) }
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = c.valueMb.toString(),
            onValueChange = { v -> v.filter(Char::isDigit).toLongOrNull()?.let { onChange(c.copy(valueMb = it)) } },
            label = { Text("MB") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeEditor(c: Condition.Age, onChange: (Condition) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OperatorDropdown(c.op, ageMode = true) { onChange(c.copy(op = it)) }
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = c.valueDays.toString(),
            onValueChange = { v -> v.filter(Char::isDigit).toIntOrNull()?.let { onChange(c.copy(valueDays = it)) } },
            label = { Text("يوم") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExtensionEditor(c: Condition.Extension, onChange: (Condition) -> Unit) {
    OutlinedTextField(
        value = c.values.joinToString(", "),
        onValueChange = { v ->
            val list = v.split(",").map { it.trim().removePrefix(".").lowercase() }.filter { it.isNotBlank() }
            onChange(c.copy(values = list))
        },
        label = { Text("mp4, mkv, avi") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PathEditor(c: Condition.PathCondition, onChange: (Condition) -> Unit) {
    OutlinedTextField(value = c.value, onValueChange = { onChange(c.copy(value = it)) }, label = { Text("مثال: Download") }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun NameEditor(c: Condition.NameCondition, onChange: (Condition) -> Unit) {
    OutlinedTextField(value = c.value, onValueChange = { onChange(c.copy(value = it)) }, label = { Text("نمط الاسم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperatorDropdown(current: Condition.Op, ageMode: Boolean = false, onChange: (Condition.Op) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when {
        ageMode && current == Condition.Op.GREATER_THAN -> "أقدم من"
        ageMode && current == Condition.Op.LESS_THAN -> "أحدث من"
        current == Condition.Op.GREATER_THAN -> "أكبر من"
        current == Condition.Op.LESS_THAN -> "أصغر من"
        current == Condition.Op.EQUALS -> "="
        else -> "?"
    }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                Condition.Op.GREATER_THAN to (if (ageMode) "أقدم من" else "أكبر من"),
                Condition.Op.LESS_THAN to (if (ageMode) "أحدث من" else "أصغر من"),
                Condition.Op.EQUALS to "يساوي",
            ).forEach { (op, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onChange(op); expanded = false })
            }
        }
    }
}

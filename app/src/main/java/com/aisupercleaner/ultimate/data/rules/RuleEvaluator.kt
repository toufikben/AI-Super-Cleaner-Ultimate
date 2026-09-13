package com.aisupercleaner.ultimate.data.rules

import java.io.File
import java.util.concurrent.TimeUnit

object RuleEvaluator {

    fun matches(file: File, condition: Condition): Boolean = when (condition) {
        is Condition.Size -> compareLong(file.length() / (1024L * 1024L), condition.valueMb, condition.op)
        is Condition.Age -> compareLong(
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - file.lastModified()),
            condition.valueDays.toLong(),
            condition.op,
        )
        is Condition.Extension -> {
            val ext = file.extension.lowercase()
            when (condition.op) {
                Condition.Op.IN -> condition.values.map { it.lowercase() }.contains(ext)
                Condition.Op.NOT_IN -> !condition.values.map { it.lowercase() }.contains(ext)
                else -> false
            }
        }
        is Condition.PathCondition -> {
            val path = file.absolutePath
            when (condition.op) {
                Condition.Op.CONTAINS -> path.contains(condition.value, ignoreCase = true)
                Condition.Op.STARTS_WITH -> path.startsWith(condition.value, ignoreCase = true)
                Condition.Op.ENDS_WITH -> path.endsWith(condition.value, ignoreCase = true)
                else -> false
            }
        }
        is Condition.NameCondition -> {
            val name = file.name
            when (condition.op) {
                Condition.Op.CONTAINS -> name.contains(condition.value, ignoreCase = true)
                Condition.Op.STARTS_WITH -> name.startsWith(condition.value, ignoreCase = true)
                Condition.Op.ENDS_WITH -> name.endsWith(condition.value, ignoreCase = true)
                Condition.Op.MATCHES -> runCatching { Regex(condition.value, RegexOption.IGNORE_CASE).matches(name) }.getOrDefault(false)
                else -> false
            }
        }
    }

    fun matchesRule(file: File, rule: CleanupRule): Boolean {
        if (!rule.enabled || rule.conditions.isEmpty()) return false
        return when (rule.logic) {
            CleanupRule.Logic.AND -> rule.conditions.all { matches(file, it) }
            CleanupRule.Logic.OR -> rule.conditions.any { matches(file, it) }
        }
    }

    private fun compareLong(left: Long, right: Long, op: Condition.Op): Boolean = when (op) {
        Condition.Op.GREATER_THAN -> left > right
        Condition.Op.LESS_THAN -> left < right
        Condition.Op.EQUALS -> left == right
        Condition.Op.NOT_EQUALS -> left != right
        else -> false
    }

    fun describe(condition: Condition): String = when (condition) {
        is Condition.Size -> "الحجم ${if (condition.op == Condition.Op.GREATER_THAN) ">" else "<"} ${condition.valueMb} MB"
        is Condition.Age -> "${if (condition.op == Condition.Op.GREATER_THAN) "أقدم من" else "أحدث من"} ${condition.valueDays} يوم"
        is Condition.Extension -> if (condition.op == Condition.Op.NOT_IN) "ليس من نوع: ${condition.values.joinToString()}" else "من نوع: ${condition.values.joinToString()}"
        is Condition.PathCondition -> "المسار يحتوي: ${condition.value}"
        is Condition.NameCondition -> "الاسم يحتوي: ${condition.value}"
    }
}

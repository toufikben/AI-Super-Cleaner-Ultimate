package com.aisupercleaner.ultimate.data.rules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CleanupRule(
    val id: String,
    val name: String,
    val description: String = "",
    val enabled: Boolean = true,
    val logic: Logic = Logic.AND,
    val conditions: List<Condition> = emptyList(),
    val action: Action = Action.SUGGEST,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null,
    val runCount: Int = 0,
    val bytesFreed: Long = 0L,
) {
    enum class Logic { AND, OR }
    enum class Action { SUGGEST, MOVE_TO_VAULT, SHRED, DELETE }
}

@Serializable
sealed class Condition {
    abstract val field: Field

    enum class Field {
        SIZE, AGE, EXTENSION, MIME_TYPE, PATH_CONTAINS, PATH_STARTS,
        NAME_CONTAINS, NAME_MATCHES, IS_IN_GALLERY
    }

    enum class Op {
        GREATER_THAN, LESS_THAN, EQUALS, NOT_EQUALS,
        CONTAINS, STARTS_WITH, ENDS_WITH, MATCHES, IN, NOT_IN
    }

    @Serializable
    @SerialName("size")
    data class Size(
        override val field: Field = Field.SIZE,
        val valueMb: Long,
        val op: Op = Op.GREATER_THAN,
    ) : Condition()

    @Serializable
    @SerialName("age")
    data class Age(
        override val field: Field = Field.AGE,
        val valueDays: Int,
        val op: Op = Op.GREATER_THAN,
    ) : Condition()

    @Serializable
    @SerialName("extension")
    data class Extension(
        override val field: Field = Field.EXTENSION,
        val values: List<String>,
        val op: Op = Op.IN,
    ) : Condition()

    @Serializable
    @SerialName("path")
    data class PathCondition(
        override val field: Field,
        val value: String,
        val op: Op,
    ) : Condition()

    @Serializable
    @SerialName("name")
    data class NameCondition(
        override val field: Field,
        val value: String,
        val op: Op,
    ) : Condition()
}

data class RuleMatch(val rule: CleanupRule, val matchedFiles: List<MatchedFile>) {
    val totalBytes: Long get() = matchedFiles.sumOf { it.sizeBytes }
    val count: Int get() = matchedFiles.size
}

data class MatchedFile(val path: String, val sizeBytes: Long, val lastModified: Long)

data class RuleEngineReport(
    val matches: List<RuleMatch>,
    val scannedFiles: Int,
    val durationMs: Long,
    val errors: List<String> = emptyList(),
) {
    val totalMatchedBytes: Long get() = matches.sumOf { it.totalBytes }
    val totalMatchedFiles: Int get() = matches.sumOf { it.count }
}

package com.aisupercleaner.ultimate.presentation.screens.rules

import com.aisupercleaner.ultimate.data.rules.CleanupRule
import com.aisupercleaner.ultimate.data.rules.RuleEngineReport

data class RulesUiState(
    val phase: Phase = Phase.LIST,
    val rules: List<CleanupRule> = emptyList(),
    val report: RuleEngineReport? = null,
    val progress: Int = 0,
    val progressPath: String = "",
    val editingRule: CleanupRule? = null,
    val errorMessage: String? = null,
) {
    val hasRules: Boolean get() = rules.isNotEmpty()
    val activeCount: Int get() = rules.count { it.enabled }

    enum class Phase { LIST, EVALUATING, REPORT, EDITOR, ERROR }
}

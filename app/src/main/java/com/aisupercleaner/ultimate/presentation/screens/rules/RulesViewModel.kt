package com.aisupercleaner.ultimate.presentation.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.cleanup.CleanupManager
import com.aisupercleaner.ultimate.data.history.HistoryEntry
import com.aisupercleaner.ultimate.data.history.HistoryRepository
import com.aisupercleaner.ultimate.data.rules.CleanupRule
import com.aisupercleaner.ultimate.data.rules.RuleEngine
import com.aisupercleaner.ultimate.data.rules.RuleRepository
import com.aisupercleaner.ultimate.data.shredder.ShredderManager
import com.aisupercleaner.ultimate.data.storage.StorageRepository
import com.aisupercleaner.ultimate.data.vault.VaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val repository: RuleRepository,
    private val engine: RuleEngine,
    private val cleanupManager: CleanupManager,
    private val shredderManager: ShredderManager,
    private val vaultManager: VaultManager,
    private val historyRepository: HistoryRepository,
    private val storageRepository: StorageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.rulesFlow.collect { list -> _uiState.update { it.copy(rules = list) } }
        }
    }

    fun startNewRule() {
        _uiState.update {
            it.copy(phase = RulesUiState.Phase.EDITOR, editingRule = CleanupRule(id = UUID.randomUUID().toString(), name = ""))
        }
    }

    fun startEdit(rule: CleanupRule) { _uiState.update { it.copy(phase = RulesUiState.Phase.EDITOR, editingRule = rule) } }
    fun cancelEdit() { _uiState.update { it.copy(phase = RulesUiState.Phase.LIST, editingRule = null) } }

    fun saveRule(rule: CleanupRule) {
        viewModelScope.launch {
            if (_uiState.value.rules.any { it.id == rule.id }) repository.updateRule(rule) else repository.addRule(rule)
            _uiState.update { it.copy(phase = RulesUiState.Phase.LIST, editingRule = null) }
        }
    }

    fun deleteRule(id: String) { viewModelScope.launch { repository.deleteRule(id) } }
    fun toggleRule(id: String, enabled: Boolean) { viewModelScope.launch { repository.toggleRule(id, enabled) } }

    fun evaluate() {
        _uiState.update { it.copy(phase = RulesUiState.Phase.EVALUATING, progress = 0, progressPath = "") }
        viewModelScope.launch {
            engine.evaluate(_uiState.value.rules).collect { event ->
                when (event) {
                    is RuleEngine.Event.Progress -> _uiState.update { it.copy(progress = event.progress.scanned, progressPath = event.progress.currentPath) }
                    is RuleEngine.Event.Done -> _uiState.update { it.copy(phase = RulesUiState.Phase.REPORT, report = event.report) }
                    is RuleEngine.Event.Error -> _uiState.update { it.copy(phase = RulesUiState.Phase.ERROR, errorMessage = event.throwable.message) }
                }
            }
        }
    }

    fun backToList() { _uiState.update { it.copy(phase = RulesUiState.Phase.LIST, report = null, progress = 0) } }

    // ✅ FIX: تنفيذ كل الإجراءات فعليًا
    fun applyAction(rule: CleanupRule, paths: List<String>) {
        viewModelScope.launch {
            val startMs = System.currentTimeMillis()
            var freedBytes = 0L
            var deletedCount = 0
            var failedCount = 0

            when (rule.action) {
                CleanupRule.Action.SUGGEST -> Unit
                CleanupRule.Action.DELETE -> {
                    val result = cleanupManager.deleteFiles(paths)
                    freedBytes = result.freedBytes; deletedCount = result.deletedCount; failedCount = result.failedCount
                }
                CleanupRule.Action.SHRED -> {
                    val results = shredderManager.shredBatch(paths.map { File(it) }, passes = 3)
                    results.forEach { r -> if (r.success) { deletedCount++; freedBytes += r.bytesShredded } else failedCount++ }
                }
                CleanupRule.Action.MOVE_TO_VAULT -> {
                    paths.forEach { path ->
                        runCatching {
                            val file = File(path)
                            if (file.exists()) {
                                val uri = android.net.Uri.fromFile(file)
                                val res = vaultManager.importFile(uri, deleteOriginal = true)
                                if (res.isSuccess) { deletedCount++; freedBytes += file.length() } else failedCount++
                            }
                        }.onFailure { failedCount++ }
                    }
                }
            }

            if (deletedCount > 0 || failedCount > 0) {
                historyRepository.record(freedBytes, deletedCount, failedCount, HistoryEntry.Source.JUNK, System.currentTimeMillis() - startMs)
                storageRepository.refresh()
            }
            evaluate()
        }
    }

    // ✅ FIX: callback بدل return
    fun exportRules(callback: (String) -> Unit) {
        viewModelScope.launch { callback(repository.exportJson()) }
    }

    fun importRules(json: String, onDone: (Int) -> Unit) {
        viewModelScope.launch { repository.importJson(json).onSuccess { onDone(it) } }
    }

    fun dismissError() { _uiState.update { it.copy(phase = RulesUiState.Phase.LIST, errorMessage = null) } }
}

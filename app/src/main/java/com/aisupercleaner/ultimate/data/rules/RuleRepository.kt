package com.aisupercleaner.ultimate.data.rules

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rulesDataStore: DataStore<Preferences> by preferencesDataStore("rules_prefs")

@Singleton
class RuleRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val rulesMutex = Mutex()

    val rulesFlow: Flow<List<CleanupRule>> = context.rulesDataStore.data.map { prefs ->
        prefs[KEY_RULES]?.let { decode(it) } ?: emptyList()
    }

    suspend fun save(rules: List<CleanupRule>) = withContext(Dispatchers.IO) {
        val serialized = json.encodeToString(ListSerializer(CleanupRule.serializer()), rules)
        context.rulesDataStore.edit { it[KEY_RULES] = serialized }
    }

    suspend fun addRule(rule: CleanupRule) { updateAtomically { it + rule } }
    suspend fun updateRule(rule: CleanupRule) { updateAtomically { rules -> rules.map { if (it.id == rule.id) rule else it } } }
    suspend fun deleteRule(id: String) { updateAtomically { rules -> rules.filterNot { it.id == id } } }
    suspend fun toggleRule(id: String, enabled: Boolean) { updateAtomically { rules -> rules.map { if (it.id == id) it.copy(enabled = enabled) else it } } }

    private suspend fun updateAtomically(transform: (List<CleanupRule>) -> List<CleanupRule>) = withContext(Dispatchers.IO) {
        rulesMutex.withLock {
            context.rulesDataStore.edit { prefs ->
                val current = prefs[KEY_RULES]?.let { decode(it) } ?: emptyList()
                prefs[KEY_RULES] = json.encodeToString(ListSerializer(CleanupRule.serializer()), transform(current))
            }
        }
    }

    // ✅ FIX: first() للقراءة، لا edit()
    suspend fun readAll(): List<CleanupRule> = withContext(Dispatchers.IO) {
        val prefs = context.rulesDataStore.data.first()
        prefs[KEY_RULES]?.let { decode(it) } ?: emptyList()
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        json.encodeToString(ListSerializer(CleanupRule.serializer()), readAll())
    }

    suspend fun importJson(jsonString: String): Result<Int> = runCatching {
        val parsed = json.decodeFromString(ListSerializer(CleanupRule.serializer()), jsonString)
        save(parsed)
        parsed.size
    }

    private fun decode(value: String): List<CleanupRule> = runCatching {
        json.decodeFromString(ListSerializer(CleanupRule.serializer()), value)
    }.getOrDefault(emptyList())

    companion object {
        private val KEY_RULES = stringPreferencesKey("cleanup_rules_v1")
    }
}

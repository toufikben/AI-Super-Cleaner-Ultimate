package com.aisupercleaner.ultimate.data.rules

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleEngine @Inject constructor(@ApplicationContext private val context: Context) {

    data class Progress(val scanned: Int, val currentPath: String = "")

    fun evaluate(rules: List<CleanupRule>): Flow<Event> = flow {
        val start = System.currentTimeMillis()
        val active = rules.filter { it.enabled && it.conditions.isNotEmpty() }
        if (active.isEmpty()) {
            emit(Event.Done(RuleEngineReport(emptyList(), 0, 0)))
            return@flow
        }

        val matchesMap = mutableMapOf<String, MutableList<MatchedFile>>()
        active.forEach { matchesMap[it.id] = mutableListOf() }

        var scanned = 0
        for (root in defaultRoots()) {
            if (!root.exists() || !root.canRead()) continue
            root.walkTopDown()
                .onEnter { true }
                .filter { it.isFile && it.canRead() && it.length() > 0 }
                .forEach { file ->
                    scanned++
                    if (scanned % 100 == 0) emit(Event.Progress(Progress(scanned, file.absolutePath)))
                    active.forEach { rule ->
                        if (RuleEvaluator.matchesRule(file, rule)) {
                            matchesMap[rule.id]?.add(MatchedFile(file.absolutePath, file.length(), file.lastModified()))
                        }
                    }
                }
        }

        val matches = active.mapNotNull { rule ->
            val files = matchesMap[rule.id].orEmpty()
            if (files.isEmpty()) null else RuleMatch(rule, files.sortedByDescending { it.sizeBytes })
        }.sortedByDescending { it.totalBytes }

        emit(Event.Done(RuleEngineReport(matches, scanned, System.currentTimeMillis() - start)))
    }.flowOn(Dispatchers.IO)

    private fun defaultRoots(): List<File> {
        val ext = Environment.getExternalStorageDirectory()
        return listOf(
            File(ext, Environment.DIRECTORY_DOWNLOADS),
            File(ext, Environment.DIRECTORY_DOCUMENTS),
            File(ext, Environment.DIRECTORY_PICTURES),
            File(ext, Environment.DIRECTORY_DCIM),
            File(ext, Environment.DIRECTORY_MOVIES),
            File(ext, Environment.DIRECTORY_MUSIC),
            File(ext, "WhatsApp"),
            File(ext, "Telegram"),
            File(ext, "Android/media"),
        ).filter { it.exists() }
    }

    sealed interface Event {
        data class Progress(val progress: RuleEngine.Progress) : Event
        data class Done(val report: RuleEngineReport) : Event
        data class Error(val throwable: Throwable) : Event
    }
}

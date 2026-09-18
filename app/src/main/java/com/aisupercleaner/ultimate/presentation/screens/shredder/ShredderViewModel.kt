package com.aisupercleaner.ultimate.presentation.screens.shredder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.core.util.FileUtils
import com.aisupercleaner.ultimate.data.shredder.ShredderManager
import com.aisupercleaner.ultimate.data.storage.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ShredderViewModel @Inject constructor(private val shredderManager: ShredderManager, private val storageRepository: StorageRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ShredderUiState())
    val uiState: StateFlow<ShredderUiState> = _uiState.asStateFlow()
    private var shredJob: Job? = null
    fun onFilesPicked(uris: List<Uri>, appContext: android.content.Context) { if (uris.isEmpty()) return; viewModelScope.launch { val files = uris.mapNotNull { uri -> runCatching { var name = "selected"; var size = 0L; appContext.contentResolver.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) { val ni = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); val si = c.getColumnIndex(android.provider.OpenableColumns.SIZE); if (ni >= 0) name = c.getString(ni) ?: name; if (si >= 0) size = c.getLong(si) } }; val cached = File(appContext.cacheDir, "shred_${UUID.randomUUID()}.tmp"); try { appContext.contentResolver.openInputStream(uri)?.use { input -> cached.outputStream().use { input.copyTo(it) } } ?: error("Cannot open selected file"); ShredderUiState.SelectedFile(cached.absolutePath, name, size) } catch (cancelled: CancellationException) { FileUtils.shredFile(cached, 1); throw cancelled } catch (failure: Throwable) { FileUtils.shredFile(cached, 1); throw failure } }.getOrElse { failure -> if (failure is CancellationException) throw failure else null } }; _uiState.update { it.copy(selectedFiles = it.selectedFiles + files) } } }
    fun removeFile(path: String) { _uiState.update { s -> s.copy(selectedFiles = s.selectedFiles.filterNot { it.path == path }) }; viewModelScope.launch { FileUtils.shredFile(File(path), 1) } }
    fun setLevel(level: ShredderUiState.Level) { if (!_uiState.value.isRunning) _uiState.update { it.copy(level = level) } }
    fun startShred() { if (shredJob?.isActive == true) return; val state = _uiState.value; if (state.selectedFiles.isEmpty()) return; _uiState.update { it.copy(phase = ShredderUiState.Phase.SHREDDING, currentIndex = 0, results = emptyList(), errorMessage = null) }; shredJob = viewModelScope.launch { val files = state.selectedFiles.map { File(it.path) }; try { val results = shredderManager.shredBatch(files, state.level.passes) { index, _ -> _uiState.update { it.copy(currentIndex = index) } }; _uiState.update { it.copy(phase = ShredderUiState.Phase.DONE, results = results, selectedFiles = emptyList()) }; storageRepository.refresh() } catch (cancelled: CancellationException) { _uiState.update { it.copy(phase = ShredderUiState.Phase.IDLE, currentIndex = 0) }; throw cancelled } catch (failure: Throwable) { _uiState.update { it.copy(phase = ShredderUiState.Phase.ERROR, errorMessage = failure.message) } } finally { files.filter { it.exists() }.forEach { FileUtils.shredFile(it, 1) }; shredJob = null } } }
    fun cancelShred() { shredJob?.cancel(); shredJob = null }
    fun dismissResult() { _uiState.update { it.copy(phase = ShredderUiState.Phase.IDLE, results = emptyList()) } }
    fun dismissError() { _uiState.update { it.copy(errorMessage = null, phase = ShredderUiState.Phase.IDLE) } }
    override fun onCleared() { shredJob?.cancel(); _uiState.value.selectedFiles.map { File(it.path) }.forEach { FileUtils.shredFile(it, 1) }; super.onCleared() }
}

package com.aisupercleaner.ultimate.presentation.screens.shredder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.shredder.ShredderManager
import com.aisupercleaner.ultimate.data.storage.StorageRepository
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
class ShredderViewModel @Inject constructor(
    private val shredderManager: ShredderManager,
    private val storageRepository: StorageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShredderUiState())
    val uiState: StateFlow<ShredderUiState> = _uiState.asStateFlow()

    fun onFilesPicked(uris: List<Uri>, appContext: android.content.Context) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val files = uris.mapNotNull { uri ->
                runCatching {
                    var name = ""; var size = 0L
                    appContext.contentResolver.query(uri, null, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val ni = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            val si = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (ni >= 0) name = c.getString(ni) ?: ""
                            if (si >= 0) size = c.getLong(si)
                        }
                    }
                    val cached = File(appContext.cacheDir, "shred_${UUID.randomUUID()}_$name")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        cached.outputStream().use { input.copyTo(it) }
                    }
                    ShredderUiState.SelectedFile(cached.absolutePath, size)
                }.getOrNull()
            }
            _uiState.update { it.copy(selectedFiles = it.selectedFiles + files) }
        }
    }

    fun removeFile(path: String) {
        _uiState.update { state -> state.copy(selectedFiles = state.selectedFiles.filterNot { it.path == path }) }
    }

    fun setLevel(level: ShredderUiState.Level) { _uiState.update { it.copy(level = level) } }

    fun startShred() {
        val state = _uiState.value
        if (state.selectedFiles.isEmpty()) return
        _uiState.update { it.copy(phase = ShredderUiState.Phase.SHREDDING, currentIndex = 0, results = emptyList()) }

        viewModelScope.launch {
            val files = state.selectedFiles.map { File(it.path) }
            val results = shredderManager.shredBatch(files, state.level.passes) { index, _ ->
                _uiState.update { it.copy(currentIndex = index) }
            }
            _uiState.update { it.copy(phase = ShredderUiState.Phase.DONE, results = results, selectedFiles = emptyList()) }
            storageRepository.refresh()
        }
    }

    fun dismissResult() { _uiState.update { it.copy(phase = ShredderUiState.Phase.IDLE, results = emptyList()) } }
    fun dismissError() { _uiState.update { it.copy(errorMessage = null, phase = ShredderUiState.Phase.IDLE) } }
}

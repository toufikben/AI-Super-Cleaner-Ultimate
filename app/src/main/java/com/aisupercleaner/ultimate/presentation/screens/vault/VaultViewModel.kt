package com.aisupercleaner.ultimate.presentation.screens.vault

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.storage.StorageRepository
import com.aisupercleaner.ultimate.data.vault.VaultAuthManager
import com.aisupercleaner.ultimate.data.vault.VaultFile
import com.aisupercleaner.ultimate.data.vault.VaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val manager: VaultManager,
    private val authManager: VaultAuthManager,
    private val storageRepository: StorageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init { refreshState(); observeFiles() }

    private fun refreshState() {
        _uiState.update {
            it.copy(
                needsSetup = !authManager.isPinSet(),
                biometricAvailable = authManager.canUseBiometric(),
                biometricEnabled = authManager.isBiometricEnabled(),
                isLocked = true,
            )
        }
    }

    private fun observeFiles() {
        viewModelScope.launch {
            manager.observeVaultFiles().collect { files ->
                _uiState.update { it.copy(files = files, totalBytes = files.sumOf { f -> f.sizeBytes }) }
            }
        }
    }

    fun unlockWithPin() {
        val pin = _uiState.value.pinInput
        if (pin.length < 4) { _uiState.update { it.copy(pinError = true) }; return }
        if (authManager.verifyPin(pin)) _uiState.update { it.copy(isLocked = false, pinInput = "", pinError = false) }
        else _uiState.update { it.copy(pinError = true, pinInput = "") }
    }

    fun onPinChanged(input: String) {
        _uiState.update { it.copy(pinInput = input.take(8), pinError = false) }
    }

    fun authenticateWithBiometric(activity: FragmentActivity) {
        if (!authManager.canUseBiometric()) return
        authManager.authenticate(
            activity = activity,
            onSuccess = { _uiState.update { it.copy(isLocked = false) } },
            onError = { msg -> _uiState.update { it.copy(errorMessage = msg) } }
        )
    }

    fun setupPin(pin: String): Boolean {
        val ok = authManager.setPin(pin)
        if (ok) refreshState()
        return ok
    }

    fun lock() {
        _uiState.update { it.copy(isLocked = true, previewFile = null) }
        viewModelScope.launch { manager.cleanupTemp() }
    }

    fun importFiles(uris: List<Uri>, deleteOriginal: Boolean = false) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importProgress = VaultUiState.ImportProgress(total = uris.size, currentIndex = 0)) }
            uris.forEachIndexed { index, uri ->
                _uiState.update {
                    it.copy(importProgress = VaultUiState.ImportProgress(uri.lastPathSegment ?: "", index + 1, uris.size))
                }
                manager.importFile(uri, deleteOriginal)
            }
            _uiState.update { it.copy(isImporting = false, importProgress = null) }
            storageRepository.refresh()
        }
    }

    fun requestPreview(file: VaultFile) {
        _uiState.update { it.copy(previewFile = file) }
        viewModelScope.launch { delay(30_000); manager.cleanupTemp() }
    }

    fun closePreview() {
        _uiState.update { it.copy(previewFile = null) }
        viewModelScope.launch { manager.cleanupTemp() }
    }

    suspend fun getPreviewFile(fileId: Long): File? = manager.decryptToTemp(fileId).getOrNull()

    fun deleteFile(fileId: Long, shred: Boolean = true) {
        viewModelScope.launch {
            manager.deleteFile(fileId, shred)
            storageRepository.refresh()
        }
    }

    fun dismissError() { _uiState.update { it.copy(errorMessage = null) } }
}

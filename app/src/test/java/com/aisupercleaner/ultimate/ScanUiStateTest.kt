package com.aisupercleaner.ultimate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanUiStateTest {
    @Test fun onlyScanningAndAnalyzingAreBusy() {
        assertTrue(ScanUiState.Scanning().isBusy())
        assertTrue(ScanUiState.Analyzing.isBusy())
        assertFalse(ScanUiState.Idle.isBusy())
        assertFalse(ScanUiState.Success("done").isBusy())
        assertFalse(ScanUiState.Error("failed").isBusy())
        assertFalse(ScanUiState.Cancelled.isBusy())
    }
}

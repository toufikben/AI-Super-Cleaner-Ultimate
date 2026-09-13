package com.aisupercleaner.ultimate.presentation.permissions

import com.aisupercleaner.ultimate.core.permissions.PermissionStatus
import com.aisupercleaner.ultimate.core.permissions.PermissionType

data class PermissionsUiState(
    val statuses: Map<PermissionType, PermissionStatus> = emptyMap(),
    val isFirstRun: Boolean = true,
) {
    val criticalGranted: Boolean get() = PermissionType.entries.filter { it.isCritical }.all { statuses[it] == PermissionStatus.GRANTED }
    val canProceed: Boolean get() = criticalGranted || statuses[PermissionType.ALL_FILES] == PermissionStatus.GRANTED
    val grantedCount: Int get() = statuses.values.count { it == PermissionStatus.GRANTED }
    val totalApplicable: Int get() = statuses.values.count { it != PermissionStatus.NOT_APPLICABLE }
}

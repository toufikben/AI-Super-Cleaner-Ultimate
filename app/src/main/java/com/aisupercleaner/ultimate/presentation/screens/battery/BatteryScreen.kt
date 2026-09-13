package com.aisupercleaner.ultimate.presentation.screens.battery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.data.system.BatteryInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(onBack: () -> Unit = {}, viewModel: BatteryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.battery_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = viewModel::refresh) { Icon(Icons.Rounded.Refresh, contentDescription = null) } },
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.isLoading) { CircularProgressIndicator(); return@Column }
            BatteryGauge(state.info)
            BatteryInfoCard(state.info)
            if (state.info.health == BatteryInfo.Health.OVERHEAT) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text("البطارية ساخنة — افصل الشاحن قليلًا", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryGauge(info: BatteryInfo) {
    val animated by animateFloatAsState(targetValue = info.levelPercent / 100f, animationSpec = tween(900), label = "battery_anim")
    val color = when {
        info.isCharging -> Color(0xFF2E7D32)
        info.levelPercent <= 15 -> Color(0xFFC62828)
        info.levelPercent <= 30 -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.primary
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(200.dp), color = MaterialTheme.colorScheme.surfaceVariant, strokeWidth = 16.dp)
        CircularProgressIndicator(progress = { animated }, modifier = Modifier.size(200.dp), color = color, strokeWidth = 16.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (info.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(4.dp))
            Text("${info.levelPercent}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = color)
            if (info.isCharging) Text("جاري الشحن", style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BatteryInfoCard(info: BatteryInfo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            BatteryRow("الحالة", if (info.isCharging) "يشحن" else "قيد التفريغ")
            BatteryRow("الحرارة", "${info.temperatureC}°C")
            BatteryRow("الفولتية", "${info.voltageMv} mV")
            BatteryRow("التقنية", info.technology)
            BatteryRow("الصحة", when (info.health) {
                BatteryInfo.Health.GOOD -> "جيدة ✓"
                BatteryInfo.Health.OVERHEAT -> "سخونة!"
                BatteryInfo.Health.DEAD -> "تالفة"
                BatteryInfo.Health.OVER_VOLTAGE -> "فولتية عالية"
                BatteryInfo.Health.COLD -> "باردة"
                BatteryInfo.Health.UNKNOWN_FAILURE -> "فشل"
                BatteryInfo.Health.UNKNOWN -> "غير معروفة"
            })
            BatteryRow("مصدر الشحن", when (info.plugged) {
                BatteryInfo.Plugged.AC -> "شاحن"
                BatteryInfo.Plugged.USB -> "USB"
                BatteryInfo.Plugged.WIRELESS -> "لاسلكي"
                BatteryInfo.Plugged.NONE -> "غير متصل"
            })
        }
    }
}

@Composable
private fun BatteryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

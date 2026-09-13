package com.aisupercleaner.ultimate.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.data.forecast.StorageForecast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ForecastCard(forecast: StorageForecast) {
    val days = forecast.daysUntilFull ?: return
    if (days > 365) return

    val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val accent = when {
        days < 7 -> MaterialTheme.colorScheme.error
        days < 30 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = accent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(forecast.recommendationRes, days), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                forecast.estimatedFullDateMs?.let { date ->
                    Text(stringResource(R.string.forecast_date, fmt.format(Date(date))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

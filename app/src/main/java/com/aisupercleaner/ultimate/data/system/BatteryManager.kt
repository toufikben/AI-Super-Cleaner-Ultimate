package com.aisupercleaner.ultimate.data.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val health: Health,
    val temperatureC: Float,
    val voltageMv: Int,
    val technology: String,
    val plugged: Plugged,
) {
    enum class Health { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, UNKNOWN_FAILURE, UNKNOWN }
    enum class Plugged { NONE, AC, USB, WIRELESS }
}

@Singleton
class BatteryManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun getBatteryInfo(): BatteryInfo = withContext(Dispatchers.IO) {
        runCatching {
            val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val percent = if (scale > 0) (level * 100) / scale else 0
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val health = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> BatteryInfo.Health.GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryInfo.Health.OVERHEAT
                BatteryManager.BATTERY_HEALTH_DEAD -> BatteryInfo.Health.DEAD
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryInfo.Health.OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_COLD -> BatteryInfo.Health.COLD
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryInfo.Health.UNKNOWN_FAILURE
                else -> BatteryInfo.Health.UNKNOWN
            }
            val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"
            val pluggedInt = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val plugged = when (pluggedInt) {
                BatteryManager.BATTERY_PLUGGED_AC -> BatteryInfo.Plugged.AC
                BatteryManager.BATTERY_PLUGGED_USB -> BatteryInfo.Plugged.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryInfo.Plugged.WIRELESS
                else -> BatteryInfo.Plugged.NONE
            }
            BatteryInfo(percent, charging, health, tempTenths / 10f, voltage, tech, plugged)
        }.getOrDefault(BatteryInfo(0, false, BatteryInfo.Health.UNKNOWN, 0f, 0, "—", BatteryInfo.Plugged.NONE))
    }
}

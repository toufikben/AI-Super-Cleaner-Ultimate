package com.aisupercleaner.ultimate.data.worker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkerSchedulerIntervalsTest {
    @Test
    fun storageAlertIntervalIsIndependentFromAutoCleanInterval() {
        val intervals = WorkerScheduler.resolveIntervals(autoCleanIntervalHours = 24, storageAlertIntervalHours = 6)

        assertThat(intervals.autoCleanHours).isEqualTo(24)
        assertThat(intervals.storageAlertHours).isEqualTo(6)
    }

    @Test
    fun invalidIntervalsAreClampedIndependently() {
        val intervals = WorkerScheduler.resolveIntervals(autoCleanIntervalHours = 0, storageAlertIntervalHours = -4)

        assertThat(intervals.autoCleanHours).isEqualTo(1)
        assertThat(intervals.storageAlertHours).isEqualTo(1)
    }
}

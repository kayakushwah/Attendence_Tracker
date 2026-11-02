package com.seventyfiveperc.app.logic

import com.seventyfiveperc.app.data.models.AttendanceRecord
import com.seventyfiveperc.app.data.models.AttendanceStatus
import kotlin.math.ceil

data class AttendanceStats(
    val percentage: Double,
    val totalLectures: Int,
    val presentCount: Int,
    val absentCount: Int,
    val safeBunks: Int,
    val requiredNextClasses: Int,
    val status: HealthStatus,
    val trend: List<Double> = emptyList()
)

enum class HealthStatus {
    CRITICAL, WARNING, STABLE, EXCELLENT
}

object AttendanceCalculator {

    fun calculateStats(records: List<AttendanceRecord>, minRequired: Int = 75): AttendanceStats {
        val validRecords = records.filter { it.status != AttendanceStatus.CANCELLED && it.status != AttendanceStatus.HOLIDAY }
            .sortedBy { it.date }
        
        val total = validRecords.size
        val present = validRecords.count { it.status == AttendanceStatus.PRESENT }
        val absent = validRecords.count { it.status == AttendanceStatus.ABSENT }
        
        val percentage = if (total == 0) 0.0 else (present.toDouble() / total.toDouble()) * 100.0
        
        // Calculate trend points (last 10 records for sparkline)
        val trend = mutableListOf<Double>()
        var runningPresent = 0
        validRecords.forEachIndexed { index, record ->
            if (record.status == AttendanceStatus.PRESENT) runningPresent++
            val runningTotal = index + 1
            trend.add((runningPresent.toDouble() / runningTotal.toDouble()) * 100.0)
        }
        val sparklineTrend = trend.takeLast(10)
        
        val safeBunks = if (percentage >= minRequired && minRequired > 0) {
            // How many more can we miss (total + x) while staying >= minRequired
            // present / (total + x) >= minRequired/100
            // present * 100 / minRequired >= total + x
            // x <= (present * 100 / minRequired) - total
            ((present * 100) / minRequired) - total
        } else 0

        val requiredNext = if (percentage < minRequired && minRequired < 100) {
            val threshold = minRequired / 100.0
            // (present + x) / (total + x) >= threshold
            // present + x >= threshold * total + threshold * x
            // x(1 - threshold) >= threshold * total - present
            ceil((threshold * total - present) / (1.0 - threshold)).toInt()
        } else if (percentage < minRequired && minRequired == 100) {
            // Special case for 100% goal: can never miss a class, but this returns
            // how many consecutive presents needed to reach a point where it's 100%?
            // Actually, if you ever missed one, you can NEVER reach 100% again.
            // Let's just return a large number or something sensible if it's strictly impossible.
            // But mathematically, if absent > 0, x is infinite.
            if (absent > 0) 999 else 0
        } else 0

        val health = when {
            percentage < minRequired -> HealthStatus.CRITICAL
            percentage < minRequired + 5 -> HealthStatus.WARNING
            percentage < 90 -> HealthStatus.STABLE
            else -> HealthStatus.EXCELLENT
        }

        return AttendanceStats(
            percentage = percentage,
            totalLectures = total,
            presentCount = present,
            absentCount = absent,
            safeBunks = safeBunks,
            requiredNextClasses = requiredNext,
            status = health,
            trend = sparklineTrend
        )
    }

    /**
     * Module 8 — Semester Predictor
     * Predicts final percentage based on different attendance scenarios for remaining classes.
     */
    fun predictFinal(
        currentPresent: Int,
        currentTotal: Int,
        remainingClasses: Int,
        attendPercentage: Int // 0-100
    ): Double {
        val willAttend = ceil(remainingClasses * (attendPercentage / 100.0)).toInt()
        val finalPresent = currentPresent + willAttend
        val finalTotal = currentTotal + remainingClasses
        return if (finalTotal == 0) 0.0 else (finalPresent.toDouble() / finalTotal.toDouble()) * 100.0
    }

    /**
     * Module 7 — Bunk Simulator
     * Updates current percentage if 'missCount' more classes are missed.
     */
    fun simulateBunk(currentPresent: Int, currentTotal: Int, missCount: Int): Double {
        val newTotal = currentTotal + missCount
        return if (newTotal == 0) 0.0 else (currentPresent.toDouble() / newTotal.toDouble()) * 100.0
    }
}

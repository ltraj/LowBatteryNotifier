package com.example.lowbatterynotifier.analytics

import com.example.lowbatterynotifier.analytics.db.ChargingSessionEntity
import com.example.lowbatterynotifier.analytics.db.DrainSampleEntity
import kotlin.time.Duration.Companion.milliseconds

data class BatteryInsights(
    val estimatedShutdownText: String,
    val averageDrainPerHourText: String?,
    val averageChargingDurationText: String?,
    val totalChargingSessions: Int,
    val longestChargingText: String?,
    val shortestChargingText: String?,
    val averagePlugInPercentText: String?,
    val averageUnplugPercentText: String?,
    val summary24hText: String?,
    val summary7dText: String?,
    val recentSessions: List<ChargingSessionSummary>,
    val confidence: String,
    val overnightDrainText: String?,
    val sourceLabel: String?,
)

data class ChargingSessionSummary(
    val startPercent: Int,
    val endPercent: Int?,
    val durationText: String,
    val whenText: String,
)

private data class DrainCalculationResult(
    val drainPerHour: Double,
    val segmentCount: Int,
    val timeSpanMs: Long,
    val sourceLabel: String,
)

object InsightsCalculator {

    private const val MIN_SEGMENT_HOURS = 0.05
    private const val MIN_DRAIN_SEGMENTS = 2
    private const val MIN_TOTAL_SPAN_MS = 30 * 60 * 1000L
    private const val MIN_DRAIN_RATE = 0.1

    fun compute(
        currentBatteryPercent: Int,
        drainSamples: List<DrainSampleEntity>,
        chargingSessions: List<ChargingSessionEntity>,
        nowMillis: Long = System.currentTimeMillis(),
        formatDuration: (Long) -> String,
        formatDateTime: (Long) -> String,
        notEnoughData: String,
    ): BatteryInsights {
        val completedSessions = chargingSessions.filter { it.isComplete }
        val drain24h = drainSamples.filter { it.timestampMillis >= (nowMillis - DAY_MS) }
        val drain7d = drainSamples.filter { it.timestampMillis >= (nowMillis - (7 * DAY_MS)) }

        val drainResult = calculateWeightedDrainResult(drain24h, nowMillis) ?: calculateDrainResult(drain24h)
        val drainPerHour = drainResult?.drainPerHour
        val confidence = if (drainResult != null) {
            calculateConfidence(drainResult.segmentCount, drainResult.timeSpanMs)
        } else {
            "Low"
        }
        val shutdownText = formatShutdownEstimate(currentBatteryPercent, drainPerHour, drain24h, notEnoughData, confidence)
        val overnightDrainText = calculateOvernightDrain(drain7d, notEnoughData)
        val sourceLabel = drainResult?.sourceLabel

        val recentSessions = chargingSessions
            .asSequence()
            .take(8)
            .map { session ->
                ChargingSessionSummary(
                    startPercent = session.startPercent,
                    endPercent = session.endPercent,
                    durationText = session.durationMillis?.let(formatDuration) ?: "—",
                    whenText = formatDateTime(session.startTimeMillis),
                )
            }
            .toList()

        return BatteryInsights(
            estimatedShutdownText = shutdownText,
            averageDrainPerHourText = drainPerHour?.let { "%.1f%% / hour".format(it) },
            averageChargingDurationText = averageDuration(completedSessions)?.let(formatDuration),
            totalChargingSessions = completedSessions.size,
            longestChargingText = completedSessions.maxDuration()?.let(formatDuration),
            shortestChargingText = completedSessions.minDuration()?.let(formatDuration),
            averagePlugInPercentText = completedSessions.map { it.startPercent }
                .takeIf { it.isNotEmpty() }?.average()
                ?.let { "%.0f%%".format(it) },
            averageUnplugPercentText = completedSessions.mapNotNull { it.endPercent }
                .takeIf { it.isNotEmpty() }?.average()
                ?.let { "%.0f%%".format(it) },
            summary24hText = buildPeriodSummary(
                drain24h,
                completedSessions.filter {
                    it.startTimeMillis >= (nowMillis - DAY_MS)
                },
                notEnoughData,
            ),
            summary7dText = buildPeriodSummary(
                drain7d,
                completedSessions.filter {
                    it.startTimeMillis >= (nowMillis - (7 * DAY_MS))
                },
                notEnoughData,
            ),
            recentSessions = recentSessions,
            confidence = confidence,
            overnightDrainText = overnightDrainText,
            sourceLabel = sourceLabel,
        )
    }

    private fun calculateDrainResult(samples: List<DrainSampleEntity>): DrainCalculationResult? {
        val unplugged = samples.asSequence().filter { !it.isCharging }.sortedBy { it.timestampMillis }.toList()
        if (unplugged.size < 2) return null

        val segmentRates = mutableListOf<Double>()
        for (index in 1 until unplugged.size) {
            val previous = unplugged[index - 1]
            val current = unplugged[index]
            val hours = (current.timestampMillis - previous.timestampMillis) / 3_600_000.0
            if (hours < MIN_SEGMENT_HOURS) continue
            val drop = previous.batteryPercent - current.batteryPercent
            if (drop > 0) {
                segmentRates.add(drop / hours)
            }
        }

        if (segmentRates.size < MIN_DRAIN_SEGMENTS) return null

        val spanMs = unplugged.last().timestampMillis - unplugged.first().timestampMillis
        if (spanMs < MIN_TOTAL_SPAN_MS) return null

        return DrainCalculationResult(
            drainPerHour = segmentRates.average(),
            segmentCount = segmentRates.size,
            timeSpanMs = spanMs,
            sourceLabel = "Based on last 24 hours",
        )
    }

    private fun calculateWeightedDrainResult(samples: List<DrainSampleEntity>, nowMillis: Long): DrainCalculationResult? {
        val unplugged = samples.asSequence().filter { !it.isCharging }.sortedBy { it.timestampMillis }.toList()
        if (unplugged.size < 2) return null

        val twoHoursAgo = nowMillis - 2 * 60 * 60 * 1000L
        val recent = unplugged.filter { it.timestampMillis >= twoHoursAgo }
        val older = unplugged.filter { it.timestampMillis < twoHoursAgo }

        val recentRate = calculateSimpleDrainRate(recent)
        val olderRate = calculateSimpleDrainRate(older)

        return when {
            recentRate != null && olderRate != null -> {
                val weightedRate = (recentRate * 0.6) + (olderRate * 0.4)
                val totalSegments = (recent.size + older.size)
                val spanMs = unplugged.last().timestampMillis - unplugged.first().timestampMillis
                DrainCalculationResult(
                    drainPerHour = weightedRate,
                    segmentCount = totalSegments,
                    timeSpanMs = spanMs,
                    sourceLabel = "Based on last 2 hours",
                )
            }
            recentRate != null -> {
                val spanMs = recent.last().timestampMillis - recent.first().timestampMillis
                if (spanMs < MIN_TOTAL_SPAN_MS) return null
                DrainCalculationResult(
                    drainPerHour = recentRate,
                    segmentCount = recent.size,
                    timeSpanMs = spanMs,
                    sourceLabel = "Based on last 2 hours",
                )
            }
            else -> null
        }
    }

    private fun calculateSimpleDrainRate(samples: List<DrainSampleEntity>): Double? {
        if (samples.size < 2) return null

        val segmentRates = mutableListOf<Double>()
        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            val hours = (current.timestampMillis - previous.timestampMillis) / 3_600_000.0
            if (hours < MIN_SEGMENT_HOURS) continue
            val drop = previous.batteryPercent - current.batteryPercent
            if (drop > 0) {
                segmentRates.add(drop / hours)
            }
        }

        if (segmentRates.size < MIN_DRAIN_SEGMENTS) return null

        return segmentRates.average()
    }

    fun averageDrainPerHour(samples: List<DrainSampleEntity>): Double? {
        return calculateDrainResult(samples)?.drainPerHour
    }

    private fun calculateConfidence(segmentCount: Int, timeSpanMs: Long): String {
        val days = timeSpanMs / DAY_MS
        return when {
            segmentCount >= 20 && days >= 3 -> "High"
            segmentCount >= 10 && days >= 1 -> "Medium"
            else -> "Low"
        }
    }

    private fun formatShutdownEstimate(
        currentPercent: Int,
        drainPerHour: Double?,
        samples24h: List<DrainSampleEntity>,
        notEnoughData: String,
        confidence: String,
    ): String {
        if (drainPerHour == null || drainPerHour < MIN_DRAIN_RATE) return notEnoughData
        val unplugged = samples24h.filter { !it.isCharging }
        if (unplugged.size < 2) return notEnoughData
        val spanMs = unplugged.last().timestampMillis - unplugged.first().timestampMillis
        if (spanMs < MIN_TOTAL_SPAN_MS) return notEnoughData

        val hoursRemaining = currentPercent / drainPerHour
        if (hoursRemaining.isNaN() || hoursRemaining.isInfinite() || hoursRemaining > 7 * 24) {
            return notEnoughData
        }
        val duration = (hoursRemaining * 3_600_000).toLong().milliseconds
        return "${formatShutdownDuration(duration.inWholeMinutes)} ($confidence confidence)"
    }

    private fun formatShutdownDuration(totalMinutes: Long): String {
        return when {
            totalMinutes < 60 -> "About $totalMinutes min until empty (estimate)"
            totalMinutes < 24 * 60 -> {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                if (mins == 0L) "About $hours h until empty (estimate)"
                else "About ${hours}h ${mins}m until empty (estimate)"
            }
            else -> {
                val days = totalMinutes / (24 * 60)
                val hours = (totalMinutes % (24 * 60)) / 60
                "About ${days}d ${hours}h until empty (estimate)"
            }
        }
    }

    private fun buildPeriodSummary(
        drainSamples: List<DrainSampleEntity>,
        chargingSessions: List<ChargingSessionEntity>,
        notEnoughData: String,
    ): String? {
        if (drainSamples.isEmpty() && chargingSessions.isEmpty()) return null
        val drain = averageDrainPerHour(drainSamples)
        val charges = chargingSessions.size
        return when {
            drain == null && charges == 0 -> notEnoughData
            drain == null -> "$charges charging session(s)"
            charges == 0 -> "Drain ~${"%.1f".format(drain)}%%/h"
            else -> "Drain ~${"%.1f".format(drain)}%%/h · $charges charge(s)"
        }
    }

    private fun averageDuration(sessions: List<ChargingSessionEntity>): Long? {
        val durations = sessions.mapNotNull { it.durationMillis }
        return if (durations.isEmpty()) null else durations.average().toLong()
    }

    private fun List<ChargingSessionEntity>.maxDuration(): Long? =
        mapNotNull { it.durationMillis }.maxOrNull()

    private fun List<ChargingSessionEntity>.minDuration(): Long? =
        mapNotNull { it.durationMillis }.minOrNull()

    private fun calculateOvernightDrain(
        drainSamples: List<DrainSampleEntity>,
        notEnoughData: String,
    ): String {
        val overnightDrains = mutableListOf<Int>()
        val samplesByDay = drainSamples.groupBy { sample ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = sample.timestampMillis
            calendar[java.util.Calendar.HOUR_OF_DAY] = 0
            calendar[java.util.Calendar.MINUTE] = 0
            calendar[java.util.Calendar.SECOND] = 0
            calendar[java.util.Calendar.MILLISECOND] = 0
            calendar.timeInMillis
        }

        for ((dayStartMillis, daySamples) in samplesByDay) {
            if (daySamples.size < 2) continue

            val elevenPM = dayStartMillis + 23 * 60 * 60 * 1000L
            val sevenAM = dayStartMillis + 7 * 60 * 60 * 1000L

            val elevenPMSample = daySamples
                .filter { !it.isCharging }
                .minByOrNull { kotlin.math.abs(it.timestampMillis - elevenPM) }

            val sevenAMSample = daySamples
                .filter { !it.isCharging }
                .minByOrNull { kotlin.math.abs(it.timestampMillis - sevenAM) }

            if (elevenPMSample != null && sevenAMSample != null) {
                val timeDiff = kotlin.math.abs(elevenPMSample.timestampMillis - elevenPM)
                val timeDiffAM = kotlin.math.abs(sevenAMSample.timestampMillis - sevenAM)

                if (timeDiff < 2 * 60 * 60 * 1000L && timeDiffAM < 2 * 60 * 60 * 1000L) {
                    val drain = elevenPMSample.batteryPercent - sevenAMSample.batteryPercent
                    if (drain > 0) {
                        overnightDrains.add(drain)
                    }
                }
            }
        }

        if (overnightDrains.size < 3) return notEnoughData

        val avgDrain = overnightDrains.average()
        return "%.1f%%".format(avgDrain)
    }

    private const val DAY_MS = 24 * 60 * 60 * 1000L
}

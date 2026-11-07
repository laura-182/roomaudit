package com.roomcheck.app.domain

/** A single point on a trend line. */
data class TrendPoint(val timestamp: Long, val score: Int)

enum class TrendDirection { IMPROVING, DECLINING, STEADY, NOT_ENOUGH_DATA }

object Trends {
    /** Compares the most recent score with the average of up to [window] earlier scores. */
    fun direction(points: List<TrendPoint>, window: Int = 3, threshold: Int = 5): TrendDirection {
        val sorted = points.sortedBy { it.timestamp }
        if (sorted.size < 2) return TrendDirection.NOT_ENOUGH_DATA
        val latest = sorted.last().score
        val earlier = sorted.dropLast(1).takeLast(window).map { it.score }.average()
        val delta = latest - earlier
        return when {
            delta >= threshold -> TrendDirection.IMPROVING
            delta <= -threshold -> TrendDirection.DECLINING
            else -> TrendDirection.STEADY
        }
    }

    /**
     * Builds one trend line per category from a flat list of (assessmentTimestamp, category, rating).
     * Categories keep the order in which they first appear in [order] when provided.
     */
    fun byCategory(
        rows: List<Triple<Long, String, Rating>>,
        order: List<String> = emptyList(),
    ): Map<String, List<TrendPoint>> {
        val grouped = rows.groupBy { it.second }.mapValues { (_, list) ->
            list.groupBy { it.first }
                .map { (ts, entries) -> TrendPoint(ts, averageScore(entries.map { it.third }) ?: 0) }
                .sortedBy { it.timestamp }
        }
        val ordered = LinkedHashMap<String, List<TrendPoint>>()
        order.forEach { name -> grouped[name]?.let { ordered[name] = it } }
        grouped.keys.sorted().forEach { name -> if (name !in ordered) ordered[name] = grouped.getValue(name) }
        return ordered
    }
}

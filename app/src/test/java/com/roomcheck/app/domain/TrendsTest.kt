package com.roomcheck.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendsTest {
    @Test
    fun direction() {
        assertEquals(TrendDirection.NOT_ENOUGH_DATA, Trends.direction(listOf(TrendPoint(1, 50))))
        assertEquals(TrendDirection.IMPROVING, Trends.direction(listOf(TrendPoint(1, 40), TrendPoint(2, 80))))
        assertEquals(TrendDirection.DECLINING, Trends.direction(listOf(TrendPoint(2, 40), TrendPoint(1, 80))))
        assertEquals(TrendDirection.STEADY, Trends.direction(listOf(TrendPoint(1, 70), TrendPoint(2, 72))))
    }

    @Test
    fun byCategoryAveragesPerAssessmentAndKeepsOrder() {
        val rows = listOf(
            Triple(1L, "Clutter", Rating.RED),
            Triple(1L, "Clutter", Rating.GREEN),
            Triple(2L, "Clutter", Rating.GREEN),
            Triple(1L, "Cleanliness", Rating.YELLOW),
            Triple(1L, "Old category", Rating.RED),
        )
        val result = Trends.byCategory(rows, order = listOf("Cleanliness", "Clutter"))
        assertEquals(listOf("Cleanliness", "Clutter", "Old category"), result.keys.toList())
        assertEquals(listOf(50, 100), result.getValue("Clutter").map { it.score })
    }

    @Test
    fun scoresAndStatus() {
        assertEquals(null, averageScore(emptyList()))
        assertEquals(50, averageScore(listOf(Rating.RED, Rating.GREEN)))
        assertEquals(Rating.GREEN, Rating.fromScore(75))
        assertEquals(Rating.YELLOW, Rating.fromScore(40))
        assertEquals(Rating.RED, Rating.fromScore(39))
    }

    @Test
    fun templatesHaveUniqueTypesAndItems() {
        val types = DefaultTemplates.all.map { it.type }
        assertEquals(types.size, types.toSet().size)
        DefaultTemplates.starterRooms.forEach { assertTrue(it in types) }
        DefaultTemplates.all.forEach { t -> assertTrue(t.categories.all { it.second.isNotEmpty() }) }
    }
}

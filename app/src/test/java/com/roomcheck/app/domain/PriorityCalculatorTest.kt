package com.roomcheck.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityCalculatorTest {

    private fun r(cat: String, item: String, rating: Rating, id: Long? = null) = RatedItem(id, cat, item, rating)

    @Test
    fun greenItemsAreNotPriorities() {
        val result = PriorityCalculator.compute(listOf(r("Clutter", "Floor", Rating.GREEN)))
        assertTrue(result.isEmpty())
    }

    @Test
    fun redRanksAboveYellow() {
        val result = PriorityCalculator.compute(
            listOf(
                r("Cleanliness", "Mirror", Rating.YELLOW),
                r("Cleanliness", "Toilet", Rating.RED),
                r("Cleanliness", "Sink", Rating.GREEN),
            )
        )
        assertEquals(listOf("Toilet", "Mirror"), result.map { it.item })
    }

    @Test
    fun weakCategoryBreaksTies() {
        val result = PriorityCalculator.compute(
            listOf(
                r("Clutter", "Floor", Rating.RED),
                r("Clutter", "Countertops", Rating.RED),
                r("Cleanliness", "Mirror", Rating.RED),
                r("Cleanliness", "Sink", Rating.GREEN),
                r("Cleanliness", "Toilet", Rating.GREEN),
            )
        )
        assertEquals("Mirror", result.last().item)
        assertTrue(result.first().reasons.any { it.startsWith("Clutter needs attention") })
    }

    @Test
    fun persistentAndWorseningItemsRankHigher() {
        val previous = listOf(
            r("Clutter", "Floor", Rating.RED, 1),
            r("Clutter", "Table", Rating.GREEN, 2),
            r("Clutter", "Shelf", Rating.GREEN, 3),
        )
        val current = listOf(
            r("Clutter", "Floor", Rating.YELLOW, 1), // improved but still not green
            r("Clutter", "Table", Rating.YELLOW, 2), // got worse
            r("Clutter", "Shelf", Rating.YELLOW, 3), // got worse
            r("Clutter", "Couch", Rating.YELLOW, 4), // new item
        )
        val result = PriorityCalculator.compute(current, previous)
        assertEquals("Floor", result[0].item)
        assertTrue(result[0].reasons.contains("Also red last time"))
        assertTrue(result.first { it.item == "Table" }.reasons.contains("Got worse since last check"))
        assertEquals("Couch", result.last().item)
    }

    @Test
    fun previousMatchesByNameWhenIdMissing() {
        val result = PriorityCalculator.compute(
            listOf(r("Clutter", "Floor", Rating.RED)),
            listOf(r("clutter", "floor ", Rating.RED)),
        )
        assertTrue(result.single().reasons.contains("Also red last time"))
    }

    @Test
    fun categorySummaries() {
        val s = PriorityCalculator.categorySummaries(
            listOf(
                r("Clutter", "Floor", Rating.RED),
                r("Clutter", "Table", Rating.GREEN),
                r("Cleanliness", "Sink", Rating.GREEN),
            )
        )
        val clutter = s.first { it.category == "Clutter" }
        assertEquals(50, clutter.score)
        assertEquals(Rating.YELLOW, clutter.status)
        assertEquals(1, clutter.red)
        assertEquals(Rating.GREEN, s.first { it.category == "Cleanliness" }.status)
    }
}

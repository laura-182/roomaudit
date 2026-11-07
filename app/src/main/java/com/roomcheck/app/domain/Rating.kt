package com.roomcheck.app.domain

import kotlin.math.roundToInt

/** Traffic-light status for a single checklist item. */
enum class Rating(val score: Int, val label: String) {
    RED(0, "Needs work"),
    YELLOW(50, "Okay"),
    GREEN(100, "Good");

    companion object {
        /** Converts a 0-100 average score back into an overall status. */
        fun fromScore(score: Int): Rating = when {
            score >= 75 -> GREEN
            score >= 40 -> YELLOW
            else -> RED
        }
    }
}

/** Average score (0-100) of a set of ratings, or null when nothing was rated. */
fun averageScore(ratings: Collection<Rating>): Int? =
    if (ratings.isEmpty()) null else ratings.map { it.score }.average().roundToInt()

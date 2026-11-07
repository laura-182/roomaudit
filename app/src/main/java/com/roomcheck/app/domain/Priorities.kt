package com.roomcheck.app.domain

/** One rated line from an assessment. [itemId] links back to the editable checklist (null if deleted). */
data class RatedItem(
    val itemId: Long?,
    val category: String,
    val item: String,
    val rating: Rating,
)

data class Priority(
    val itemId: Long?,
    val category: String,
    val item: String,
    val rating: Rating,
    /** Higher = more urgent. */
    val score: Int,
    val reasons: List<String>,
)

data class CategorySummary(
    val category: String,
    val score: Int,
    val status: Rating,
    val red: Int,
    val yellow: Int,
    val green: Int,
)

object PriorityCalculator {

    /**
     * Ranks every non-green item from [current]. Ranking considers:
     *  - severity (red before yellow)
     *  - how much of the item's category is struggling
     *  - whether the item was also a problem in the [previous] assessment
     *  - whether it got worse since the [previous] assessment
     */
    fun compute(current: List<RatedItem>, previous: List<RatedItem> = emptyList()): List<Priority> {
        val byCategory = current.groupBy { it.category }
        val previousByKey = previous.associateBy { keyOf(it) }
        val previousById = previous.filter { it.itemId != null }.associateBy { it.itemId }

        return current
            .filter { it.rating != Rating.GREEN }
            .map { rated ->
                val reasons = mutableListOf<String>()
                var score = when (rated.rating) {
                    Rating.RED -> 100
                    Rating.YELLOW -> 50
                    Rating.GREEN -> 0
                }
                reasons += if (rated.rating == Rating.RED) "Rated red" else "Rated yellow"

                val siblings = byCategory[rated.category].orEmpty()
                val struggling = siblings.count { it.rating != Rating.GREEN }
                val weakness = struggling.toDouble() / siblings.size
                score += (weakness * 30).toInt()
                if (siblings.size > 1 && weakness >= 0.5) {
                    reasons += "${rated.category} needs attention ($struggling of ${siblings.size} items)"
                }

                val before = rated.itemId?.let { previousById[it] } ?: previousByKey[keyOf(rated)]
                if (before != null) {
                    if (before.rating.score > rated.rating.score) {
                        score += 15
                        reasons += "Got worse since last check"
                    } else if (before.rating != Rating.GREEN) {
                        score += 20
                        reasons += "Also ${before.rating.name.lowercase()} last time"
                    }
                }

                Priority(rated.itemId, rated.category, rated.item, rated.rating, score, reasons)
            }
            .sortedWith(compareByDescending<Priority> { it.score }.thenBy { it.category }.thenBy { it.item })
    }

    fun categorySummaries(current: List<RatedItem>): List<CategorySummary> =
        current.groupBy { it.category }.map { (category, items) ->
            val score = averageScore(items.map { it.rating }) ?: 0
            CategorySummary(
                category = category,
                score = score,
                status = Rating.fromScore(score),
                red = items.count { it.rating == Rating.RED },
                yellow = items.count { it.rating == Rating.YELLOW },
                green = items.count { it.rating == Rating.GREEN },
            )
        }

    private fun keyOf(r: RatedItem) = r.category.trim().lowercase() + "|" + r.item.trim().lowercase()
}

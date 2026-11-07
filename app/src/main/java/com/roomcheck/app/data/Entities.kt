package com.roomcheck.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.roomcheck.app.domain.Rating

@Entity(tableName = "rooms")
data class HomeRoom(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(HomeRoom::class, ["id"], ["roomId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("roomId")],
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long,
    val name: String,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "items",
    foreignKeys = [ForeignKey(Category::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId")],
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val sortOrder: Int = 0,
)

data class CategoryWithItems(
    @Embedded val category: Category,
    @Relation(parentColumn = "id", entityColumn = "categoryId")
    val items: List<ChecklistItem>,
) {
    val sortedItems: List<ChecklistItem> get() = items.sortedWith(compareBy({ it.sortOrder }, { it.id }))
}

@Entity(
    tableName = "assessments",
    foreignKeys = [ForeignKey(HomeRoom::class, ["id"], ["roomId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("roomId")],
)
data class Assessment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long,
    /** Epoch millis of when the assessment was saved (the date stamp). */
    val timestamp: Long,
    val overallScore: Int,
    val notes: String = "",
)

/**
 * A rating captured in an assessment. Category and item names are copied (snapshotted) so history
 * stays readable even after the user renames or deletes items in the room's checklist.
 */
@Entity(
    tableName = "assessment_ratings",
    foreignKeys = [ForeignKey(Assessment::class, ["id"], ["assessmentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("assessmentId")],
)
data class AssessmentRating(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val itemId: Long?,
    val categoryName: String,
    val itemName: String,
    val rating: Rating,
    val sortOrder: Int = 0,
)

/** Row used for category trend charts. */
data class RatingWithTime(
    val timestamp: Long,
    val categoryName: String,
    val rating: Rating,
)

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long?,
    val roomName: String,
    val title: String,
    val categoryName: String = "",
    val rating: Rating? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val done: Boolean = false,
    val completedAt: Long? = null,
)

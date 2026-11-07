package com.roomcheck.app.data

import android.content.Context
import androidx.room.withTransaction
import com.roomcheck.app.domain.DefaultTemplates
import com.roomcheck.app.domain.Priority
import com.roomcheck.app.domain.Rating
import com.roomcheck.app.domain.RatedItem
import com.roomcheck.app.domain.averageScore

class Repository(private val db: AppDatabase, private val context: Context) {
    private val rooms = db.roomDao()
    private val assessments = db.assessmentDao()
    private val todos = db.todoDao()

    // ---- Rooms & checklists -------------------------------------------------------------------

    fun observeRooms() = rooms.observeRooms()
    fun observeRoom(id: Long) = rooms.observeRoom(id)
    fun observeChecklist(roomId: Long) = rooms.observeChecklist(roomId)
    suspend fun getChecklist(roomId: Long) = rooms.getChecklist(roomId)

    /** Seeds a few starter rooms the very first time the app runs. */
    suspend fun seedIfFirstLaunch() {
        val prefs = context.getSharedPreferences("roomcheck", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return
        DefaultTemplates.starterRooms.forEach { addRoom(it, it) }
        prefs.edit().putBoolean("seeded", true).apply()
    }

    suspend fun addRoom(name: String, templateType: String): Long = db.withTransaction {
        val template = DefaultTemplates.forType(templateType)
        val roomId = rooms.insertRoom(
            HomeRoom(name = name.trim(), type = template.type, sortOrder = rooms.nextRoomOrder())
        )
        template.categories.forEachIndexed { ci, (categoryName, items) ->
            val categoryId = rooms.insertCategory(Category(roomId = roomId, name = categoryName, sortOrder = ci))
            items.forEachIndexed { ii, itemName ->
                rooms.insertItem(ChecklistItem(categoryId = categoryId, name = itemName, sortOrder = ii))
            }
        }
        roomId
    }

    suspend fun renameRoom(room: HomeRoom, name: String) = rooms.updateRoom(room.copy(name = name.trim()))
    suspend fun deleteRoom(room: HomeRoom) = rooms.deleteRoom(room)

    /** Wipes the room's checklist and rebuilds it from its type's default template. */
    suspend fun resetChecklist(room: HomeRoom) = db.withTransaction {
        rooms.getChecklist(room.id).forEach { rooms.deleteCategory(it.category) }
        DefaultTemplates.forType(room.type).categories.forEachIndexed { ci, (categoryName, items) ->
            val categoryId = rooms.insertCategory(Category(roomId = room.id, name = categoryName, sortOrder = ci))
            items.forEachIndexed { ii, itemName ->
                rooms.insertItem(ChecklistItem(categoryId = categoryId, name = itemName, sortOrder = ii))
            }
        }
    }

    suspend fun addCategory(roomId: Long, name: String) {
        val order = (rooms.getChecklist(roomId).maxOfOrNull { it.category.sortOrder } ?: -1) + 1
        rooms.insertCategory(Category(roomId = roomId, name = name.trim(), sortOrder = order))
    }

    suspend fun renameCategory(category: Category, name: String) =
        rooms.updateCategories(listOf(category.copy(name = name.trim())))

    suspend fun deleteCategory(category: Category) = rooms.deleteCategory(category)

    /** Moves a category up (-1) or down (+1) within its room. */
    suspend fun moveCategory(roomId: Long, categoryId: Long, delta: Int) = db.withTransaction {
        val list = rooms.getChecklist(roomId).map { it.category }.toMutableList()
        val index = list.indexOfFirst { it.id == categoryId }
        val target = index + delta
        if (index < 0 || target !in list.indices) return@withTransaction
        list.add(target, list.removeAt(index))
        rooms.updateCategories(list.mapIndexed { i, c -> c.copy(sortOrder = i) })
    }

    suspend fun addItem(category: CategoryWithItems, name: String) {
        val order = (category.items.maxOfOrNull { it.sortOrder } ?: -1) + 1
        rooms.insertItem(ChecklistItem(categoryId = category.category.id, name = name.trim(), sortOrder = order))
    }

    suspend fun renameItem(item: ChecklistItem, name: String) =
        rooms.updateItems(listOf(item.copy(name = name.trim())))

    suspend fun deleteItem(item: ChecklistItem) = rooms.deleteItem(item)

    suspend fun moveItem(category: CategoryWithItems, itemId: Long, delta: Int) {
        val list = category.sortedItems.toMutableList()
        val index = list.indexOfFirst { it.id == itemId }
        val target = index + delta
        if (index < 0 || target !in list.indices) return
        list.add(target, list.removeAt(index))
        rooms.updateItems(list.mapIndexed { i, item -> item.copy(sortOrder = i) })
    }

    // ---- Assessments --------------------------------------------------------------------------

    fun observeAssessments(roomId: Long) = assessments.observeForRoom(roomId)
    fun observeLatestPerRoom() = assessments.observeLatestPerRoom()
    fun observeAssessment(id: Long) = assessments.observeAssessment(id)
    fun observeRatings(assessmentId: Long) = assessments.observeRatings(assessmentId)
    fun observeRatingHistory(roomId: Long) = assessments.observeRatingHistory(roomId)

    /** Ratings from the most recent assessment, keyed by checklist item id (for "copy last time"). */
    suspend fun latestRatingsByItem(roomId: Long): Map<Long, Rating> {
        val latest = assessments.getLatest(roomId) ?: return emptyMap()
        return assessments.getRatings(latest.id)
            .mapNotNull { r -> r.itemId?.let { it to r.rating } }
            .toMap()
    }

    /** Saves a date-stamped assessment and returns its id. Only rated items are stored. */
    suspend fun saveAssessment(roomId: Long, ratings: Map<Long, Rating>, notes: String): Long =
        db.withTransaction {
            val checklist = rooms.getChecklist(roomId)
            val rows = mutableListOf<AssessmentRating>()
            var order = 0
            checklist.forEach { cwi ->
                cwi.sortedItems.forEach { item ->
                    ratings[item.id]?.let { rating ->
                        rows += AssessmentRating(
                            assessmentId = 0,
                            itemId = item.id,
                            categoryName = cwi.category.name,
                            itemName = item.name,
                            rating = rating,
                            sortOrder = order++,
                        )
                    }
                }
            }
            val score = averageScore(rows.map { it.rating }) ?: 0
            val id = assessments.insertAssessment(
                Assessment(roomId = roomId, timestamp = System.currentTimeMillis(), overallScore = score, notes = notes.trim())
            )
            assessments.insertRatings(rows.map { it.copy(assessmentId = id) })
            id
        }

    suspend fun deleteAssessment(id: Long) = assessments.deleteAssessment(id)

    /** Ratings of the assessment that came before [assessment] in the same room (for trend-aware priorities). */
    suspend fun previousRatings(assessment: Assessment): List<RatedItem> {
        val previous = assessments.getPrevious(assessment.roomId, assessment.timestamp) ?: return emptyList()
        return assessments.getRatings(previous.id).map { it.toRatedItem() }
    }

    // ---- To-do list ---------------------------------------------------------------------------

    fun observeTodos() = todos.observeAll()

    /** Adds priorities to the to-do list, skipping ones that are already open for the same room. Returns count added. */
    suspend fun addPrioritiesToTodo(room: HomeRoom, priorities: List<Priority>): Int {
        val open = todos.getOpen()
            .filter { it.roomId == room.id }
            .map { it.categoryName.lowercase() to it.title.lowercase() }
            .toSet()
        val newItems = priorities
            .filter { (it.category.lowercase() to it.item.lowercase()) !in open }
            .map {
                TodoItem(
                    roomId = room.id,
                    roomName = room.name,
                    title = it.item,
                    categoryName = it.category,
                    rating = it.rating,
                )
            }
        todos.insert(newItems)
        return newItems.size
    }

    suspend fun addCustomTodo(title: String, roomName: String) =
        todos.insert(listOf(TodoItem(roomId = null, roomName = roomName.trim(), title = title.trim())))

    suspend fun setTodoDone(item: TodoItem, done: Boolean) =
        todos.update(item.copy(done = done, completedAt = if (done) System.currentTimeMillis() else null))

    suspend fun deleteTodo(item: TodoItem) = todos.delete(item)
    suspend fun clearCompletedTodos() = todos.clearCompleted()
}

fun AssessmentRating.toRatedItem() = RatedItem(itemId, categoryName, itemName, rating)

package com.roomcheck.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY sortOrder, id")
    fun observeRooms(): Flow<List<HomeRoom>>

    @Query("SELECT * FROM rooms WHERE id = :id")
    fun observeRoom(id: Long): Flow<HomeRoom?>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getRoom(id: Long): HomeRoom?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM rooms")
    suspend fun nextRoomOrder(): Int

    @Insert suspend fun insertRoom(room: HomeRoom): Long
    @Update suspend fun updateRoom(room: HomeRoom)
    @Delete suspend fun deleteRoom(room: HomeRoom)

    @Transaction
    @Query("SELECT * FROM categories WHERE roomId = :roomId ORDER BY sortOrder, id")
    fun observeChecklist(roomId: Long): Flow<List<CategoryWithItems>>

    @Transaction
    @Query("SELECT * FROM categories WHERE roomId = :roomId ORDER BY sortOrder, id")
    suspend fun getChecklist(roomId: Long): List<CategoryWithItems>

    @Insert suspend fun insertCategory(category: Category): Long
    @Update suspend fun updateCategories(categories: List<Category>)
    @Delete suspend fun deleteCategory(category: Category)

    @Insert suspend fun insertItem(item: ChecklistItem): Long
    @Update suspend fun updateItems(items: List<ChecklistItem>)
    @Delete suspend fun deleteItem(item: ChecklistItem)
}

@Dao
interface AssessmentDao {
    @Insert suspend fun insertAssessment(assessment: Assessment): Long
    @Insert suspend fun insertRatings(ratings: List<AssessmentRating>)

    @Query("DELETE FROM assessments WHERE id = :id")
    suspend fun deleteAssessment(id: Long)

    @Query("SELECT * FROM assessments WHERE roomId = :roomId ORDER BY timestamp DESC")
    fun observeForRoom(roomId: Long): Flow<List<Assessment>>

    /** The newest assessment for every room. */
    @Query(
        """SELECT a.* FROM assessments a
           WHERE a.timestamp = (SELECT MAX(b.timestamp) FROM assessments b WHERE b.roomId = a.roomId)"""
    )
    fun observeLatestPerRoom(): Flow<List<Assessment>>

    @Query("SELECT * FROM assessments WHERE id = :id")
    fun observeAssessment(id: Long): Flow<Assessment?>

    @Query("SELECT * FROM assessment_ratings WHERE assessmentId = :assessmentId ORDER BY sortOrder, id")
    fun observeRatings(assessmentId: Long): Flow<List<AssessmentRating>>

    @Query("SELECT * FROM assessment_ratings WHERE assessmentId = :assessmentId ORDER BY sortOrder, id")
    suspend fun getRatings(assessmentId: Long): List<AssessmentRating>

    @Query(
        """SELECT * FROM assessments WHERE roomId = :roomId AND timestamp < :before
           ORDER BY timestamp DESC LIMIT 1"""
    )
    suspend fun getPrevious(roomId: Long, before: Long): Assessment?

    @Query("SELECT * FROM assessments WHERE roomId = :roomId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(roomId: Long): Assessment?

    @Query(
        """SELECT a.timestamp AS timestamp, r.categoryName AS categoryName, r.rating AS rating
           FROM assessment_ratings r INNER JOIN assessments a ON r.assessmentId = a.id
           WHERE a.roomId = :roomId ORDER BY a.timestamp"""
    )
    fun observeRatingHistory(roomId: Long): Flow<List<RatingWithTime>>
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY done, createdAt DESC")
    fun observeAll(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todos WHERE done = 0")
    suspend fun getOpen(): List<TodoItem>

    @Insert suspend fun insert(items: List<TodoItem>)
    @Update suspend fun update(item: TodoItem)
    @Delete suspend fun delete(item: TodoItem)

    @Query("DELETE FROM todos WHERE done = 1")
    suspend fun clearCompleted()
}

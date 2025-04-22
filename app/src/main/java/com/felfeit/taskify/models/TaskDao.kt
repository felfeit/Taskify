package com.felfeit.taskify.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Data Access Object (DAO) for Database Operations & Queries
@Dao
interface TaskDao {

    // Update existing task or Insert new task
    @Upsert
    suspend fun saveTask(tasks: Task)

    // Get all tasks ordered by priority
    @Query("SELECT * FROM tasks ORDER BY CASE priority WHEN 'High' THEN 1 WHEN 'Medium' THEN 2 ELSE 3 END")
    fun getAllTasks(): Flow<List<Task>>

    // Delete a task
    @Delete
    suspend fun deleteTask(task: Task)

    // Search tasks
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<Task>>
}
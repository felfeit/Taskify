package com.felfeit.taskify.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = REPLACE)
    suspend fun addTask(tasks: Task)

    @Query("SELECT * FROM tasks ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END")
    fun getAllTasks(): Flow<List<Task>>

    @Update(onConflict = REPLACE)
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
package com.felfeit.taskify.repositories

import com.felfeit.taskify.models.TaskDao
import com.felfeit.taskify.models.Task
import kotlinx.coroutines.flow.Flow

/**
 * Mediates data between datasources (DB/API) and ViewModel
 *
 * @param dao Data Access Object for Database Operations
 */
class TaskRepository(private val dao: TaskDao) {
    val allTasks: Flow<List<Task>> = dao.getAllTasks()
    suspend fun saveTask(task: Task) = dao.saveTask(task)
    suspend fun deleteTask(task: Task) = dao.deleteTask(task)
    fun searchTasks(query: String) : Flow<List<Task>> {
        return if(query.isBlank()) {
            dao.getAllTasks()
        } else {
            dao.searchTasks(query)
        }
    }
}
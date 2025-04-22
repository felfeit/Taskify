package com.felfeit.taskify.repositories

import com.felfeit.taskify.models.TaskDao
import com.felfeit.taskify.models.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    suspend fun addTask(task: Task) = taskDao.addTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
}
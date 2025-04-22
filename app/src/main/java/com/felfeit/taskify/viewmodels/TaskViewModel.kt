package com.felfeit.taskify.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.felfeit.taskify.models.TaskDatabase
import com.felfeit.taskify.models.Task
import com.felfeit.taskify.repositories.TaskRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    // [1] Gunakan backing property untuk LiveData
    private val _allTasks: LiveData<List<Task>>
    val allTasks: LiveData<List<Task>> get() = _allTasks

    private val repository: TaskRepository

    init {
        val dao = TaskDatabase.getInstance(application).dao()
        repository = TaskRepository(dao)

        // [2] Transformasi Flow ke LiveData sekaligus
        _allTasks = repository.allTasks
            .catch { e -> Log.e("TaskViewModel", "Error loading tasks", e) }
            .asLiveData(viewModelScope.coroutineContext)
    }

    // [3] Tambahkan error handling dan konfirmasi operasi
    private val _operationStatus = MutableSharedFlow<OperationStatus>()
    val operationStatus: SharedFlow<OperationStatus> = _operationStatus

    fun addTask(title: String, priority: String) = viewModelScope.launch {
        try {
            val task = Task(title = title, priority = priority)
            repository.addTask(task)
            _operationStatus.emit(OperationStatus.SUCCESS)
        } catch (e: Exception) {
            _operationStatus.emit(OperationStatus.ERROR(e.message ?: "Unknown error"))
            Log.e("TaskViewModel", "Add task failed", e)
        }
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        try {
            repository.updateTask(task)
            _operationStatus.emit(OperationStatus.SUCCESS)
        } catch (e: Exception) {
            _operationStatus.emit(OperationStatus.ERROR("Update failed: ${e.localizedMessage}"))
        }
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        try {
            repository.deleteTask(task)
            _operationStatus.emit(OperationStatus.SUCCESS)
        } catch (e: Exception) {
            _operationStatus.emit(OperationStatus.ERROR("Delete failed"))
        }
    }

    // [4] Sealed class untuk status operasi
    sealed class OperationStatus {
        object SUCCESS : OperationStatus()
        data class ERROR(val message: String) : OperationStatus()
    }
}
package com.felfeit.taskify.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.felfeit.taskify.models.TaskDatabase
import com.felfeit.taskify.models.Task
import com.felfeit.taskify.repositories.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    //  Private MutableLiveData to manage the data
    private val _allTasks = MutableLiveData<List<Task>>()

    //  Public LiveData to expose read-only access
    val allTasks: LiveData<List<Task>> get() = _allTasks

    //  Data Sources
    private val repository: TaskRepository

    //  Operation Status
    private val _operationStatus = MutableSharedFlow<OperationStatus>()
    val operationStatus: SharedFlow<OperationStatus> = _operationStatus

    private val _searchQuery = MutableStateFlow("")
    val searchResults: Flow<List<Task>> = _searchQuery
        .debounce(300) // Delay 300ms untuk anti-spam
        .distinctUntilChanged()
        .flatMapLatest { query ->
            repository.searchTasks(query)
        }

    init {
        val dao = TaskDatabase.getInstance(application).dao()
        repository = TaskRepository(dao)
        loadTasks()
    }

    // Method to save task (update and insert)
    fun saveTask(task: Task) = viewModelScope.launch {
        try {
            repository.saveTask(task)
            _operationStatus.emit(OperationStatus.SUCCESS)
            loadTasks() // Refresh data
        } catch (e: Exception) {
            _operationStatus.emit(OperationStatus.ERROR("Gagal menyimpan: ${e.message}"))
        }
    }

    // Method to delete single task
    fun deleteTask(task: Task) = viewModelScope.launch {
        try {
            repository.deleteTask(task)
            _operationStatus.emit(OperationStatus.SUCCESS)
            loadTasks()
        } catch (e: Exception) {
            _operationStatus.emit(OperationStatus.ERROR("Gagal menghapus"))
        }
    }

    // Method to load all tasks
    private fun loadTasks() = viewModelScope.launch {
        repository.allTasks.collect { tasks ->
            _allTasks.postValue(tasks)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Sealed class for representing UI State
    sealed class OperationStatus {
        object SUCCESS : OperationStatus()
        data class ERROR(val message: String) : OperationStatus()
    }
}
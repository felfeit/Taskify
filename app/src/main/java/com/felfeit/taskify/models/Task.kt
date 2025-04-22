package com.felfeit.taskify.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data Class representing a Task Entity for Room Database
 *
 * @property id Unique identifier (auto-generated)
 * @property title Task title (required)
 * @property isCompleted Completion status (default: false)
 * @property priority Priority level ("High", "Medium", "Low") (default: "Medium")
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: String = "Medium"
)

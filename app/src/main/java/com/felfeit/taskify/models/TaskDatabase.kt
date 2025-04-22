package com.felfeit.taskify.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Room Database instance for application
@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun dao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

//        Singleton Pattern to get instance of TaskDatabase
        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context,
                        TaskDatabase::class.java,
                        "task_database"
                    ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
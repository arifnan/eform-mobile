package com.example.eform.data.database

import android.content.Context
import androidx.room.Room

// Kelas ini akan mengelola instansiasi RoomDatabase
class DatabaseProvider(private val context: Context) {

    // Singleton pattern untuk memastikan hanya ada satu instance database
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "eform_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
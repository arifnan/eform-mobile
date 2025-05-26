package com.example.eform.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.eform.data.model.AnswerEntity
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.FormResponseEntity
import com.example.eform.data.model.QuestionEntity
import com.example.eform.data.model.UserEntity
import com.example.eform.data.model.UserFavoriteFormEntity
import com.example.eform.data.model.NotificationEntity // <<< --- IMPORT ENTITAS BARU
import com.example.eform.ui.form.components.Converters

@Database(
    entities = [
        FormEntity::class, QuestionEntity::class, UserEntity::class,
        UserFavoriteFormEntity::class, NotificationEntity::class,
        FormResponseEntity::class, AnswerEntity::class // <<< --- TAMBAHKAN ENTITAS BARU
    ],
    version = 6, // <<< --- NAIKKAN VERSI DATABASE (misal dari 5 ke 6)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao
    abstract fun questionDao(): QuestionDao
    abstract fun userDao(): UserDao
    abstract fun userFavoriteFormDao(): UserFavoriteFormDao
    abstract fun notificationDao(): NotificationDao
    abstract fun formResponseDao(): FormResponseDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eform_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
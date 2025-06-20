package com.example.eform.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.eform.data.model.AnswerEntity
import com.example.eform.data.model.DraftAnswerEntity
import com.example.eform.data.model.FormDraftEntity
import com.example.eform.data.model.FormEntity
import com.example.eform.data.model.FormResponseEntity
import com.example.eform.data.model.QuestionEntity
import com.example.eform.data.model.UserEntity
import com.example.eform.data.model.UserFavoriteFormEntity
import com.example.eform.data.model.NotificationEntity
import com.example.eform.ui.form.components.Converters

@Database(
    entities = [
        FormEntity::class,
        QuestionEntity::class,
        UserEntity::class,
        UserFavoriteFormEntity::class,
        NotificationEntity::class,
        FormResponseEntity::class,
        AnswerEntity::class,
        FormDraftEntity::class,
        DraftAnswerEntity::class
    ],
    version = 7, // <<< --- NAIKKAN VERSI DATABASE (misal dari 5 ke 6)
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
    abstract fun draftDao(): DraftDao
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
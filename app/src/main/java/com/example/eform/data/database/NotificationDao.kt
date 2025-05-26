package com.example.eform.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.eform.data.model.NotificationEntity

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getNotificationsForUser(userId: Int): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId")
    suspend fun getNotificationCountForUser(userId: Int): Int

    // Ambil notifikasi tertua untuk pengguna tertentu
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldestNotificationForUser(userId: Int): NotificationEntity?

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Int)

    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAllNotificationsForUser(userId: Int) // Opsional, untuk menghapus semua notif pengguna
}
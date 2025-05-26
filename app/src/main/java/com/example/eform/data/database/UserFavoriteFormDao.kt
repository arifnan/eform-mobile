package com.example.eform.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.eform.data.model.UserFavoriteFormEntity

@Dao
interface UserFavoriteFormDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Abaikan jika sudah ada
    suspend fun addFavorite(favorite: UserFavoriteFormEntity)

    @Delete
    suspend fun removeFavorite(favorite: UserFavoriteFormEntity)

    @Query("SELECT * FROM user_favorite_forms WHERE userId = :userId AND formId = :formId")
    suspend fun getFavoriteStatus(userId: Int, formId: Int): UserFavoriteFormEntity?

    @Query("SELECT formId FROM user_favorite_forms WHERE userId = :userId")
    suspend fun getFavoriteFormIdsByUserId(userId: Int): List<Int>
}
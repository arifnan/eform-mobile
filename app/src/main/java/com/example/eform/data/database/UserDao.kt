package com.example.eform.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.eform.data.model.UserEntity

//@Dao
//interface UserDao {
//    // Menyimpan user baru
//    @Insert
//    suspend fun insert(user: UserEntity)
//
//    // Mengambil user berdasarkan ID
//    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
//    suspend fun getUserById(id: Int): UserEntity?
//
//    // Mengupdate data user
//    @Update
//    suspend fun update(user: UserEntity)
//
//    // Menghapus user
//    @Query("DELETE FROM user WHERE id = :id")
//    suspend fun delete(id: Int)
//}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser (user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE nip = :nip LIMIT 1")
    suspend fun getUserByNip(nip: String): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity) // Tambahkan fungsi update
}
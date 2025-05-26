package com.example.eform.data.repository

//import com.example.eform.data.database.UserDao
//import com.example.eform.data.model.UserEntity
////import com.example.eform.data.api.RetrofitInstance
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//class UserRepository(private val userDao: UserDao) {
//
//    // Register user secara lokal menggunakan Room
//    suspend fun registerUser(user: UserEntity) {
//        // Insert user ke dalam database lokal (Room)
//        userDao.insert(user)
//    }
//
//    // Login dengan mencari user berdasarkan email di database lokal
//    suspend fun loginUser(email: String): UserEntity? {
//        // Cek apakah ada user dengan email yang cocok
//        return userDao.getUserByEmail(email)
//    }
//
//    // Update user profile secara lokal
//    suspend fun updateUserProfile(user: UserEntity) {
//        userDao.update(user)
//    }
//
////    // Register user di server API (Laravel) dan kemudian di simpan secara lokal
////    suspend fun registerUserOnServer(user: UserEntity) {
////        // Simulate API call, Anda dapat memanggil API untuk registrasi
////        try {
////            val response = RetrofitInstance.api.registerUser(user)
////            if (response.isSuccessful) {
////                // Jika berhasil, simpan data user di database lokal
////                registerUser(user)
////            }
////        } catch (e: Exception) {
////            // Handle error jika ada masalah dengan koneksi API
////        }
////    }
//}

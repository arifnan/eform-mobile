package com.example.eform.ui.viewmodel

//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.eform.data.model.UserEntity
//import com.example.eform.data.repository.UserRepository
//import kotlinx.coroutines.launch
//
//class UserViewModel(private val repository: UserRepository) : ViewModel() {
//
//    // Fungsi untuk registrasi user
//    fun registerUser(user: UserEntity) {
//        viewModelScope.launch {
//            repository.registerUser(user)
//        }
//    }
//
//    // Fungsi untuk login user
//    fun loginUser(email: String, onResult: (UserEntity?) -> Unit) {
//        viewModelScope.launch {
//            val user = repository.loginUser(email)
//            onResult(user)
//        }
//    }
//
//    // Fungsi untuk update profil user
//    fun updateUserProfile(user: UserEntity) {
//        viewModelScope.launch {
//            repository.updateUserProfile(user)
//        }
//    }
//}

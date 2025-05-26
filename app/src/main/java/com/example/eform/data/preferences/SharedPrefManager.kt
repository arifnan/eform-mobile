package com.example.eform.data.preferences

//import android.content.Context
//import android.content.SharedPreferences
//
//object SharedPrefManager {
//    private const val PREFS_NAME = "eform_prefs"
//    private const val KEY_TOKEN = "token"
//
//    private fun getPreferences(context: Context): SharedPreferences {
//        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//    }
//
//    fun saveToken(context: Context, token: String) {
//        val editor = getPreferences(context).edit()
//        editor.putString(KEY_TOKEN, token)
//        editor.apply()
//    }
//
//    fun getToken(context: Context): String? {
//        return getPreferences(context).getString(KEY_TOKEN, null)
//    }
//
//    fun clearToken(context: Context) {
//        val editor = getPreferences(context).edit()
//        editor.remove(KEY_TOKEN)
//        editor.apply()
//    }
//}

package com.pakdrive

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("DriverUidPref", Context.MODE_PRIVATE)

    fun putValue(key: String, value: String) {
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getValue(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun deleteValue(key: String) {
        val editor = prefs.edit()
        editor.remove(key)
        editor.apply()
    }
}
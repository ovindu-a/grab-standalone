package com.example.grabstandalone

import android.content.Context
import android.text.TextUtils

object SmartWearDataStore {

    private const val PREFS_NAME = "grab_prefs"
    private const val KEY_BT_ADDRESS = "bt_address"
    private const val DEFAULT_BT_ADDRESS = "0:0:0:0"

    var btAddress: String = DEFAULT_BT_ADDRESS
        get() = if (TextUtils.isEmpty(field)) DEFAULT_BT_ADDRESS else field

    fun saveBtAddress(context: Context, address: String) {
        btAddress = address
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_BT_ADDRESS, address).apply()
    }

    fun loadBtAddress(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BT_ADDRESS, "") ?: ""
        if (saved.isNotEmpty()) btAddress = saved
        return saved
    }
}

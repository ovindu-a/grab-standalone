package com.example.grabstandalone

import android.content.Context
import android.text.TextUtils

object SmartWearDataStore {

    private const val PREFS_NAME = "grab_prefs"
    private const val KEY_BT_ADDRESS = "bt_address"
    private const val KEY_IS_PROD = "is_prod"
    private const val DEFAULT_BT_ADDRESS = "0:0:0:0"

    const val PROD_NUMBER    = "+6590855038"
    const val SANDBOX_NUMBER = "+6585066005"

    var btAddress: String = DEFAULT_BT_ADDRESS
        get() = if (TextUtils.isEmpty(field)) DEFAULT_BT_ADDRESS else field

    var isProd: Boolean = true

    fun activeNumber(): String = if (isProd) PROD_NUMBER else SANDBOX_NUMBER

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

    fun saveMode(context: Context, prod: Boolean) {
        isProd = prod
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_PROD, prod).apply()
    }

    fun loadMode(context: Context) {
        isProd = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_PROD, true)
    }
}

package com.ladsers.passtable.android

import android.content.Context

enum class ParamKey(
    val str: String,
    val defBool: Boolean = false,
    val defInt: Int = 0,
    val defStr: String = ""
) {
    INITIAL_SETUP("initialSetup", defBool = true),

    UI_SHOW_CARD_AFTER_CHANGES("uiShowCardAfterChanges", defBool = true),
    UI_CHECK_REMEMBER_PASSWORD_BY_DEFAULT("uiCheckRememberPasswordByDefault", defBool = false),

    SECURITY_REREQUEST_PASSWORD_MODE("securityRerequestPasswordMode", defInt = 0),
    SECURITY_REREQUEST_PASSWORD_SECS("securityRerequestPasswordSecs", defInt = 120),
    SECURITY_SHOW_PASSWORD_IN_CARD("securityShowPasswordInCard", defBool = false),

    RECENT_FILES_MAX_ITEMS("recentFilesMaxItems", defInt = 20)
}

object ParamsContainer {

    fun set(context: Context?, key: ParamKey, value: Boolean): Boolean {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return false
        shPref.edit().putBoolean(key.str, value).apply()
        return true
    }

    fun set(context: Context?, key: ParamKey, value: Int): Boolean {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return false
        shPref.edit().putInt(key.str, value).apply()
        return true
    }

    fun set(context: Context?, key: ParamKey, value: String): Boolean {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return false
        shPref.edit().putString(key.str, value).apply()
        return true
    }

    fun getBool(context: Context?, key: ParamKey): Boolean? {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return null
        return shPref.getBoolean(key.str, key.defBool)
    }

    fun getInt(context: Context?, key: ParamKey): Int? {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return null
        return shPref.getInt(key.str, key.defInt)
    }

    fun getStr(context: Context?, key: ParamKey): String? {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return null
        return shPref.getString(key.str, key.defStr)
    }


}
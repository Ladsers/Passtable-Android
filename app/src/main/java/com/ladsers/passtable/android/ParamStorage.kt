package com.ladsers.passtable.android

import android.content.Context

enum class Param(
    val str: String,
    val defBool: Boolean = false,
    val defInt: Int = 0,
    val defStr: String = ""
) {
    INITIAL_SETUP("initialSetup", defBool = true),

    CHECKBOX_REMEMBER_PASSWORD_BY_DEFAULT("checkboxRememberPasswordByDefault", defBool = true),
    LOCK_MODE("lockMode", defInt = 0),
    LOCK_SECS("lockSecs", defInt = 120),
    LOCK_ALLOW_WHEN_EDITING("lockAllowWhenEditing", defBool = true),
    REMEMBER_RECENT_FILES("rememberRecentFiles", defBool = true),
    THEME("theme", defInt = 0),
    PREVENT_SCREEN_CAPTURE("preventScreenCapture", defBool = true)
}

object ParamStorage {

    fun set(context: Context?, param: Param, value: Boolean): Boolean {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return false
        shPref.edit().putBoolean(param.str, value).apply()
        return true
    }

    fun set(context: Context?, param: Param, value: Int): Boolean {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return false
        shPref.edit().putInt(param.str, value).apply()
        return true
    }

    fun set(context: Context?, param: Param, value: String): Boolean {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return false
        shPref.edit().putString(param.str, value).apply()
        return true
    }

    fun getBool(context: Context?, param: Param): Boolean {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return param.defBool
        return shPref.getBoolean(param.str, param.defBool)
    }

    fun getInt(context: Context?, param: Param): Int {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return param.defInt
        return shPref.getInt(param.str, param.defInt)
    }

    fun getStr(context: Context?, param: Param): String {
        val shPref = context?.getSharedPreferences("paramsContainer", Context.MODE_PRIVATE)
            ?: return param.defStr
        return shPref.getString(param.str, param.defStr)!!
    }


}
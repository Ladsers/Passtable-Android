package com.ladsers.passtable.android.containers

import android.content.Context

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
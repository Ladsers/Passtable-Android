package com.ladsers.passtable.android

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri

object RecentFiles {

    fun add(context: Context?, data: Uri): Boolean {
        return edit(context,data,true)
    }

    fun remove(context: Context?, data: Uri): Boolean {
        return edit(context,data,false)
    }

    private fun edit(context: Context?, data: Uri, isAdding: Boolean): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        val str = shPref.getString("uri", "")
        val uriList = if (!str.isNullOrEmpty()) {
            val strList = str.split("|")
            strList.map { it.toUri() }.toMutableList()
        } else mutableListOf()
        val maxItems = shPref.getInt("maxItems", 5)

        uriList.remove(data)
        if (isAdding) {
            uriList.add(data)
            if (uriList.size > maxItems) uriList.removeAt(0)
        }

        val newStr = uriList.joinToString("|") { it.toString() }
        with(shPref.edit()) {
            putString("uri", newStr)
            apply()
        }
        return true
    }

    fun load(context: Context?): MutableList<Uri> {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return mutableListOf()

        val str = shPref.getString("uri", "")
        if (str.isNullOrEmpty()) return mutableListOf()
        val strList = str.split("|")
        return strList.map { it.toUri() }.reversed().toMutableList()
    }

    fun clear(context: Context?): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        with(shPref.edit()) {
            putString("uri", "")
            apply()
        }
        return true
    }
}
package com.ladsers.passtable.android

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.text.SimpleDateFormat
import java.util.*

object RecentFiles {

    fun add(context: Context?, data: Uri): Boolean {
        return edit(context, data, true)
    }

    fun remove(context: Context?, data: Uri): Boolean {
        return edit(context, data, false)
    }

    private fun edit(context: Context?, data: Uri, isAdding: Boolean): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        val strUri = shPref.getString("uri", "")
        val uriList = if (!strUri.isNullOrEmpty()) {
            val strList = strUri.split("|")
            strList.map { it.toUri() }.toMutableList()
        } else mutableListOf()

        val strDate = shPref.getString("date", "")
        val dateList = if (!strDate.isNullOrEmpty()) {
            strDate.split("|").toMutableList()
        } else mutableListOf()

        val strMpEncrypted = shPref.getString("mpEncrypted", "")
        val mpEncryptedList = if (!strMpEncrypted.isNullOrEmpty()) {
            strMpEncrypted.split("|").toMutableList()
        } else mutableListOf()

        var currentMpEncrypted = " "
        val index = uriList.indexOf(data)
        if (index != -1) {
            uriList.removeAt(index)
            dateList.removeAt(index)

            currentMpEncrypted = mpEncryptedList[index]
            mpEncryptedList.removeAt(index)
        }

        if (isAdding) {
            uriList.add(data)
            dateList.add(getCurrentDateAsStr())
            mpEncryptedList.add(currentMpEncrypted)
            val maxItems = shPref.getInt("maxItems", 15)
            if (uriList.size > maxItems) {
                uriList.removeAt(0)
                dateList.removeAt(0)
                mpEncryptedList.removeAt(0)
            }
        }

        val newStrUri = uriList.joinToString("|") { it.toString() }
        val newStrDate = dateList.joinToString("|")
        val newStrMpEncrypted = mpEncryptedList.joinToString("|")
        with(shPref.edit()) {
            putString("uri", newStrUri)
            putString("date", newStrDate)
            putString("mpEncrypted", newStrMpEncrypted)
            apply()
        }
        return true
    }

    fun loadUri(context: Context?): MutableList<Uri> {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return mutableListOf()

        val str = shPref.getString("uri", "")
        if (str.isNullOrEmpty()) return mutableListOf()
        val strList = str.split("|")
        return strList.map { it.toUri() }.reversed().toMutableList()
    }

    fun loadDate(context: Context?): MutableList<String> {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return mutableListOf()

        val str = shPref.getString("date", "")
        if (str.isNullOrEmpty()) return mutableListOf()
        val strList = str.split("|")
        return strList.reversed().toMutableList()
    }

    fun getLastMpEncrypted(context: Context?): String? {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return null
        val strMpEncrypted = shPref.getString("mpEncrypted", "")
        val mpEncryptedList = strMpEncrypted!!.split("|").toMutableList()
        return mpEncryptedList[mpEncryptedList.lastIndex]
    }

    fun clear(context: Context?): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        with(shPref.edit()) {
            putString("uri", "")
            putString("date", "")
            putString("mpEncrypted", "")
            apply()
        }
        return true
    }

    fun forgetMpsEncrypted(context: Context?): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        val strUri = shPref.getString("uri", "")
        val uriList = if (!strUri.isNullOrEmpty()) {
            val strList = strUri.split("|")
            strList.map { it.toUri() }.toMutableList()
        } else mutableListOf()

        val mpEncryptedList = mutableListOf<String>()
        while (mpEncryptedList.size < uriList.size) mpEncryptedList.add(" ")
        val newStrMpEncrypted = mpEncryptedList.joinToString("|")
        shPref.edit().putString("mpEncrypted", newStrMpEncrypted).apply()
        return true
    }

    fun rememberLastMpEncrypted(context: Context?, data: String): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false
        val strMpEncrypted = shPref.getString("mpEncrypted", "")
        val mpEncryptedList = strMpEncrypted!!.split("|").toMutableList()
        mpEncryptedList[mpEncryptedList.lastIndex] = data
        val newStrMpEncrypted = mpEncryptedList.joinToString("|")

        shPref.edit().putString("mpEncrypted", newStrMpEncrypted).apply()
        return true
    }

    private fun getCurrentDateAsStr(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Calendar.getInstance().time)
    }
}
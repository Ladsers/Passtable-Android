package com.ladsers.passtable.android.containers

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

        /* password = primary password */
        val strPasswordEncrypted = shPref.getString("passwordEncrypted", "")
        val passwordEncryptedList = if (!strPasswordEncrypted.isNullOrEmpty()) {
            strPasswordEncrypted.split("|").toMutableList()
        } else mutableListOf()

        var currentPasswordEncrypted = " "
        val index = uriList.indexOf(data)
        if (index != -1) {
            uriList.removeAt(index)
            dateList.removeAt(index)

            currentPasswordEncrypted = passwordEncryptedList[index]
            passwordEncryptedList.removeAt(index)
        }

        if (isAdding) {
            uriList.add(data)
            dateList.add(getCurrentDateAsStr())
            passwordEncryptedList.add(currentPasswordEncrypted)
            val maxItems = shPref.getInt("maxItems", 15)
            if (uriList.size > maxItems) {
                uriList.removeAt(0)
                dateList.removeAt(0)
                passwordEncryptedList.removeAt(0)
            }
        }

        val newStrUri = uriList.joinToString("|") { it.toString() }
        val newStrDate = dateList.joinToString("|")
        val newStrPasswordEncrypted = passwordEncryptedList.joinToString("|")
        with(shPref.edit()) {
            putString("uri", newStrUri)
            putString("date", newStrDate)
            putString("passwordEncrypted", newStrPasswordEncrypted)
            apply()
        }
        return true
    }

    fun clear(context: Context?): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        with(shPref.edit()) {
            putString("uri", "")
            putString("date", "")
            putString("passwordEncrypted", "")
            apply()
        }
        return true
    }

    fun isNotEmpty(context: Context?): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        val strUri = shPref.getString("uri", "")
        return !strUri.isNullOrEmpty()
    }

    private fun loadData(context: Context?, key: String): List<String>? {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return null
        val str = shPref.getString(key, "")
        return if (str.isNullOrEmpty()) null else str.split("|")
    }

    fun loadUri(context: Context?): MutableList<Uri> {
        val strList = loadData(context, "uri") ?: return mutableListOf()
        return strList.map { it.toUri() }.reversed().toMutableList()
    }

    fun loadDate(context: Context?): MutableList<String> {
        val strList = loadData(context, "date") ?: return mutableListOf()
        return strList.reversed().toMutableList()
    }

    fun loadPasswordsEncrypted(context: Context?): MutableList<Boolean> {
        val strList = loadData(context, "passwordEncrypted") ?: return mutableListOf()
        return strList.map { it.isNotBlank() }.reversed().toMutableList()
    }

    fun getLastPasswordEncrypted(context: Context?): String? {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return null
        val strPasswordEncrypted = shPref.getString("passwordEncrypted", "")
        val passwordEncryptedList = strPasswordEncrypted!!.split("|").toMutableList()
        return passwordEncryptedList[passwordEncryptedList.lastIndex]
    }

    fun forgetPasswordEncrypted(context: Context?, data: Uri): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        val strUri = shPref.getString("uri", "")
        val uriList = if (!strUri.isNullOrEmpty()) {
            val strList = strUri.split("|")
            strList.map { it.toUri() }.toMutableList()
        } else mutableListOf()

        val strPasswordEncrypted = shPref.getString("passwordEncrypted", "")
        val passwordEncryptedList = if (!strPasswordEncrypted.isNullOrEmpty()) {
            strPasswordEncrypted.split("|").toMutableList()
        } else mutableListOf()

        val index = uriList.indexOf(data)
        if (index != -1) passwordEncryptedList[index] = " "

        val newStrPasswordEncrypted = passwordEncryptedList.joinToString("|")
        with(shPref.edit()) {
            putString("passwordEncrypted", newStrPasswordEncrypted)
            apply()
        }
        return true
    }

    fun forgetAllPasswordsEncrypted(context: Context?): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false

        val strUri = shPref.getString("uri", "")
        val uriList = if (!strUri.isNullOrEmpty()) {
            val strList = strUri.split("|")
            strList.map { it.toUri() }.toMutableList()
        } else mutableListOf()

        val passwordEncryptedList = mutableListOf<String>()
        while (passwordEncryptedList.size < uriList.size) passwordEncryptedList.add(" ")
        val newStrPasswordEncrypted = passwordEncryptedList.joinToString("|")
        shPref.edit().putString("passwordEncrypted", newStrPasswordEncrypted).apply()
        return true
    }

    fun rememberLastPasswordEncrypted(context: Context?, data: String): Boolean {
        val shPref = context?.getSharedPreferences("recentFiles", Context.MODE_PRIVATE)
            ?: return false
        val strPasswordEncrypted = shPref.getString("passwordEncrypted", "")
        val passwordEncryptedList = strPasswordEncrypted!!.split("|").toMutableList()
        passwordEncryptedList[passwordEncryptedList.lastIndex] = data
        val newStrPasswordEncrypted = passwordEncryptedList.joinToString("|")

        shPref.edit().putString("passwordEncrypted", newStrPasswordEncrypted).apply()
        return true
    }

    private fun getCurrentDateAsStr(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Calendar.getInstance().time)
    }
}
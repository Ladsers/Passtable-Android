package com.ladsers.passtable.android.components.tableActivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.ladsers.passtable.android.R
import com.ladsers.passtable.lib.DataItem
import com.ladsers.passtable.lib.DataTable

class TableClipboard(
    private val context: Context,
    private val dataList: List<DataItem>,
    private val table: DataTable
) {
    enum class Key { NOTE, USERNAME, PASSWORD }

    fun copy(id: Int, key: Key) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val tableId = if (dataList[id].id == -1) id else dataList[id].id

        val data = when (key) {
            Key.NOTE -> table.getNote(tableId)
            Key.USERNAME -> table.getUsername(tableId)
            Key.PASSWORD -> table.getPassword(tableId)
        }

        val clip = ClipData.newPlainText("data", data)
        clipboard.setPrimaryClip(clip)

        val msg = when (key) {
            Key.NOTE -> context.getString(R.string.ui_msg_noteCopied)
            Key.USERNAME -> context.getString(R.string.ui_msg_usernameCopied)
            Key.PASSWORD -> context.getString(R.string.ui_msg_passwordCopied)
        }
        Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    fun copy(data: String, key: Key) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("data", data)
        clipboard.setPrimaryClip(clip)

        val msg = when (key) {
            Key.NOTE -> context.getString(R.string.ui_msg_noteCopied)
            Key.USERNAME -> context.getString(R.string.ui_msg_usernameCopied)
            Key.PASSWORD -> context.getString(R.string.ui_msg_passwordCopied)
        }
        Toast.makeText(context.applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}
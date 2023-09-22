package com.ladsers.passtable.android.components

import android.content.Context
import android.net.Uri

class BackupManager(private val context: Context) {

    private val fileLimit: Byte = 5

    fun create(uri: Uri, data: String) {
        val path = uri.path?.replace('/', '_')?.replace(':', '_') ?: return

        find(path)?.let {
            context.deleteFile(it)
        } ?: let {
            if (context.fileList().size >= fileLimit) deleteFirst()
        }

        val timestamp = System.currentTimeMillis() / 1000 // accuracy in seconds is enough
        val fileName = "${timestamp}_${path}"
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(data.toByteArray())
        }
    }

    fun find(uri: Uri): String? {
        val path = uri.path?.replace('/', '_')?.replace(':', '_')
            ?: return null
        return find(path)
    }

    fun restore(uri: Uri): String? {
        val path = uri.path?.replace('/', '_')?.replace(':', '_')
            ?: return null
        find(path)?.let {
            return context.openFileInput(it).bufferedReader().readText()
        } ?: return null
    }

    fun deleteAll() {
        for (file in context.fileList()) context.deleteFile(file)
    }

    private fun find(path: String) = context.fileList()
        .find { s -> s.contains(path) } // contains() is needed not only to discard the timestamp, but also to search for part of the path.

    private fun deleteFirst() {
        val list = context.fileList()
        val timestamps = list.map { s -> s.split('_')[0].toInt() }
        val map = timestamps.zip(list).toMap().toSortedMap()

        context.deleteFile(map[map.firstKey()])
    }

}
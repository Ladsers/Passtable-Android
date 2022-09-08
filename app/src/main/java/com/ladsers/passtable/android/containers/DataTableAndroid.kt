package com.ladsers.passtable.android.containers

import android.content.ContentResolver
import androidx.core.net.toUri
import com.ladsers.passtable.lib.DataTable
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class DataTableAndroid(
    path: String? = null, primaryPassword: String? = null, cryptData: String = " ",
    private val contentResolver: ContentResolver
) :
    DataTable(path, primaryPassword, cryptData) {

    override fun writeToFile(pathToFile: String, cryptData: String) {
        val uri = pathToFile.toUri()
        //val outputStream = contentResolver.openOutputStream(uri, "rwt")

        //read-only for Google Drive files.
        val outputStream = contentResolver.openOutputStream(uri, "wt")
        val bufferedWriter = BufferedWriter(OutputStreamWriter(outputStream))
        bufferedWriter.use { out -> out.write(cryptData) }
    }
}
package com.ladsers.passtable.android

import DataTable
import android.content.ContentResolver
import androidx.core.net.toUri
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class DataTableAndroid(
    path: String? = null, masterPass: String? = null, cryptData: String = " ",
    private val contentResolver: ContentResolver
) :
    DataTable(path, masterPass, cryptData) {

    override fun writeToFile(pathToFile: String, cryptData: String) {
        val uri = pathToFile.toUri()
        val outputStream = contentResolver.openOutputStream(uri)
        val bufferedWriter = BufferedWriter(OutputStreamWriter(outputStream))
        bufferedWriter.use { out -> out.write(cryptData) }
    }
}
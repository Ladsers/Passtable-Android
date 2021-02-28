package com.ladsers.passtable.android

import DataTable

class DataTableAndroid(path: String? = null, masterPass: String? = null, cryptData: String = " "):
    DataTable(path, masterPass, cryptData){

    override fun writeToFile(pathToFile: String, cryptData: String) {
        //TODO
    }
}
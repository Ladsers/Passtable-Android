package com.ladsers.passtable.android

import DataItem
import androidx.recyclerview.widget.DiffUtil

class SearchDiffCallback(
    private val oldList: List<DataItem>,
    private val newList: List<DataItem>,
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldId =
            if (oldList[oldItemPosition].id == -1) oldItemPosition else oldList[oldItemPosition].id
        val newId =
            if (newList[newItemPosition].id == -1) newItemPosition else newList[newItemPosition].id
        return oldId == newId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return false
    }
}
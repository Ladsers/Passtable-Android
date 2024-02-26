package com.ladsers.passtable.android.callbacks

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ladsers.passtable.lib.DataItem

class ReorderCallback(
    private val list: MutableList<DataItem>
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition

        list[to] = list[from].also { list[from] = list[to] }
        recyclerView.adapter?.notifyItemMoved(from, to)

        // todo table handler

        return true
    }

    fun setReorderDrag(isEnabled: Boolean) {
        if (isEnabled) setDefaultDragDirs(ItemTouchHelper.UP or ItemTouchHelper.DOWN)
        else setDefaultDragDirs(0)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // not used
    }

}
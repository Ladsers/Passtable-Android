package com.ladsers.passtable.android.callbacks

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ladsers.passtable.android.containers.DataTableAndroid
import com.ladsers.passtable.android.enums.SearchStatus
import com.ladsers.passtable.lib.DataItem

class ReorderCallback(
    private val list: MutableList<DataItem>,
    private val table: DataTableAndroid,
    private val getSearchStatus: () -> SearchStatus,
    private val saving: () -> Unit
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    private var startPos: Int? = null
    private var endPos: Int = 0

    var dragIsActive: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        dragIsActive = true

        val from = viewHolder.adapterPosition
        val to = target.adapterPosition

        list[to] = list[from].also { list[from] = list[to] }
        recyclerView.adapter?.notifyItemMoved(from, to)
        table.swapItems(from, to)

        startPos = startPos ?: from
        endPos = to

        return true
    }

    override fun getDragDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (getSearchStatus() == SearchStatus.NONE) setDefaultDragDirs(ItemTouchHelper.UP or ItemTouchHelper.DOWN)
        else setDefaultDragDirs(0)

        return super.getDragDirs(recyclerView, viewHolder)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        startPos?.let { if (it != endPos) saving() }
        startPos = null
        dragIsActive = false
        super.clearView(recyclerView, viewHolder)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // not used
    }
}
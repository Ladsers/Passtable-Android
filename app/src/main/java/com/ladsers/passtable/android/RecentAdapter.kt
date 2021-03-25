package com.ladsers.passtable.android

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ladsers.passtable.android.databinding.ItemRecentBinding

class RecentAdapter(
    private val recentList: MutableList<Uri>,
    private val contextActivity: Context,
    private val open: (Int) -> Unit,
) : RecyclerView.Adapter<RecentAdapter.ItemViewHolder>() {

    class ItemViewHolder(val binding: ItemRecentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemRecentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            with(recentList[position]) {
                binding.tvFileName.text = contextActivity.getFileName(this) ?: "???"
                binding.clItem.setOnClickListener { open(position) }
            }
        }
    }

    override fun getItemCount(): Int {
        return recentList.size
    }
}
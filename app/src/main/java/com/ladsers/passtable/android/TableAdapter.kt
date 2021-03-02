package com.ladsers.passtable.android

import DataItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ladsers.passtable.android.databinding.ItemCollectionBinding

class TableAdapter(private val dataList: MutableList<DataItem>,
                   private val showCard: (Int) -> Unit,
                   private val showPassword: (Int) -> Unit) : RecyclerView.Adapter<TableAdapter.ItemViewHolder>() {

    class ItemViewHolder(val binding: ItemCollectionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            with(dataList[position]) {
                binding.tvTag.text = tag
                binding.tvNote.text = note
                binding.tvLogin.text = login
                binding.tvPassword.text = password

                if (password == "/no") binding.btShowPassword.visibility = View.GONE

                binding.tvPassword.visibility =
                    if (password != "/yes" && password != "/no") View.VISIBLE else View.GONE

                binding.clItem.setOnClickListener { showCard(position) }
                binding.btShowPassword.setOnClickListener { showPassword(position) }
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}
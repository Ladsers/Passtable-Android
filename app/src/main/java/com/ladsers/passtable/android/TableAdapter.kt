package com.ladsers.passtable.android

import DataItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.databinding.ItemCollectionBinding

class TableAdapter(
    private val dataList: MutableList<DataItem>,
    private val showCard: (Int) -> Unit,
    private val showPassword: (Int) -> Unit
) : RecyclerView.Adapter<TableAdapter.ItemViewHolder>() {

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
                if (tag != "0") {
                    val color = MaterialColors.getColor(
                        binding.clItem,
                        getColor(tag)
                    )
                    val drawable = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.background_item_hastag
                    )
                    DrawableCompat.setTint(drawable!!, color)
                    binding.clItem.background = drawable
                } else {
                    binding.clItem.background = ContextCompat.getDrawable(
                        binding.root.context,
                        R.drawable.background_item_notag
                    )
                }

                binding.tvNote.text = note
                binding.tvLogin.text = login
                binding.tvPassword.text = password

                binding.tvNote.visibility =
                    if (binding.tvNote.text == "") View.GONE else View.VISIBLE

                binding.tvLogin.visibility =
                    if (binding.tvLogin.text == "") View.GONE else View.VISIBLE

                binding.tvPassword.visibility =
                    if (password != "/yes" && password != "/no") View.VISIBLE else View.GONE

                binding.btShowPassword.visibility =
                    if (password == "/no") View.GONE else View.VISIBLE

                binding.btShowPassword.icon = ContextCompat.getDrawable(
                    binding.root.context,
                    if (password != "/yes" && password != "/no") R.drawable.ic_lock else R.drawable.ic_password_show
                )

                binding.clItem.setOnClickListener { showCard(position) }
                binding.btShowPassword.setOnClickListener { showPassword(position) }
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    private fun getColor(tag: String): Int {
        return when (tag) {
            "1" -> R.attr.tagRed
            "2" -> R.attr.tagGreen
            "3" -> R.attr.tagBlue
            "4" -> R.attr.tagYellow
            "5" -> R.attr.tagPurple
            else -> R.attr.whiteOrBlack
        }
    }
}
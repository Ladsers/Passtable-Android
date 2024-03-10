package com.ladsers.passtable.android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.SnackbarManager
import com.ladsers.passtable.android.components.menus.DataItemMenu
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.ItemDataTableBinding
import com.ladsers.passtable.android.enums.DataItemMainAction
import com.ladsers.passtable.android.enums.Param
import com.ladsers.passtable.lib.DataItem

class TableAdapter(
    private val dataList: MutableList<DataItem>,
    private val itemMenu: DataItemMenu
) : RecyclerView.Adapter<TableAdapter.ItemViewHolder>() {

    private lateinit var recyclerView: RecyclerView

    class ItemViewHolder(val binding: ItemDataTableBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemDataTableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
                binding.tvUsername.text = username
                binding.tvPassword.text = password

                binding.tvNote.visibility =
                    if (binding.tvNote.text == "") View.GONE else View.VISIBLE

                binding.tvUsername.visibility =
                    if (binding.tvUsername.text == "") View.GONE else View.VISIBLE

                binding.tvPassword.visibility =
                    if (password != "/yes" && password != "/no") View.VISIBLE else View.GONE

                binding.btShowPassword.visibility =
                    if (password == "/no") View.GONE else View.VISIBLE

                binding.btShowPassword.icon = ContextCompat.getDrawable(
                    binding.root.context,
                    if (password != "/yes" && password != "/no") R.drawable.ic_lock else R.drawable.ic_password_show
                )

                binding.clItem.setOnClickListener {
                    itemMenu.showPopupMenu(binding, it, position)
                    showInfoPinToScreen(binding)
                }

                binding.btShowPassword.setOnClickListener {
                    itemMenu.doMainAction(position, DataItemMainAction.SHOW_PASSWORD)
                    notifyItemChanged(position)
                    if (position == dataList.lastIndex) recyclerView.scrollToPosition(dataList.lastIndex)
                }
            }
        }
    }

    override fun getItemCount() = dataList.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
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

    private fun showInfoPinToScreen(binding: ItemDataTableBinding) {
        val context = binding.root.context
        val param = Param.INITIAL_INFO_PIN_TO_SCREEN
        val pinIsAvailable = binding.tvUsername.text != "" && binding.tvPassword.text != "/no"

        if (ParamStorage.getBool(context, param) && pinIsAvailable) SnackbarManager.showInitInfo(
            context,
            binding.root,
            param,
            context.getString(R.string.app_info_pinToScreen)
        )
    }
}
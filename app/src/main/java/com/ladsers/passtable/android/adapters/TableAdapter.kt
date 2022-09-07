package com.ladsers.passtable.android.adapters

import android.content.Context
import android.os.Build
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.components.SnackbarManager
import com.ladsers.passtable.android.containers.Param
import com.ladsers.passtable.android.containers.ParamStorage
import com.ladsers.passtable.android.databinding.ItemDataTableBinding
import com.ladsers.passtable.lib.DataItem

class TableAdapter(
    private val dataList: MutableList<DataItem>,
    private val popupAction: (Int, Int) -> Unit,
    private val showPassword: (Int) -> Unit
) : RecyclerView.Adapter<TableAdapter.ItemViewHolder>() {

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
                binding.tvLogin.text = username
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

                binding.clItem.setOnClickListener { showPopupMenu(binding, it, position) }
                binding.btShowPassword.setOnClickListener { showPassword(position) }
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    private fun showPopupMenu(binding: ItemDataTableBinding, view: View, position: Int): Boolean {
        val pop = PopupMenu(binding.root.context, view, Gravity.CENTER, 0,
            R.style.PopupMenuCustomPosTable
        )
        pop.inflate(R.menu.menu_item)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pop.setForceShowIcon(true)

        pop.menu.findItem(R.id.btShowNote).isVisible = isEllipsized(binding.tvNote)
        pop.menu.findItem(R.id.btShowLogin).isVisible = isEllipsized(binding.tvLogin)
        pop.menu.findItem(R.id.btShowPassword).isVisible = isEllipsized(binding.tvPassword)

        pop.menu.findItem(R.id.btCopyNote).isVisible = binding.tvNote.text != ""
        pop.menu.findItem(R.id.btCopyLogin).isVisible = binding.tvLogin.text != ""
        pop.menu.findItem(R.id.btCopyPassword).isVisible = binding.tvPassword.text != "/no"

        val pinIsAvailable = binding.tvLogin.text != "" && binding.tvPassword.text != "/no"
        pop.menu.findItem(R.id.btPinToScreen).isVisible = pinIsAvailable
        showInfoPinToScreen(binding.root.context, binding.root, pinIsAvailable)

        val colorNegative = ContextCompat.getColor(binding.root.context, R.color.actionNegative)
        val itemMenuRemove = pop.menu.findItem(R.id.btRemove)
        val spanStr = SpannableString(itemMenuRemove.title.toString())
        spanStr.setSpan(ForegroundColorSpan(colorNegative), 0, spanStr.length, 0)
        itemMenuRemove.title = spanStr

        pop.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.btShowNote -> popupAction(position, 1)
                R.id.btShowLogin -> popupAction(position, 2)
                R.id.btShowPassword -> popupAction(position, 3)
                R.id.btCopyNote -> popupAction(position, 4)
                R.id.btCopyLogin -> popupAction(position, 5)
                R.id.btCopyPassword -> popupAction(position, 6)
                R.id.btEdit -> popupAction(position, 7)
                R.id.btRemove -> popupAction(position, 8)
                R.id.btPinToScreen -> popupAction(position, 9)
            }
            true
        }
        pop.show()

        return true
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

    private fun isEllipsized(tv: TextView): Boolean {
        val layout = tv.layout ?: return false
        val count = layout.lineCount
        return count > 0 && layout.getEllipsisCount(count - 1) > 0
    }

    private fun showInfoPinToScreen(context: Context, view: View, pinIsAvailable: Boolean) {
        val param = Param.INITIAL_INFO_PIN_TO_SCREEN
        if (ParamStorage.getBool(context, param) && pinIsAvailable) SnackbarManager.showInitInfo(
            context,
            view,
            param,
            context.getString(R.string.app_info_pinToScreen)
        )
    }
}
package com.ladsers.passtable.android.components

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.lib.DataItem
import com.ladsers.passtable.lib.DataTable

class TagPanel(
    private val context: Context,
    private val binding: ActivityTableBinding,
    private val dataList: MutableList<DataItem>,
    private val table: DataTable,
    private val notifyUser: (String) -> Unit,
    private val notifyDataSetChanged: (List<DataItem>) -> Unit,
) {
    var searchModeIsActive = false
        private set

    private val tagIsActive = MutableList(6) { false }

    fun isAnyTagActive() = tagIsActive.any { it }

    fun findActiveTag(): String? {
        if (tagIsActive.count { it } != 1) return null
        return tagIsActive.indexOf(true).toString()
    }

    fun checkTag(index: String) = tagIsActive[index.toInt()]

    fun init() {
        binding.btTagRed.setOnClickListener { v -> searchByTag(1, v as MaterialButton) }
        binding.btTagGreen.setOnClickListener { v -> searchByTag(2, v as MaterialButton) }
        binding.btTagBlue.setOnClickListener { v -> searchByTag(3, v as MaterialButton) }
        binding.btTagYellow.setOnClickListener { v -> searchByTag(4, v as MaterialButton) }
        binding.btTagPurple.setOnClickListener { v -> searchByTag(5, v as MaterialButton) }

        var query = ""
        val searchWithDelay = Runnable { searchByData(query) }
        binding.etSearch.doAfterTextChanged { text ->
            binding.etSearch.removeCallbacks(searchWithDelay)
            query = text.toString()
            if (query.isNotEmpty()) binding.etSearch.postDelayed(searchWithDelay, 500)
            else searchByData(query)
        }

        binding.btSearch.setOnClickListener { switchPanel() }
    }

    fun switchPanel() {
        if (!isAnyTagActive()) {
            val imm =
                context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            searchModeIsActive = !searchModeIsActive

            binding.clTagButtons.visibility = if (searchModeIsActive) View.GONE else View.VISIBLE
            binding.etSearch.visibility = if (searchModeIsActive) View.VISIBLE else View.GONE

            if (searchModeIsActive) {
                binding.clPanel.setBackgroundColor(
                    MaterialColors.getColor(
                        binding.clPanel,
                        R.attr.editBackground
                    )
                )
                binding.etSearch.requestFocus()
                binding.etSearch.inputType = InputType.TYPE_CLASS_TEXT
                imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
                binding.btSearch.icon =
                    ContextCompat.getDrawable(context, R.drawable.ic_search_off)
            } else {
                binding.clPanel.setBackgroundColor(
                    MaterialColors.getColor(
                        binding.clPanel,
                        R.attr.panelTableBackground
                    )
                )
                binding.etSearch.clearFocus()
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                binding.etSearch.inputType = InputType.TYPE_NULL
                binding.etSearch.text.clear()
                binding.btSearch.icon = ContextCompat.getDrawable(context, R.drawable.ic_search)
            }
        } else {
            // closing tags
            for (i in 1..5) tagIsActive[i] = false
            val dataListOld = dataList.toList()
            dataList.clear()
            dataList.addAll(table.getData())
            notifyUser("")
            notifyDataSetChanged(dataListOld)

            with(binding) {
                btSearch.icon = ContextCompat.getDrawable(context, R.drawable.ic_search)

                btTagRed.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_red)
                btTagGreen.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_green)
                btTagBlue.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_blue)
                btTagYellow.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_yellow)
                btTagPurple.icon = ContextCompat.getDrawable(context, R.drawable.ic_tag_purple)
            }
        }
    }

    private fun searchByData(query: String) {
        val dataListOld = dataList.toList()
        dataList.clear()
        dataList.addAll(if (query.isNotEmpty()) table.searchByData(query) else table.getData())
        notifyUser(query)
        notifyDataSetChanged(dataListOld)
    }

    private fun searchByTag(tagCode: Int, btTag: MaterialButton) {
        tagIsActive[tagCode] = !tagIsActive[tagCode]

        fun getIcon(tag: Int, tagChecked: Int) =
            ContextCompat.getDrawable(context, if (tagIsActive[tagCode]) tagChecked else tag)

        btTag.icon = when (btTag) {
            binding.btTagRed -> getIcon(R.drawable.ic_tag_red, R.drawable.ic_tag_red_checked)
            binding.btTagGreen -> getIcon(R.drawable.ic_tag_green, R.drawable.ic_tag_green_checked)
            binding.btTagBlue -> getIcon(R.drawable.ic_tag_blue, R.drawable.ic_tag_blue_checked)
            binding.btTagYellow -> getIcon(
                R.drawable.ic_tag_yellow,
                R.drawable.ic_tag_yellow_checked
            )
            binding.btTagPurple -> getIcon(
                R.drawable.ic_tag_purple,
                R.drawable.ic_tag_purple_checked
            )
            else -> null
        }

        val dataListOld = dataList.toList()
        dataList.clear()
        if (isAnyTagActive()) {
            dataList.addAll(
                table.searchByTag(
                    tagIsActive[1],
                    tagIsActive[2],
                    tagIsActive[3],
                    tagIsActive[4],
                    tagIsActive[5]
                )
            )
            binding.btSearch.icon = ContextCompat.getDrawable(context, R.drawable.ic_search_off)
        } else {
            dataList.addAll(table.getData())
            binding.btSearch.icon = ContextCompat.getDrawable(context, R.drawable.ic_search)
        }

        notifyUser("")
        notifyDataSetChanged(dataListOld)
    }
}
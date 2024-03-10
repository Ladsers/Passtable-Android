package com.ladsers.passtable.android.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.databinding.ActivityTableBinding
import com.ladsers.passtable.android.enums.SearchStatus
import com.ladsers.passtable.lib.DataItem
import com.ladsers.passtable.lib.DataTable
import com.ladsers.passtable.lib.enums.ItemTagColor

class Searcher(
    private val context: Context,
    private val binding: ActivityTableBinding,
    private val dataList: MutableList<DataItem>,
    private val table: DataTable,
    private val notifyUser: () -> Unit,
    private val notifyDataSetChanged: (List<DataItem>) -> Unit,
) {
    private val iconSearch = ContextCompat.getDrawable(context, R.drawable.ic_search)
    private val iconSearchOff = ContextCompat.getDrawable(context, R.drawable.ic_search_off)

    private var searchRunnable: Runnable? = null
    private var searchTextWatcher: TextWatcher? = null

    private var textQuery = ""
        set(value) {
            field = value.lowercase()
        }
    private val activeTags = BooleanArray(6)

    /**
     * @see SearchStatus
     */
    var searchStatus = SearchStatus.NONE
        private set(value) {
            field = value
            binding.btSearch.icon = if (value == SearchStatus.NONE) iconSearch else iconSearchOff
        }

    init {
        with(binding) {
            btSearch.setOnSearchBtnClickListener()
            btTagRed.setOnTagClickListener(ItemTagColor.RED)
            btTagGreen.setOnTagClickListener(ItemTagColor.GREEN)
            btTagBlue.setOnTagClickListener(ItemTagColor.BLUE)
            btTagYellow.setOnTagClickListener(ItemTagColor.YELLOW)
            btTagPurple.setOnTagClickListener(ItemTagColor.PURPLE)
        }
    }

    /**
     * Clear the search query and return the search engine to its default state from any situation.
     */
    fun clearSearch() {
        if (searchStatus == SearchStatus.NONE) return

        searchStatus = SearchStatus.NONE

        activeTags.fill(false)
        textQuery = ""

        with(binding) {
            btTagRed.icon = getIconDrawable(ItemTagColor.RED)
            btTagGreen.icon = getIconDrawable(ItemTagColor.GREEN)
            btTagBlue.icon = getIconDrawable(ItemTagColor.BLUE)
            btTagYellow.icon = getIconDrawable(ItemTagColor.YELLOW)
            btTagPurple.icon = getIconDrawable(ItemTagColor.PURPLE)
        }

        deactivateEditText()

        val dataListOld = dataList.toList()
        dataList.clear()
        dataList.addAll(table.getData())
        notifyUser()
        notifyDataSetChanged(dataListOld)
    }

    /**
     * Attach the keyboard processing.
     */
    fun onKeyDown(keyCode: Int, event: KeyEvent?) {
        when (keyCode) {
            KeyEvent.KEYCODE_F -> {
                if (event?.isCtrlPressed == true) {
                    when (searchStatus) {
                        SearchStatus.NONE -> setTextQueryEmpty()
                        SearchStatus.TAG_QUERY -> {
                            clearSearch()
                            setTextQueryEmpty()
                        }

                        else -> clearSearch()
                    }
                }
            }

            KeyEvent.KEYCODE_ESCAPE -> clearSearch()

            KeyEvent.KEYCODE_1 -> changeTagState(ItemTagColor.RED, binding.btTagRed)
            KeyEvent.KEYCODE_2 -> changeTagState(ItemTagColor.GREEN, binding.btTagGreen)
            KeyEvent.KEYCODE_3 -> changeTagState(ItemTagColor.BLUE, binding.btTagBlue)
            KeyEvent.KEYCODE_4 -> changeTagState(ItemTagColor.YELLOW, binding.btTagYellow)
            KeyEvent.KEYCODE_5 -> changeTagState(ItemTagColor.PURPLE, binding.btTagPurple)
            KeyEvent.KEYCODE_0 -> if (searchStatus == SearchStatus.TAG_QUERY) clearSearch()
        }
    }

    /**
     * Check the item can be shown in the current selection.
     * It is relevant for adding/editing items in search mode.
     */
    fun checkItemCanBeShown(item: DataItem): Boolean {
        return when (searchStatus) {
            SearchStatus.NONE -> true
            SearchStatus.TEXT_QUERY_EMPTY -> true
            SearchStatus.TEXT_QUERY -> item.note.lowercase().contains(textQuery)
                    || item.username.lowercase().contains(textQuery)

            SearchStatus.TAG_QUERY -> activeTags[item.tag.toInt()]
        }
    }

    /**
     * Get a single tag. If no tags are selected or several are selected, it returns null.
     */
    fun getSingleTag(): ItemTagColor? {
        if (activeTags.count { it } != 1) return null
        val activeTagIndex = activeTags.indexOf(true)
        return ItemTagColor.entries.find { it.index == activeTagIndex }
    }

    private fun setTextQueryEmpty() {
        if (searchStatus == SearchStatus.TEXT_QUERY_EMPTY) return

        if (searchStatus == SearchStatus.NONE) activateEditText()

        searchStatus = SearchStatus.TEXT_QUERY_EMPTY

        textQuery = ""

        val dataListOld = dataList.toList()
        dataList.clear()
        dataList.addAll(table.getData())
        notifyUser()
        notifyDataSetChanged(dataListOld)
    }

    private fun searchByText(query: String) {
        searchStatus = SearchStatus.TEXT_QUERY

        textQuery = query
        val dataListOld = dataList.toList()
        dataList.clear()
        dataList.addAll(table.searchByText(query))
        notifyUser()
        notifyDataSetChanged(dataListOld)
    }

    private fun searchByTags() {
        searchStatus = SearchStatus.TAG_QUERY

        val dataListOld = dataList.toList()
        dataList.clear()
        dataList.addAll(
            table.searchByTag(
                activeTags[ItemTagColor.RED.index],
                activeTags[ItemTagColor.GREEN.index],
                activeTags[ItemTagColor.BLUE.index],
                activeTags[ItemTagColor.YELLOW.index],
                activeTags[ItemTagColor.PURPLE.index]
            )
        )
        notifyUser()
        notifyDataSetChanged(dataListOld)
    }

    private fun activateEditText() {
        val imm =
            context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager

        binding.clTagButtons.visibility = View.GONE
        binding.etSearch.visibility = View.VISIBLE
        binding.clPanel.setBackgroundColor(
            MaterialColors.getColor(
                binding.clPanel,
                R.attr.editBackground
            )
        )

        binding.etSearch.subscribeOnTextChanged()
        binding.etSearch.requestFocus()
        binding.etSearch.inputType = InputType.TYPE_CLASS_TEXT
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun EditText.subscribeOnTextChanged() {
        var query = ""
        searchRunnable = searchRunnable ?: Runnable { searchByText(query) }
        searchTextWatcher = searchTextWatcher ?: object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // not used
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                this@subscribeOnTextChanged.removeCallbacks(searchRunnable)
                query = s?.toString() ?: ""
                if (query.isNotEmpty()) this@subscribeOnTextChanged.postDelayed(searchRunnable, 500)
                else setTextQueryEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
                // not used
            }
        }

        this.addTextChangedListener(searchTextWatcher)
    }

    private fun deactivateEditText() {
        val imm =
            context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager

        binding.clTagButtons.visibility = View.VISIBLE
        binding.etSearch.visibility = View.GONE
        binding.clPanel.setBackgroundColor(
            MaterialColors.getColor(
                binding.clPanel,
                R.attr.panelTableBackground
            )
        )

        binding.etSearch.unsubscribeOnTextChanged()
        binding.etSearch.clearFocus()
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.inputType = InputType.TYPE_NULL
        binding.etSearch.text.clear()
    }

    private fun EditText.unsubscribeOnTextChanged() {
        searchRunnable?.let { this.removeCallbacks(it) }
        searchTextWatcher?.let { this.removeTextChangedListener(it) }
    }

    private fun MaterialButton.setOnSearchBtnClickListener() {
        this.setOnClickListener {
            if (searchStatus == SearchStatus.NONE) setTextQueryEmpty() else clearSearch()
        }
    }

    private fun MaterialButton.setOnTagClickListener(tagColor: ItemTagColor) {
        this.setOnClickListener { changeTagState(tagColor, this) }
    }

    private fun changeTagState(tagColor: ItemTagColor, button: MaterialButton) {
        if (searchStatus != SearchStatus.NONE && searchStatus != SearchStatus.TAG_QUERY) return

        val isChecked = !activeTags[tagColor.index]
        activeTags[tagColor.index] = isChecked

        button.icon = getIconDrawable(tagColor, isChecked)

        if (activeTags.any { it }) searchByTags() else clearSearch()
    }

    private fun getIconDrawable(color: ItemTagColor, isChecked: Boolean = false): Drawable {
        val resource = when (color) {
            ItemTagColor.RED -> if (isChecked) R.drawable.ic_tag_red_checked else R.drawable.ic_tag_red
            ItemTagColor.GREEN -> if (isChecked) R.drawable.ic_tag_green_checked else R.drawable.ic_tag_green
            ItemTagColor.BLUE -> if (isChecked) R.drawable.ic_tag_blue_checked else R.drawable.ic_tag_blue
            ItemTagColor.YELLOW -> if (isChecked) R.drawable.ic_tag_yellow_checked else R.drawable.ic_tag_yellow
            ItemTagColor.PURPLE -> if (isChecked) R.drawable.ic_tag_purple_checked else R.drawable.ic_tag_purple
        }

        return ContextCompat.getDrawable(context, resource)!!
    }
}
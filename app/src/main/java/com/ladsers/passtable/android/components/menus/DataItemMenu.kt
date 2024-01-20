package com.ladsers.passtable.android.components.menus

import android.os.Build
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.activities.TableActivity
import com.ladsers.passtable.android.components.DataPanel
import com.ladsers.passtable.android.components.tableActivity.TableClipboard
import com.ladsers.passtable.android.containers.DataTableAndroid
import com.ladsers.passtable.android.databinding.ItemDataTableBinding
import com.ladsers.passtable.android.dialogs.MessageDlg
import com.ladsers.passtable.android.enums.DataItemMainAction
import com.ladsers.passtable.android.enums.DataItemMainAction.*
import com.ladsers.passtable.android.enums.DataItemPopupAction
import com.ladsers.passtable.android.enums.DataItemPopupAction.*
import com.ladsers.passtable.lib.DataItem

class DataItemMenu(
    private val activity: TableActivity,
    private val messageDlg: MessageDlg,
    private val editItem: (Int) -> Unit,
    private val deleteItem: (Int, String) -> Unit
) {
    private val dataPanel: DataPanel = DataPanel(activity)

    private lateinit var dataList: MutableList<DataItem>
    private lateinit var table: DataTableAndroid
    private lateinit var tableClipboard: TableClipboard

    fun attachData(table: DataTableAndroid, dataList: MutableList<DataItem>) {
        this.table = table
        this.dataList = dataList
        tableClipboard = TableClipboard(activity, dataList, table)
    }

    /**
     * Do action of the button on the item card.
     */
    fun doMainAction(itemId: Int, action: DataItemMainAction){
        val tableId = if (dataList[itemId].id == -1) itemId else dataList[itemId].id

        when (action) {
            SHOW_PASSWORD -> {
                dataList[itemId].password =
                    if (dataList[itemId].password == "/yes") table.getPassword(tableId) else "/yes"
            }
        }
    }

    /**
     * Show pop-up menu by clicking on item card.
     */
    fun showPopupMenu(binding: ItemDataTableBinding, view: View, position: Int): Boolean {
        val pop = PopupMenu(
            binding.root.context, view, Gravity.CENTER, 0,
            R.style.PopupMenuCustomPosTable
        )
        pop.inflate(R.menu.menu_item)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pop.setForceShowIcon(true)

        fun isEllipsized(tv: TextView): Boolean {
            val layout = tv.layout ?: return false
            val count = layout.lineCount
            return count > 0 && layout.getEllipsisCount(count - 1) > 0
        }

        pop.menu.findItem(R.id.btShowNote).isVisible = isEllipsized(binding.tvNote)
        pop.menu.findItem(R.id.btShowUsername).isVisible = isEllipsized(binding.tvUsername)
        pop.menu.findItem(R.id.btShowPassword).isVisible = isEllipsized(binding.tvPassword)

        pop.menu.findItem(R.id.btCopyNote).isVisible = binding.tvNote.text != ""
        pop.menu.findItem(R.id.btCopyUsername).isVisible = binding.tvUsername.text != ""
        pop.menu.findItem(R.id.btCopyPassword).isVisible = binding.tvPassword.text != "/no"

        pop.menu.findItem(R.id.btPinToScreen).isVisible =
            binding.tvUsername.text != "" && binding.tvPassword.text != "/no"

        pop.menu.findItem(R.id.btMoveToTop).isVisible = dataList.count() == table.getSize()

        val colorNegative = ContextCompat.getColor(binding.root.context, R.color.actionNegative)
        val btDelete = pop.menu.findItem(R.id.btDelete)
        val spanStr = SpannableString(btDelete.title.toString())
        spanStr.setSpan(ForegroundColorSpan(colorNegative), 0, spanStr.length, 0)
        btDelete.title = spanStr

        pop.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.btShowNote -> doPopupAction(position, OPEN_NOTE)
                R.id.btShowUsername -> doPopupAction(position, OPEN_USERNAME)
                R.id.btShowPassword -> doPopupAction(position, OPEN_PASSWORD)
                R.id.btCopyNote -> doPopupAction(position, COPY_NOTE)
                R.id.btCopyUsername -> doPopupAction(position, COPY_USERNAME)
                R.id.btCopyPassword -> doPopupAction(position, COPY_PASSWORD)
                R.id.btEdit -> doPopupAction(position, EDIT)
                R.id.btDelete -> doPopupAction(position, DELETE)
                R.id.btPinToScreen -> doPopupAction(position, PIN_TO_SCREEN)
                R.id.btMoveToTop -> doPopupAction(position, MOVE_TO_TOP)
            }
            true
        }
        pop.show()

        return true
    }

    /**
     * Do action from item's pop-up menu.
     */
    private fun doPopupAction(itemId: Int, action: DataItemPopupAction) {
        val tableId = if (dataList[itemId].id == -1) itemId else dataList[itemId].id

        val keyNote = TableClipboard.Key.NOTE
        val keyUsername = TableClipboard.Key.USERNAME
        val keyPassword = TableClipboard.Key.PASSWORD
        when (action) {
            OPEN_NOTE -> openBigDataDlg(R.string.app_com_note, table.getNote(tableId), keyNote)
            OPEN_USERNAME -> openBigDataDlg(R.string.app_com_username, table.getUsername(tableId), keyUsername)
            OPEN_PASSWORD -> openBigDataDlg(R.string.app_com_password, table.getPassword(tableId), keyPassword)
            COPY_NOTE -> tableClipboard.copy(itemId, keyNote)
            COPY_USERNAME -> tableClipboard.copy(itemId, keyUsername)
            COPY_PASSWORD -> tableClipboard.copy(itemId, keyPassword)
            EDIT -> editItem(itemId)
            DELETE -> deleteItem(itemId, table.getNote(tableId))
            PIN_TO_SCREEN -> dataPanel.show(table.getUsername(tableId), table.getPassword(tableId))
            MOVE_TO_TOP -> {} // TODO
        }
    }

    /**
     * Open dialog for big data that has been cropped to show.
     */
    private fun openBigDataDlg(strResource: Int, data: String, key: TableClipboard.Key) {
        messageDlg.quickDialog(
            activity.getString(strResource),
            data,
            { tableClipboard.copy(data, key) },
            posText = activity.getString(R.string.app_bt_copy),
            negText = activity.getString(R.string.app_bt_close),
            posIcon = R.drawable.ic_copy
        )
    }
}
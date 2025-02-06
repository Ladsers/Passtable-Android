package com.ladsers.passtable.android.adapters

import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ladsers.passtable.android.R
import com.ladsers.passtable.android.databinding.ItemRecentFileBinding
import com.ladsers.passtable.android.enums.RecentFileStatus
import com.ladsers.passtable.android.enums.RecentItemPopupAction
import com.ladsers.passtable.android.extensions.getFileName
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class RecentAdapter(
    private val recentUri: MutableList<Uri>,
    private val recentDate: MutableList<String>,
    private val recentPasswords: MutableList<Boolean>, // having encrypted primary passwords?
    private val contextActivity: Context,
    private val open: (Int, RecentFileStatus) -> Unit,
    private val popupAction: (Int, RecentItemPopupAction) -> Unit,
) : RecyclerView.Adapter<RecentAdapter.ItemViewHolder>() {

    class ItemViewHolder(val binding: ItemRecentFileBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemRecentFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            val fileName = contextActivity.getFileName(recentUri[position]) ?: "???"

            binding.tvFileName.text = fileName
            binding.tvLastDate.text = getFormattedDate(recentDate[position])

            val gdriveFile: Boolean
            val gdrivePattern = "content://com.google.android.apps.docs.storage"
            if (recentUri[position].toString().startsWith(gdrivePattern)) {
                gdriveFile = true
                binding.ivGdrive.visibility = View.VISIBLE
            } else {
                gdriveFile = false
                binding.ivGdrive.visibility = View.GONE
            }

            val canBeOpened = fileName != "???"
            val recentFileStatus = if (canBeOpened) RecentFileStatus.OK
            else if (gdriveFile) RecentFileStatus.GDRIVE_FILE_NOT_AVAILABLE
            else RecentFileStatus.LOCAL_FILE_NOT_AVAILABLE

            binding.clItem.setOnClickListener { open(position, recentFileStatus) }
            binding.clItem.setOnLongClickListener {
                it.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
                showPopupMenu(it, recentFileStatus, position)
            }
        }
    }

    override fun getItemCount() = recentUri.size

    private fun getFormattedDate(str: String): String {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = try {
            parser.parse(str)
        } catch (e: Exception) {
            null
        }

        var result = ""

        date?.let {
            result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val dt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
                val formatter =
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                dt.format(formatter)
            } else {
                val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                formatter.format(date)
            }
        }

        return result
    }

    private fun showPopupMenu(view: View, status: RecentFileStatus, position: Int): Boolean {
        val pop = PopupMenu(
            contextActivity, view, Gravity.CENTER, 0,
            R.style.PopupMenuCustomPosRecent
        )
        pop.inflate(R.menu.menu_recent_files)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pop.setForceShowIcon(true)

        val btDisableBiometric = pop.menu.findItem(R.id.btDisableBiometric)
        btDisableBiometric.isVisible = recentPasswords[position]
        btDisableBiometric.isEnabled = recentPasswords[position]
        val colorNegative = ContextCompat.getColor(contextActivity, R.color.actionNegative)
        val btRemoveFromList = pop.menu.findItem(R.id.btRemoveFromList)
        val spanStr = SpannableString(btRemoveFromList.title.toString())
        spanStr.setSpan(ForegroundColorSpan(colorNegative), 0, spanStr.length, 0)
        btRemoveFromList.title = spanStr
        val btShare = pop.menu.findItem(R.id.btShare)
        btShare.isVisible = status == RecentFileStatus.OK
        btShare.isEnabled = status == RecentFileStatus.OK

        pop.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.btRemoveFromList -> popupAction(
                    position,
                    RecentItemPopupAction.REMOVE_FROM_LIST
                )

                R.id.btDisableBiometric -> popupAction(
                    position,
                    RecentItemPopupAction.DISABLE_BIOMETRIC
                )

                R.id.btShare -> popupAction(position, RecentItemPopupAction.SHARE_FILE)
            }
            true
        }
        pop.show()

        return true
    }
}
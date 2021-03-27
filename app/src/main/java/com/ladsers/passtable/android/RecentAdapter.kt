package com.ladsers.passtable.android

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ladsers.passtable.android.databinding.ItemRecentBinding
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class RecentAdapter(
    private val recentUri: MutableList<Uri>,
    private val recentDate: MutableList<String>,
    private val contextActivity: Context,
    private val open: (Int, Boolean) -> Unit,
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
            val fileName = contextActivity.getFileName(recentUri[position]) ?: "???"
            binding.tvFileName.text = fileName
            binding.tvLastDate.text = getFormattedDate(recentDate[position])
            binding.clItem.setOnClickListener { open(position, fileName != "???") }
        }
    }

    override fun getItemCount(): Int {
        return recentUri.size
    }

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
            result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
}
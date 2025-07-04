package com.example.medipaws

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ImageView
import java.util.UUID
import android.widget.LinearLayout
import android.widget.CheckBox

sealed class MedicineListItem {
    data class Single(val entry: MedicineEntry) : MedicineListItem()
    data class Parent(val entry: MedicineEntry, val children: List<MedicineEntry>, val expanded: Boolean) : MedicineListItem()
    data class Child(val entry: MedicineEntry) : MedicineListItem()
}

class MedicineEntryAdapter(
    private var entries: List<MedicineEntry>,
    private val onLongPress: (MedicineEntry) -> Unit,
    private val onToggleTaken: (MedicineEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val expandedSeries = mutableSetOf<String>()
    private var items: List<MedicineListItem> = listOf()

    // Multi-select mode
    var multiSelectMode: Boolean = false
        private set
    val selectedIds = mutableSetOf<String>()
    fun toggleSelection(entryId: String) {
        if (selectedIds.contains(entryId)) selectedIds.remove(entryId)
        else selectedIds.add(entryId)
        notifyDataSetChanged()
    }
    fun setMultiSelectMode(enabled: Boolean) {
        multiSelectMode = enabled
        if (!enabled) selectedIds.clear()
        notifyDataSetChanged()
    }
    fun getSelectedEntries(): List<MedicineEntry> {
        return items.mapNotNull {
            when (it) {
                is MedicineListItem.Single -> if (selectedIds.contains(it.entry.id.toString())) it.entry else null
                is MedicineListItem.Parent -> if (selectedIds.contains(it.entry.id.toString())) it.entry else null
                is MedicineListItem.Child -> if (selectedIds.contains(it.entry.id.toString())) it.entry else null
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_SINGLE = 0
        private const val VIEW_TYPE_PARENT = 1
        private const val VIEW_TYPE_CHILD = 2
    }

    init {
        buildItems()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is MedicineListItem.Single -> VIEW_TYPE_SINGLE
        is MedicineListItem.Parent -> VIEW_TYPE_PARENT
        is MedicineListItem.Child -> VIEW_TYPE_CHILD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SINGLE -> {
                val view = inflater.inflate(R.layout.entry_item_single, parent, false)
                SingleViewHolder(view)
            }
            VIEW_TYPE_PARENT -> {
                val view = inflater.inflate(R.layout.entry_item_parent, parent, false)
                ParentViewHolder(view)
            }
            VIEW_TYPE_CHILD -> {
                val view = inflater.inflate(R.layout.entry_item_child, parent, false)
                ChildViewHolder(view)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MedicineListItem.Single -> (holder as SingleViewHolder).bind(item)
            is MedicineListItem.Parent -> (holder as ParentViewHolder).bind(item)
            is MedicineListItem.Child -> (holder as ChildViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateEntries(newEntries: List<MedicineEntry>) {
        entries = newEntries
        buildItems()
        notifyDataSetChanged()
    }

    private fun buildItems() {
        val grouped = entries.groupBy { it.seriesId ?: it.id.toString() }
        val newItems = mutableListOf<MedicineListItem>()
        for ((seriesId, group) in grouped) {
            val sortedGroup = group.sortedBy { it.dateTime }
            if (sortedGroup.size == 1) {
                newItems.add(MedicineListItem.Single(sortedGroup.first()))
            } else {
                val parent = sortedGroup.first()
                val children = sortedGroup.drop(1)
                val isExpanded = expandedSeries.contains(seriesId)
                newItems.add(MedicineListItem.Parent(parent, children, isExpanded))
                if (isExpanded) {
                    children.forEach { newItems.add(MedicineListItem.Child(it)) }
                }
            }
        }
        items = newItems
    }

    inner class SingleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val singleItemLayout: LinearLayout = itemView.findViewById(R.id.singleItemLayout)
        private val singleNameText: TextView = itemView.findViewById(R.id.singleNameText)
        private val singleDoseText: TextView = itemView.findViewById(R.id.singleDoseText)
        private val singleDateTimeText: TextView = itemView.findViewById(R.id.singleDateTimeText)
        private val singleCheckmarkView: ImageView = itemView.findViewById(R.id.singleCheckmarkView)
        private val singleCheckbox: CheckBox = itemView.findViewById(R.id.singleCheckbox)
        fun bind(item: MedicineListItem.Single) {
            val entry = item.entry
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            singleNameText.text = entry.name
            singleDoseText.text = entry.dose
            singleDateTimeText.text = sdf.format(entry.dateTime)
            when (entry.status) {
                EntryStatus.DONE -> {
                    singleCheckmarkView.setColorFilter(0xFF4CAF50.toInt()) // Green
                    singleItemLayout.alpha = 0.5f
                }
                EntryStatus.PENDING -> {
                    singleCheckmarkView.setColorFilter(0xFF888888.toInt()) // Gray
                    singleItemLayout.alpha = 1f
                }
                EntryStatus.LOST -> {
                    singleCheckmarkView.setColorFilter(0xFFD32F2F.toInt()) // Red
                    singleItemLayout.alpha = 0.5f
                }
            }
            singleCheckmarkView.setOnClickListener {
                val newStatus = if (entry.status == EntryStatus.DONE) EntryStatus.PENDING else EntryStatus.DONE
                onToggleTaken(entry.copy(status = newStatus))
            }
            if (multiSelectMode) {
                singleCheckbox.visibility = View.VISIBLE
                singleCheckbox.isChecked = selectedIds.contains(entry.id.toString())
                singleCheckbox.setOnClickListener {
                    toggleSelection(entry.id.toString())
                }
                singleItemLayout.setOnClickListener {
                    toggleSelection(entry.id.toString())
                }
            } else {
                singleCheckbox.visibility = View.GONE
                singleItemLayout.setOnClickListener(null)
            }
            singleItemLayout.setOnLongClickListener {
                onLongPress(entry)
                true
            }
        }
    }

    inner class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupHeaderLayout: LinearLayout = itemView.findViewById(R.id.groupHeaderLayout)
        private val expandCollapseIcon: TextView = itemView.findViewById(R.id.expandCollapseIcon)
        private val seriesInfoText: TextView = itemView.findViewById(R.id.seriesInfoText)
        private val checkmarkView: ImageView = itemView.findViewById(R.id.checkmarkView)
        private val parentCheckbox: CheckBox = itemView.findViewById(R.id.parentCheckbox)
        fun bind(item: MedicineListItem.Parent) {
            val entry = item.entry
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val repeatInfo = if (entry.intervalValue > 0) {
                "Every ${entry.intervalValue} ${entry.intervalUnit}${if (entry.intervalValue > 1) "s" else ""}" +
                (entry.repeatUntil?.let { ", until ${SimpleDateFormat("dd-MM-yyyy").format(it)}" } ?: "")
            } else ""
            seriesInfoText.text = "${entry.name} ${if (entry.dose.isNotBlank()) "- ${entry.dose}" else ""}\n$repeatInfo\n${sdf.format(entry.dateTime)}"
            expandCollapseIcon.text = if (item.expanded) "▲" else "▼"
            groupHeaderLayout.setOnClickListener {
                if (multiSelectMode) {
                    toggleSelection(entry.id.toString())
                } else {
                    val seriesId = entry.seriesId ?: entry.id.toString()
                    if (item.expanded) expandedSeries.remove(seriesId) else expandedSeries.add(seriesId)
                    buildItems()
                    notifyDataSetChanged()
                }
            }
            if (multiSelectMode) {
                parentCheckbox.visibility = View.VISIBLE
                parentCheckbox.isChecked = selectedIds.contains(entry.id.toString())
                parentCheckbox.setOnClickListener {
                    toggleSelection(entry.id.toString())
                }
            } else {
                parentCheckbox.visibility = View.GONE
            }
            // Checkmark logic
            when (entry.status) {
                EntryStatus.DONE -> {
                    checkmarkView.setColorFilter(0xFF4CAF50.toInt()) // Green
                    groupHeaderLayout.alpha = 0.5f
                }
                EntryStatus.PENDING -> {
                    checkmarkView.setColorFilter(0xFF888888.toInt()) // Gray
                    groupHeaderLayout.alpha = 1f
                }
                EntryStatus.LOST -> {
                    checkmarkView.setColorFilter(0xFFD32F2F.toInt()) // Red
                    groupHeaderLayout.alpha = 0.5f
                }
            }
            checkmarkView.setOnClickListener {
                val newStatus = if (entry.status == EntryStatus.DONE) EntryStatus.PENDING else EntryStatus.DONE
                onToggleTaken(entry.copy(status = newStatus))
            }
            groupHeaderLayout.setOnLongClickListener {
                onLongPress(entry)
                true
            }
        }
    }

    inner class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val childItemLayout: LinearLayout = itemView.findViewById(R.id.childItemLayout)
        private val childDateTimeText: TextView = itemView.findViewById(R.id.childDateTimeText)
        private val childDoseText: TextView = itemView.findViewById(R.id.childDoseText)
        private val childCheckmarkView: ImageView = itemView.findViewById(R.id.childCheckmarkView)
        private val childCheckbox: CheckBox = itemView.findViewById(R.id.childCheckbox)
        fun bind(item: MedicineListItem.Child) {
            val entry = item.entry
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            childDateTimeText.text = sdf.format(entry.dateTime)
            childDoseText.text = entry.dose
            if (multiSelectMode) {
                childCheckbox.visibility = View.VISIBLE
                childCheckbox.isChecked = selectedIds.contains(entry.id.toString())
                childCheckbox.setOnClickListener {
                    toggleSelection(entry.id.toString())
                }
                childItemLayout.setOnClickListener {
                    toggleSelection(entry.id.toString())
                }
            } else {
                childCheckbox.visibility = View.GONE
                childItemLayout.setOnClickListener(null)
            }
            // Checkmark logic
            when (entry.status) {
                EntryStatus.DONE -> {
                    childCheckmarkView.setColorFilter(0xFF4CAF50.toInt()) // Green
                    childItemLayout.alpha = 0.5f
                }
                EntryStatus.PENDING -> {
                    childCheckmarkView.setColorFilter(0xFF888888.toInt()) // Gray
                    childItemLayout.alpha = 1f
                }
                EntryStatus.LOST -> {
                    childCheckmarkView.setColorFilter(0xFFD32F2F.toInt()) // Red
                    childItemLayout.alpha = 0.5f
                }
            }
            childCheckmarkView.setOnClickListener {
                val newStatus = if (entry.status == EntryStatus.DONE) EntryStatus.PENDING else EntryStatus.DONE
                onToggleTaken(entry.copy(status = newStatus))
            }
            childItemLayout.setOnLongClickListener {
                onLongPress(entry)
                true
            }
        }
    }
} 
package com.example.medipaws

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MedicineEntryAdapter(
    private var entries: List<MedicineEntry>,
    private val onLongPress: (MedicineEntry) -> Unit
) : RecyclerView.Adapter<MedicineEntryAdapter.EntryViewHolder>() {
    private var expandedEntryId: Long? = null

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.medicineNameText)
        val doseText: TextView = itemView.findViewById(R.id.doseText)
        val dateTimeText: TextView = itemView.findViewById(R.id.dateTimeText)
        val notesText: TextView = itemView.findViewById(R.id.notesText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.entry_item, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.nameText.text = entry.name
        holder.doseText.text = entry.dose
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        holder.dateTimeText.text = sdf.format(entry.dateTime)

        val isExpanded = expandedEntryId == entry.id
        if (!entry.notes.isNullOrBlank() && isExpanded) {
            holder.notesText.text = entry.notes
            holder.notesText.visibility = View.VISIBLE
        } else {
            holder.notesText.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            // Scale animation
            holder.itemView.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                holder.itemView.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction {
                    val isExpanded = expandedEntryId == entry.id
                    expandedEntryId = if (isExpanded) null else entry.id
                    notifyDataSetChanged()
                }.start()
            }.start()
        }
        holder.itemView.setOnLongClickListener {
            onLongPress(entry)
            true
        }
    }

    override fun getItemCount(): Int = entries.size

    fun updateEntries(newEntries: List<MedicineEntry>) {
        entries = newEntries
        // If the expanded entry is no longer in the list, collapse
        if (expandedEntryId != null && entries.none { it.id == expandedEntryId }) {
            expandedEntryId = null
        }
        notifyDataSetChanged()
    }
} 
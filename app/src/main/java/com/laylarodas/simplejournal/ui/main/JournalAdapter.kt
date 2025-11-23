package com.laylarodas.simplejournal.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.laylarodas.simplejournal.data.model.JournalEntry
import com.laylarodas.simplejournal.databinding.ItemJournalEntryBinding

class JournalAdapter :
    ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemJournalEntryBinding.inflate(inflater, parent, false)
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class JournalViewHolder(
        private val binding: ItemJournalEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: JournalEntry) {
            binding.entryTitle.text = entry.title.ifBlank { "Sin título" }
            binding.entryBody.text = entry.content.ifBlank { "..." }
            binding.entryDate.text = entry.formattedDate()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<JournalEntry>() {
            override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean =
                oldItem == newItem
        }
    }
}


package com.timebill.stopwatch.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.timebill.stopwatch.databinding.ItemSessionBinding
import com.timebill.stopwatch.model.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionsAdapter(
    private val onDelete: (String) -> Unit,
    private val onClick: (String) -> Unit
) : ListAdapter<Session, SessionsAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(private val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(session: Session) {
            binding.root.setOnClickListener {
                session.id?.let { onClick(it) }
            }
            binding.tvItemClient.text = if (session.clientName.isNullOrEmpty()) "Unnamed Client" else session.clientName
            binding.tvItemEarnings.text = String.format(Locale.getDefault(), "₹%.2f", session.earnings ?: 0.0)
            binding.tvItemDuration.text = formatDuration(session.durationMillis ?: 0L)
            binding.tvItemDate.text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(session.timestamp ?: 0L))
            
            binding.btnDelete.setOnClickListener {
                session.id?.let { onDelete(it) }
            }
        }
        
        private fun formatDuration(millis: Long): String {
            val seconds = (millis / 1000) % 60
            val minutes = (millis / (1000 * 60)) % 60
            val hours = (millis / (1000 * 60 * 60))
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean = oldItem == newItem
    }
}
package com.timebill.stopwatch.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.TransitionManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.timebill.stopwatch.R
import com.timebill.stopwatch.databinding.ItemClientBinding
import com.timebill.stopwatch.model.Client

class ClientsAdapter(
    private val onEdit: ((Client) -> Unit)? = null,
    private val onDelete: ((Client) -> Unit)? = null,
    private val onSelect: ((Client) -> Unit)? = null
) : ListAdapter<Client, ClientsAdapter.ClientViewHolder>(ClientDiffCallback()) {

    private var expandedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val binding = ItemClientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ClientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        holder.bind(getItem(position), position == expandedPosition)
    }

    inner class ClientViewHolder(private val binding: ItemClientBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(client: Client, isExpanded: Boolean) {
            binding.tvClientName.text = client.clientName
            binding.tvMobile.text = if (client.mobile.isNullOrEmpty()) "Not provided" else client.mobile
            binding.tvEmail.text = if (client.email.isNullOrEmpty()) "Not provided" else client.email
            binding.tvAddress.text = if (client.address.isNullOrEmpty()) "Not provided" else client.address

            binding.ivExpand.visibility = View.VISIBLE
            binding.expandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.ivExpand.setImageResource(if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)

            // Show/Hide buttons based on mode
            if (onSelect != null) {
                binding.btnSelect.visibility = View.VISIBLE
                binding.btnEdit.visibility = View.GONE
                binding.btnDelete.visibility = View.GONE
            } else {
                binding.btnSelect.visibility = View.GONE
                binding.btnEdit.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.VISIBLE
            }

            binding.headerClient.setOnClickListener {
                val currentPos = adapterPosition
                if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

                TransitionManager.beginDelayedTransition(binding.cardClient)
                if (expandedPosition == currentPos) {
                    expandedPosition = -1
                    notifyItemChanged(currentPos)
                } else {
                    val prevExpanded = expandedPosition
                    expandedPosition = currentPos
                    if (prevExpanded != -1) notifyItemChanged(prevExpanded)
                    notifyItemChanged(expandedPosition)
                }
            }

            binding.btnSelect.setOnClickListener { onSelect?.invoke(client) }
            binding.btnEdit.setOnClickListener { onEdit?.invoke(client) }
            binding.btnDelete.setOnClickListener { onDelete?.invoke(client) }
        }
    }

    class ClientDiffCallback : DiffUtil.ItemCallback<Client>() {
        override fun areItemsTheSame(oldItem: Client, newItem: Client): Boolean = oldItem.clientId == newItem.clientId
        override fun areContentsTheSame(oldItem: Client, newItem: Client): Boolean = oldItem == newItem
    }
}
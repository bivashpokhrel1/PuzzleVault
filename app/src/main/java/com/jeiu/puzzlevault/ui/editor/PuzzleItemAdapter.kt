package com.jeiu.puzzlevault.ui.editor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jeiu.puzzlevault.databinding.ItemPuzzleBinding
import com.jeiu.puzzlevault.model.PuzzleItem

class PuzzleItemAdapter(
    private val onDelete: (PuzzleItem) -> Unit
) : RecyclerView.Adapter<PuzzleItemAdapter.ViewHolder>() {

    private val items = mutableListOf<PuzzleItem>()
    val currentItems: List<PuzzleItem> get() = items.toList()

    fun addItem(item: PuzzleItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(item: PuzzleItem) {
        val idx = items.indexOf(item)
        if (idx >= 0) {
            items.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPuzzleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onDelete)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemPuzzleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PuzzleItem, onDelete: (PuzzleItem) -> Unit) {
            binding.tvItemInfo.text = "${item.type}  (${item.xGrid}, ${item.yGrid})"
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
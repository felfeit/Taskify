package com.felfeit.taskify.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.felfeit.taskify.R
import com.felfeit.taskify.databinding.ItemTaskBinding
import com.felfeit.taskify.models.Task

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.apply {
                tvTitle.text = task.title
                cbCompleted.isChecked = task.isCompleted
                priorityCircle.setImageResource(
                    when (task.priority) {
                        "HIGH" -> R.drawable.high_priority_circle
                        "MEDIUM" -> R.drawable.medium_priority_circle
                        else -> R.drawable.low_priority_circle
                    }
                )

                // Handle event
                root.setOnClickListener { onTaskClick(task) }
                // [FIX] Reset listener sementara
                cbCompleted.setOnCheckedChangeListener(null)
                cbCompleted.isChecked = task.isCompleted

                cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                    // [IMPORTANT] Pastikan task yang di-update adalah yang benar
                    val currentTask = getItem(adapterPosition)
                    if (currentTask.isCompleted != isChecked) {
                        onTaskChecked(currentTask, isChecked)
                    }
                }
                btnDelete.setOnClickListener { onDeleteClick(task) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position)) // getItem() dari ListAdapter
    }
}


class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.id == newItem.id // Bandingkan berdasarkan ID
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem // Bandingkan semua konten (auto jika data class)
    }
}
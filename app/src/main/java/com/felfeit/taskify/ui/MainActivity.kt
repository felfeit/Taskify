package com.felfeit.taskify.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.felfeit.taskify.R
import com.felfeit.taskify.databinding.ActivityMainBinding
import com.felfeit.taskify.databinding.DialogAddTaskBinding
import com.felfeit.taskify.models.Task
import com.felfeit.taskify.viewmodels.TaskViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var adapter: TaskAdapter
    private var currentDialog: AlertDialog? = null
    private var tasksObserver: Observer<List<Task>>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setupAdapter()
        setupRecyclerView()
        setupViewModel()
        setupFAB()
    }

    override fun onDestroy() {
        tasksObserver?.let { viewModel.allTasks.removeObserver(it) }
        currentDialog?.dismiss()
        super.onDestroy()
    }

    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(binding.root, message, duration).show()
    }

    // [1] Edge-to-edge Optimization
    private fun setupEdgeToEdge() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // [2] Adapter Setup (Optimized)
    private fun setupAdapter() {
        adapter = TaskAdapter(
            onTaskClick = { task ->
                showEditTaskDialog(task)
            },
            onTaskChecked = { task, isChecked ->
                viewModel.updateTask(task.copy(isCompleted = isChecked))
            },
            onDeleteClick = { task ->
                showDeleteConfirmationDialog(task)
            }
        )
    }

    // [3] RecyclerView Setup with Animation
    private fun setupRecyclerView() {
        binding.rvTasks.apply {
            adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 200
                changeDuration = 150
            }
        }
    }

    // [4] ViewModel Observation
    private fun setupViewModel() {
        tasksObserver = Observer { tasks ->
            adapter.submitList(tasks) {
                val newItemIndex = tasks.indexOfFirst { it.id == 0 }
                if (newItemIndex != -1) binding.rvTasks.scrollToPosition(newItemIndex)
            }
            binding.emptyState.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.allTasks.observe(this, tasksObserver!!)

        lifecycleScope.launch {
            viewModel.operationStatus.collect { status ->
                when (status) {
                    is TaskViewModel.OperationStatus.SUCCESS ->
                        showSnackbar("Operasi berhasil")

                    is TaskViewModel.OperationStatus.ERROR ->
                        showSnackbar(status.message)
                }
            }
        }
    }

    // [5] FAB Implementation with Animation
    private fun setupFAB() {
        binding.fabAddTask.apply {
            setOnClickListener {
                showAddTaskDialog()
            }
        }
    }

    // [6] Add Task Dialog (Material)
    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Tambah Task Baru")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { dialog, _ ->
                val title = dialogBinding.etTitle.text.toString()
                val priority = when (dialogBinding.priorityGroup.checkedRadioButtonId) {
                    R.id.rb_high -> "HIGH"
                    R.id.rb_medium -> "MEDIUM"
                    else -> "LOW"
                }

                if (title.isNotBlank()) {
                    viewModel.addTask(title, priority)
                }
            }
            .setNegativeButton("Batal", null)
            .create()
            .apply {
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                show()

                // Auto-focus keyboard
                dialogBinding.etTitle.requestFocus()
            }

        dialog.setOnDismissListener {
            hideKeyboard()
        }
        dialog.show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    // [7] Edit Task Dialog
    private fun showEditTaskDialog(task: Task) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater).apply {
            etTitle.setText(task.title)
            when (task.priority) {
                "HIGH" -> rbHigh.isChecked = true
                "MEDIUM" -> rbMedium.isChecked = true
                else -> rbLow.isChecked = true
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Task")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val newTitle = dialogBinding.etTitle.text.toString()
                val newPriority = when (dialogBinding.priorityGroup.checkedRadioButtonId) {
                    R.id.rb_high -> "HIGH"
                    R.id.rb_medium -> "MEDIUM"
                    else -> "LOW"
                }

                viewModel.updateTask(task.copy(title = newTitle, priority = newPriority))
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // [8] Delete Confirmation
    private fun showDeleteConfirmationDialog(task: Task) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Task?")
            .setMessage("Anda yakin ingin menghapus \"${task.title}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteTask(task)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
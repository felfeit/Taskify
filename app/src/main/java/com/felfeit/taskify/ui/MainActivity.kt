package com.felfeit.taskify.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setupAdapter()
        setupRecyclerView()
        setupViewModel()
        setupFAB()
        setupSearchField()
    }

    // Edge-to-edge Optimization
    private fun setupEdgeToEdge() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Setup Task Adapter
    private fun setupAdapter() {
        adapter = TaskAdapter(
            onTaskClick = { task ->
                showTaskDialog(task)
            },
            onTaskChecked = { task, isChecked ->
                viewModel.saveTask(task.copy(isCompleted = isChecked))
            },
            onDeleteClick = { task ->
                showDeleteConfirmationDialog(task)
            }
        )
    }

    // Setup Recycler View
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

    // ViewModel Observer
    private fun setupViewModel() {
        viewModel.allTasks.observe(this) { tasks ->
            adapter.submitList(tasks) {
                val newItemIndex = tasks.indexOfFirst { it.id == 0 }
                if (newItemIndex != -1) binding.rvTasks.smoothScrollToPosition(newItemIndex)
            }
            binding.emptyState.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        }

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

    // Setup Search
    private fun setupSearchField() {
        binding.searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle search button di keyboard
        binding.searchField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        // Observe hasil pencarian
        lifecycleScope.launch {
            viewModel.searchResults.collect { tasks ->
                adapter.submitList(tasks)
            }
        }
    }


    // Floating Action Button
    private fun setupFAB() {
        binding.fabAddTask.apply {
            setOnClickListener {
                showTaskDialog()
            }
        }
    }

    // Alert Dialog for Saving Task
    private fun showTaskDialog(task: Task? = null) {
        currentDialog?.dismiss()

        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater).apply {
            task?.let {
                etTitle.setText(it.title)
                when (it.priority) {
                    "High" -> rbHigh.isChecked = true
                    "Medium" -> rbMedium.isChecked = true
                    else -> rbLow.isChecked = true
                }
            }
        }

        currentDialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (task == null) "Tambah Task" else "Edit Task")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { dialog, _ ->
                val title = dialogBinding.etTitle.text.toString()
                val priority = when (dialogBinding.priorityGroup.checkedRadioButtonId) {
                    R.id.rb_high -> "High"
                    R.id.rb_medium -> "Medium"
                    else -> "Low"
                }

                if (title.isNotBlank()) {
                    // Use saveTask for update or insert
                    val taskToSave = task?.copy(
                        title = title,
                        priority = priority
                    ) ?: Task(
                        title = title,
                        priority = priority
                    )
                    viewModel.saveTask(taskToSave)
                }
            }
            .setNegativeButton("Batal", null)
            .create()
            .apply {
                setOnDismissListener {
                    hideKeyboard()
                    currentDialog = null
                }
                show()
                dialogBinding.etTitle.requestFocus()
            }
    }

    // Alert Dialog Confirmation for Deleting Task
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

    // Snackbar
    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(binding.root, message, duration)
            .setAction("Dismiss") { }
            .show()
    }

    // Extension
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onDestroy() {
        currentDialog?.dismiss()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
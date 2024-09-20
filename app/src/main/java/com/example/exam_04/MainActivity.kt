package com.example.exam_04

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var taskList: MutableList<Task>
    private lateinit var adapter: TaskAdapter
    private lateinit var noDataImage: ImageView
    private lateinit var taskRecycler: RecyclerView
    private lateinit var addTask: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create notification channel
        createNotificationChannel()

        // Load tasks from internal storage
        taskList = loadTasks()
        adapter = TaskAdapter(taskList) { task -> showTaskDetails(task) }

        noDataImage = findViewById(R.id.noDataImage)
        taskRecycler = findViewById(R.id.taskRecycler)
        addTask = findViewById(R.id.addTask)

        taskRecycler.layoutManager = LinearLayoutManager(this)
        taskRecycler.adapter = adapter

        updateNoDataImageVisibility()

        addTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivityForResult(intent, ADD_TASK_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check if the result is OK
        if (resultCode == RESULT_OK && data != null) {
            // Retrieve the Task object
            try {
                val task = data.getSerializableExtra("task") as? Task

                if (task != null) {
                    when (requestCode) {
                        // Handle task addition
                        ADD_TASK_REQUEST -> {
                            taskList.add(task)
                            saveTasks()
                            adapter.notifyDataSetChanged()
                            setAlarm(task) // Set alarm for task
                        }
                        // Handle task editing
                        EDIT_TASK_REQUEST -> {
                            val index = taskList.indexOfFirst { it.title == task.title && it.date == task.date && it.time == task.time }
                            if (index != -1) {
                                Log.d("MainActivity", "Editing task at index $index: ${task.title}")
                                taskList[index] = task
                                saveTasks()
                                adapter.notifyItemChanged(index)  // Notify adapter of the change
                                setAlarm(task)
                            }
                        }
                    }

                    updateNoDataImageVisibility()
                } else {
                    Log.e("MainActivity", "Task is null")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error retrieving task", e)
            }
        } else {
            Log.e("MainActivity", "Result not OK or data is null")
        }
    }

    // Load tasks from internal storage
    private fun loadTasks(): MutableList<Task> {
        val file = File(filesDir, "tasks.dat")
        return if (file.exists()) {
            ObjectInputStream(FileInputStream(file)).use { it.readObject() as MutableList<Task> }
        } else {
            mutableListOf()
        }
    }

    // Save tasks to internal storage
    private fun saveTasks() {
        try {
            val file = File(filesDir, "tasks.dat")
            ObjectOutputStream(FileOutputStream(file)).use { it.writeObject(taskList) }
            Log.d("MainActivity", "Tasks saved to internal storage.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to save tasks.", e)
        }
    }

    private fun updateNoDataImageVisibility() {
        if (taskList.isEmpty()) {
            noDataImage.visibility = View.VISIBLE
            taskRecycler.visibility = View.GONE
        } else {
            noDataImage.visibility = View.GONE
            taskRecycler.visibility = View.VISIBLE
        }
    }

    private fun setAlarm(task: Task) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("title", task.title)
            putExtra("description", task.description)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, task.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Check if we are on Android 12 (API level 31) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.alarmTimeInMillis, pendingIntent)
                } catch (e: SecurityException) {
                    Toast.makeText(this, "Permission to schedule exact alarms is not granted.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Request the user to enable exact alarm permission
                Toast.makeText(this, "You need to enable exact alarm permissions in settings.", Toast.LENGTH_LONG).show()
                // Optionally redirect to app settings
            }
        } else {
            // For older versions, set the alarm directly
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.alarmTimeInMillis, pendingIntent)
        }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_channel",
                "Task Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for task reminders"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showTaskDetails(task: Task) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(task.title)
            .setMessage("Description: ${task.description}\nDate: ${task.date}\nTime: ${task.time}")

        builder.setPositiveButton("Edit") { dialog, _ ->
            val intent = Intent(this, AddTaskActivity::class.java).apply {
                putExtra("task", task)  // Pass the existing task
                putExtra("isEdit", true)
            }
            startActivityForResult(intent, EDIT_TASK_REQUEST)
            dialog.dismiss()
        }

        builder.setNegativeButton("Delete") { dialog, _ ->
            taskList.remove(task)
            saveTasks()
            adapter.notifyDataSetChanged()
            cancelAlarm(task)
            updateNoDataImageVisibility()
            dialog.dismiss()
        }

        builder.setNeutralButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun cancelAlarm(task: Task) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, task.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }


    companion object {
        const val ADD_TASK_REQUEST = 1
        const val EDIT_TASK_REQUEST = 2
    }
}

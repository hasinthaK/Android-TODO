package com.example.exam_04

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Calendar

class AddTaskActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var addButton: Button
    private lateinit var cancelButton: Button

    private val fileName = "tasks.dat" // The name of the file to store tasks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        titleEditText = findViewById(R.id.titleEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        datePicker = findViewById(R.id.datePicker)
        timePicker = findViewById(R.id.timePicker)
        addButton = findViewById(R.id.addButton)
        cancelButton = findViewById(R.id.cancelButton)

        val isEdit = intent.getBooleanExtra("isEdit", false)
        val task = intent.getSerializableExtra("task") as? Task

        if (isEdit && task != null) {
            // Pre-fill the fields with existing task details
            titleEditText.setText(task.title)
            descriptionEditText.setText(task.description)
            val dateParts = task.date.split("-")
            datePicker.updateDate(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())

            val timeParts = task.time.split(":")
            timePicker.hour = timeParts[0].toInt()
            timePicker.minute = timeParts[1].toInt()

            // Change button text to indicate we're editing
            addButton.text = "Update Task"
        }

        addButton.setOnClickListener {
            if (isEdit && task != null) {
                editTask(task)
            } else {
                addTask()  // Add new task
            }
        }

        cancelButton.setOnClickListener {
            finish() // Close the activity
        }
    }

    private fun addTask() {

        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        // Validate title and description
        if (title.isBlank()) {
            Toast.makeText(this, "Please enter a task title.", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isBlank()) {
            Toast.makeText(this, "Please enter a task description.", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected date and time
        val selectedDate = "${datePicker.year}-${datePicker.month + 1}-${datePicker.dayOfMonth}"
        val selectedTime = "${timePicker.hour}:${timePicker.minute}"

        // Calculate alarm time in millis
        val alarmTimeInMillis = calculateAlarmTimeInMillis(selectedDate, selectedTime)

        // Create a Task object
        val task = Task(title, description, selectedDate, selectedTime, alarmTimeInMillis)


        // Set the result before finishing the activity
        val resultIntent = Intent().apply {
            putExtra("task", task)
        }
        setResult(RESULT_OK, resultIntent)

        // Finish the activity
        finish() // Close the activity
    }

    private fun editTask(existingTask: Task) {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(this, "Please enter a task title.", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isBlank()) {
            Toast.makeText(this, "Please enter a task description.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDate = "${datePicker.year}-${datePicker.month + 1}-${datePicker.dayOfMonth}"
        val selectedTime = "${timePicker.hour}:${timePicker.minute}"
        val alarmTimeInMillis = calculateAlarmTimeInMillis(selectedDate, selectedTime)

        // Update the existing task with the new values
        val updatedTask = existingTask.copy(
            title = title,
            description = description,
            date = selectedDate,
            time = selectedTime,
            alarmTimeInMillis = alarmTimeInMillis
        )

        val resultIntent = Intent().apply {
            putExtra("task", updatedTask)
        }
        setResult(RESULT_OK, resultIntent)
        finish()  // Close the activity
    }


    private fun calculateAlarmTimeInMillis(selectedDate: String, selectedTime: String): Long {
        val dateParts = selectedDate.split("-")
        val timeParts = selectedTime.split(":")

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, dateParts[0].toInt())
            set(Calendar.MONTH, dateParts[1].toInt() - 1) // Month is 0-indexed
            set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
            set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            set(Calendar.MINUTE, timeParts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis
    }

    private fun saveTaskToInternalStorage(task: Task) {
        val file = File(filesDir, fileName)
        try {
            val taskList: MutableList<Task> = if (file.exists()) {
                ObjectInputStream(FileInputStream(file)).use { it.readObject() as MutableList<Task> }
            } else {
                mutableListOf()
            }

            // Add the new task to the list
            taskList.add(task)

            // Save the updated task list back to internal storage
            ObjectOutputStream(FileOutputStream(file)).use { it.writeObject(taskList) }

            Log.d("AddTaskActivity", "Task saved to internal storage: $task")
        } catch (e: Exception) {
            Log.e("AddTaskActivity", "Error saving task", e)
            Toast.makeText(this, "Failed to save task.", Toast.LENGTH_SHORT).show()
        }
    }

}

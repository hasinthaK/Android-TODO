package com.example.exam_04
import java.io.Serializable

data class Task(
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val alarmTimeInMillis: Long
) : Serializable
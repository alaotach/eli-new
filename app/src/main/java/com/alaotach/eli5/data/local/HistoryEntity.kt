package com.alaotach.eli5.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sourceText: String,
    val resultText: String,
    val type: String, // "ELI5", "Direct", "Detailed", "Proofread"
    val timestamp: Long = System.currentTimeMillis()
)

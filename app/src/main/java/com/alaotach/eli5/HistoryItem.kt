package com.alaotach.eli5

data class HistoryItem(
    val timestamp: Long,
    val originalText: String,
    val resultText: String,
    val mode: String
)

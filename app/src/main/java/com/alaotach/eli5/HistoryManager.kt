package com.alaotach.eli5

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object HistoryManager {
    private const val KEY_HISTORY = "history_list"
    private const val PREFS_NAME = "eli5_history"

    @Throws(JSONException::class)
    fun saveItem(context: Context, item: HistoryItem) {
        if (!SettingsManager.isHistoryEnabled(context)) return
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val string = sharedPreferences.getString(KEY_HISTORY, "[]")
        val jSONArray = JSONArray(string ?: "[]")
        val jSONObject = JSONObject().apply {
            put("timestamp", item.timestamp)
            put("originalText", item.originalText)
            put("resultText", item.resultText)
            put("mode", item.mode)
        }
        jSONArray.put(jSONObject)
        sharedPreferences.edit().putString(KEY_HISTORY, jSONArray.toString()).apply()
    }

    @Throws(JSONException::class)
    fun getHistory(context: Context): List<HistoryItem> {
        val string = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_HISTORY, "[]")
        val jSONArray = JSONArray(string ?: "[]")
        val list = mutableListOf<HistoryItem>()
        for (i in 0 until jSONArray.length()) {
            val obj = jSONArray.getJSONObject(i)
            val timestamp = obj.optLong("timestamp")
            val originalText = obj.optString("originalText")
            val resultText = obj.optString("resultText")
            val mode = obj.optString("mode")
            list.add(HistoryItem(timestamp, originalText, resultText, mode))
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun clearHistory(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Throws(JSONException::class)
    fun deleteItem(context: Context, itemToDelete: HistoryItem) {
        val mutableList = getHistory(context).toMutableList()
        mutableList.removeAll { it.timestamp == itemToDelete.timestamp && it.originalText == itemToDelete.originalText }
        val jSONArray = JSONArray()
        for (item in mutableList.reversed()) {
            val jSONObject = JSONObject().apply {
                put("timestamp", item.timestamp)
                put("originalText", item.originalText)
                put("resultText", item.resultText)
                put("mode", item.mode)
            }
            jSONArray.put(jSONObject)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_HISTORY, jSONArray.toString()).apply()
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = (now - timestamp).coerceAtLeast(0)
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }

    fun calculateDayStreak(items: List<HistoryItem>): Int {
        if (items.isEmpty()) return 0
        val days = items.map {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(java.util.Calendar.YEAR) * 1000 + cal.get(java.util.Calendar.DAY_OF_YEAR)
        }.distinct().sortedDescending()

        val todayCal = java.util.Calendar.getInstance()
        val today = todayCal.get(java.util.Calendar.YEAR) * 1000 + todayCal.get(java.util.Calendar.DAY_OF_YEAR)
        val yesterday = today - 1

        if (days.first() != today && days.first() != yesterday) return 0
        var streak = 0
        var currentDay = days.first()
        for (d in days) {
            if (d == currentDay) {
                streak++
                currentDay--
            } else {
                break
            }
        }
        return maxOf(streak, 1)
    }

    fun calculateTopicsCount(items: List<HistoryItem>): Int {
        if (items.isEmpty()) return 0
        val topics = items.map {
            it.originalText.trim().split("\\s+".toRegex()).firstOrNull()?.lowercase() ?: ""
        }.filter { it.length > 2 }.distinct().size
        return maxOf(topics, minOf(items.size, 15))
    }
}

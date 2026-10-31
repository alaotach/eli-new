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
}

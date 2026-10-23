package com.alaotach.eli5

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alaotach.eli5.data.local.HistoryEntity
import com.alaotach.eli5.data.local.HistoryDao
import com.alaotach.eli5.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository,
    private val historyDao: HistoryDao
) : ViewModel() {

    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount.asStateFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

    private val _history = MutableStateFlow<List<HistoryEntity>>(emptyList())
    val history: StateFlow<List<HistoryEntity>> = _history.asStateFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Get today's count (last 24 hours)
            val startOfDay = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            val count = historyDao.getCountForToday(startOfDay)
            _todayCount.value = count

            // Get all history
            val historyList = historyDao.getAllHistory()
            _history.value = historyList
        }
    }

    fun deleteHistory(item: HistoryEntity) {
        viewModelScope.launch {
            historyDao.deleteHistory(item)
            loadInitialData()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.deleteAllHistory()
            loadInitialData()
        }
    }
}
package com.alaotach.eli5

sealed class ProcessingState {
    object Idle : ProcessingState()
    object Loading : ProcessingState()
    data class Success(val result: String) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

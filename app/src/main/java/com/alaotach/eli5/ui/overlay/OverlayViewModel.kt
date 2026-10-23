package com.alaotach.eli5.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alaotach.eli5.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OverlayViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _states = MutableStateFlow<Map<String, OverlayUiState>>(emptyMap())
    val states: StateFlow<Map<String, OverlayUiState>> = _states.asStateFlow()

    fun getExplanation(text: String, mode: String) {
        if (_states.value.containsKey(mode)) {
            return
        }

        _states.value = _states.value.toMutableMap().apply { put(mode, OverlayUiState.Loading) }

        viewModelScope.launch {
            repository.getExplanationStream(text, mode).collect { chunk ->
                val current = _states.value.toMutableMap()
                if (chunk.startsWith("Error:")) {
                    current[mode] = OverlayUiState.Error(chunk)
                } else {
                    current[mode] = OverlayUiState.Success(chunk)
                }
                _states.value = current
            }
        }
    }

    sealed class OverlayUiState {
        object Idle : OverlayUiState()
        object Loading : OverlayUiState()
        data class Success(val result: String) : OverlayUiState()
        data class Error(val message: String) : OverlayUiState()
    }
}
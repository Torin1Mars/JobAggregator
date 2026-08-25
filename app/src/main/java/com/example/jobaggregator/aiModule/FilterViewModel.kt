package com.example.jobaggregator.aiModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FruitFilterUiState(
    val prompt: String = "",
    val visible: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FruitFilterViewModel(
    private val service: GeminiFilterService = GeminiFilterService(geminiKey)
) : ViewModel() {

    private val allFruits = listOf(
        "Apple", "Banana", "Cherry", "Mango", "Kiwi",
        "Watermelon", "Grape", "Pineapple", "Blueberry", "Papaya"
    )

    private val _uiState = MutableStateFlow(FruitFilterUiState(visible = allFruits))
    val uiState: StateFlow<FruitFilterUiState> = _uiState.asStateFlow()

    fun onPromptChange(prompt: String) {
        _uiState.value = _uiState.value.copy(prompt = prompt)
    }

    fun applyFilter() {
        val prompt = _uiState.value.prompt
        if (prompt.isBlank()) {
            _uiState.value = _uiState.value.copy(visible = allFruits, error = null)
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            service.filterItems(allFruits, prompt)
                .onSuccess { filtered -> _uiState.value = _uiState.value.copy(visible = filtered, isLoading = false) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
        }
    }
}

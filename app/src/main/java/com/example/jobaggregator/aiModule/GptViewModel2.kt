package com.example.jobaggregator.aiModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.client.OpenAI
import com.example.jobaggregator.data.JobCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VacancyAiViewModel(
    private val openAI: OpenAI // construct with your API key / inject via DI
) : ViewModel() {

    private val _uiState = MutableStateFlow<VacancyAiUiState>(VacancyAiUiState.Idle)
    val uiState: StateFlow<VacancyAiUiState> = _uiState.asStateFlow()

    fun askAi(vacancies: List<JobCard>, userRequest: String) {
        viewModelScope.launch {
            _uiState.value = VacancyAiUiState.Loading
            _uiState.value = try {
                VacancyAiUiState.Success(queryVacanciesWithAi(openAI, vacancies, userRequest))
            } catch (e: Exception) {
                VacancyAiUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface VacancyAiUiState {
    data object Idle : VacancyAiUiState
    data object Loading : VacancyAiUiState
    data class Success(val answer: VacancyAiAnswer) : VacancyAiUiState
    data class Error(val message: String) : VacancyAiUiState
}
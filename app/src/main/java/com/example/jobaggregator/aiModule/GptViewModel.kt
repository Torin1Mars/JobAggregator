package com.example.jobaggregator.aiModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.client.OpenAI
import com.example.jobaggregator.data.JobCard
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ChatGptViewModel2 : ViewModel() {
    private val openAiObj: OpenAI = OpenAI(token = gptKey)
    private var gptService: GptFilterService? = null

    private val _uiState = MutableStateFlow<VacancyAiUiState>(VacancyAiUiState.Idle)
    val uiState: StateFlow<VacancyAiUiState> = _uiState.asStateFlow()

    fun askAi(vacancies: List<JobCard>, userRequest: String) {
        viewModelScope.launch {
            _uiState.value = VacancyAiUiState.Loading
            gptService = GptFilterService()

            try {
                withTimeout(gptAnswerDelay) {
                    val reply = gptService!!.queryVacanciesWithAi(openAiObj,vacancies, userRequest)

                    _uiState.value = VacancyAiUiState.Success(reply)
                }
            } catch (e: TimeoutCancellationException){
                VacancyAiUiState.Error("Timeout exceeded!")
            }
            catch (e: Exception) {
                VacancyAiUiState.Error(e.localizedMessage ?: "Error while checking!")
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


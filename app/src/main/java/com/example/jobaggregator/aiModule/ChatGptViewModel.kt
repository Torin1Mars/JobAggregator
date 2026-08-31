package com.example.jobaggregator.aiModule

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.client.OpenAI
import com.example.jobaggregator.data.JobCard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class ChatGptViewModel @Inject constructor(): ViewModel() {
    private val openAiObj: OpenAI = OpenAI(token = gptKey)
    private var gptService: GptFilterService? = null

    private val _uiState = MutableStateFlow<VacancyAiUiState>(VacancyAiUiState.Idle)
    val uiState: StateFlow<VacancyAiUiState> = _uiState.asStateFlow()

    private val _aiReplyState = MutableStateFlow<VacancyAiAnswer>(VacancyAiAnswer(emptyList<String>(), ""))
    val aiReplyState = _aiReplyState.asStateFlow()

    private val _filteredVacanciesIdList = MutableStateFlow<List<String>>(emptyList())
    val filteredVacanciesIdList = _filteredVacanciesIdList.asStateFlow()


     init {
         Log.d("MyTag", "Ai view model created")
     }

    fun askAi(vacancies: List<JobCard>, userRequest: String) {
        viewModelScope.launch {
            _uiState.value = VacancyAiUiState.Loading
            gptService = GptFilterService()

            try {
                withTimeout(gptAnswerDelay) {
                    val reply = gptService!!.queryVacanciesWithAi(openAiObj,vacancies, userRequest)

                    _aiReplyState.value = reply
                    _uiState.value = VacancyAiUiState.Success
                }
            } catch (e: TimeoutCancellationException){
                VacancyAiUiState.Error("Timeout exceeded!")
            } catch (e: Exception) {
                VacancyAiUiState.Error(e.localizedMessage ?: "Error while checking!")
            }

            Log.d("MyTag", aiReplyState.value.explanation)
        }
    }

    public fun setToIdle(){
        _uiState.value = VacancyAiUiState.Idle
    }
}

sealed interface VacancyAiUiState {
    data object Idle : VacancyAiUiState
    data object Loading : VacancyAiUiState
    data object Success: VacancyAiUiState
    data class Error(val message: String) : VacancyAiUiState
}

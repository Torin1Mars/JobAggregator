package com.example.jobaggregator.aiModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.jobaggregator.data.JobCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
                /*val completion = openAiObj.chatCompletion(
                    ChatCompletionRequest(
                        model = ModelId(gptModelTitle),
                        messages = listOf(
                            ChatMessage(role = ChatRole.User, content = userRequest)
                        )
                    )
                )
                val reply = completion.choices.first().message.content.orEmpty()*/
                //_uiState.value = _uiState.value.copy(response = reply, isLoading = false)

                gptService!!.queryVacanciesWithAi(openAiObj,vacancies, userRequest)
            } catch (e: Exception) {
                VacancyAiUiState.Error(e.localizedMessage ?: "Error while checking!")
            }



            /*
            _uiState.value = try {
                VacancyAiUiState.Success(
                    gptService!!.queryVacanciesWithAi(openAiObj, vacancies, userRequest,gptModel))
            } catch (e: Exception) {
                VacancyAiUiState.Error(e.message ?: "Unknown error")
            }*/
        }
    }
}


sealed interface VacancyAiUiState {
    data object Idle : VacancyAiUiState
    data object Loading : VacancyAiUiState
    data class Success(val answer: VacancyAiAnswer) : VacancyAiUiState
    data class Error(val message: String) : VacancyAiUiState
}


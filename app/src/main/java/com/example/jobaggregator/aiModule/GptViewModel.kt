import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.jobaggregator.aiModule.chatGptKey
import com.example.jobaggregator.data.JobCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class ChatGptUiState(
    val prompt: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatGptViewModel : ViewModel() {
    private val key: String = chatGptKey

    private val openAI = OpenAI(token = key)

    private val _uiState = MutableStateFlow(ChatGptUiState())
    val uiState: StateFlow<ChatGptUiState> = _uiState.asStateFlow()

    fun onPromptChange(newPrompt: String) {
        _uiState.value = _uiState.value.copy(prompt = newPrompt)
    }

    fun sendPrompt() {
        val currentPrompt = _uiState.value.prompt.trim()
        if (currentPrompt.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val completion = openAI.chatCompletion(
                    ChatCompletionRequest(
                        model = ModelId("gpt-5.4-mini"),
                        messages = listOf(
                            ChatMessage(role = ChatRole.User, content = currentPrompt)
                        )
                    )
                )
                val reply = completion.choices.first().message.content.orEmpty()
                _uiState.value = _uiState.value.copy(response = reply, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.localizedMessage ?: "Something went wrong",
                    isLoading = false
                )
            }
        }
    }
}


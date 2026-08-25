package com.example.jobaggregator.aiModule

import ChatGptViewModel
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGptScreen(context:Context) {

    val GptViewModel: ChatGptViewModel = viewModel()

    val uiState by GptViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ask ChatGPT", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = uiState.prompt,
            onValueChange = GptViewModel::onPromptChange,
            label = { Text("Your prompt") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Button(
            onClick = GptViewModel::sendPrompt,
            enabled = !uiState.isLoading && uiState.prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isLoading) "Asking..." else "Send")
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        }

        uiState.error?.let { err ->
            Text(
                text = "Error: $err",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (uiState.response.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.response,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}


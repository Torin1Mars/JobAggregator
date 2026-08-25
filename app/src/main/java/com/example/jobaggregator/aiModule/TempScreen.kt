package com.example.jobaggregator.aiModule

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel

@Composable
fun FruitFilterScreen(context: Context) {

    val viewModel: FruitFilterViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.prompt,
            onValueChange = viewModel::onPromptChange,
            label = { Text("e.g. \"only tropical fruits\"") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = viewModel::applyFilter, enabled = !state.isLoading) {
            Text(if (state.isLoading) "Filtering…" else "Filter")
        }

        state.error?.let {
            Text(it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(state.visible.size) { fruit ->
                Text(state.visible[fruit].toString(), modifier = Modifier.padding(vertical = 5.dp))
            }
        }

    }
}


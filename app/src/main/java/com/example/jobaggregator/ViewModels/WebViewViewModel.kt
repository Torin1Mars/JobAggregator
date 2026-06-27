package com.example.jobaggregator.ViewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobaggregator.Parsers.WebViewPool
import com.example.jobaggregator.Parsers.parseAllVacancies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebViewViewModel(context: Context) : ViewModel() {
    private val webViewPool = WebViewPool(context, poolSize = 5)

    private val _vacancies = MutableStateFlow<List<String>>(emptyList())
    val vacancies: StateFlow<List<String>> = _vacancies.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun parse(searchUrl: String, totalPages: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _vacancies.value = parseAllVacancies(webViewPool, searchUrl, totalPages)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        viewModelScope.launch { webViewPool.shutdown() }
    }
}

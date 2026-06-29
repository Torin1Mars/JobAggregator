package com.example.jobaggregator.ViewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jobaggregator.Parsers.WebViewPool
import com.example.jobaggregator.Parsers.checkHowManyPagesInRespond
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebViewViewModel(context: Context) : ViewModel() {
    private val webViewPool = WebViewPool(context)

    private val _vacancies = MutableStateFlow<List<String>>(emptyList())
    private val _respondPagesCount = MutableStateFlow<Int?>(null)

    val vacancies: StateFlow<List<String>> = _vacancies.asStateFlow()
    val respondPagesCount: StateFlow<Int?> = _respondPagesCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun parseUserQuery(searchUrl: String) {
        /*viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _vacancies.value = parseAllVacancies(webViewPool, searchUrl, totalPages)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }*/


        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _respondPagesCount.value = checkHowManyPagesInRespond(searchUrl, webViewPool)
                //_vacancies.value = parseAllVacancies(webViewPool, searchUrl)
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

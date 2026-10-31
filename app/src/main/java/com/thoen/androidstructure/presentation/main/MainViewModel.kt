package com.thoen.androidstructure.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thoen.androidstructure.domain.usecase.GetGreetingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(private val getGreeting: GetGreetingUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadGreeting()
    }

    fun loadGreeting() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { getGreeting() }
                .onSuccess { greeting ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            greeting = greeting.message,
                            endpoint = greeting.endpoint,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unknown error",
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(getGreeting: GetGreetingUseCase): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(getGreeting) }
        }
    }
}

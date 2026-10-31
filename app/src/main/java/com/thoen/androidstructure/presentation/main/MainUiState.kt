package com.thoen.androidstructure.presentation.main

data class MainUiState(
    val isLoading: Boolean = false,
    val greeting: String = "",
    val endpoint: String = "",
    val errorMessage: String? = null,
)

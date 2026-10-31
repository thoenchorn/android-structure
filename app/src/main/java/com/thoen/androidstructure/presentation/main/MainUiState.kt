package com.thoen.androidstructure.presentation.main

import com.thoen.androidstructure.domain.model.Todo

data class MainUiState(
    val isLoading: Boolean = false,
    val todos: List<Todo> = emptyList(),
    val errorMessage: String? = null,
) {
    val completedCount: Int = todos.count { it.completed }
    val progress: Float = if (todos.isEmpty()) 0f else completedCount.toFloat() / todos.size
}

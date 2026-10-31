package com.thoen.androidstructure.domain.usecase

import com.thoen.androidstructure.domain.model.Todo
import com.thoen.androidstructure.domain.repository.TodoRepository

class GetTodosUseCase(private val repository: TodoRepository) {

    suspend operator fun invoke(): List<Todo> = repository.getTodos()
}

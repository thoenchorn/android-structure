package com.thoen.androidstructure.data.repository

import com.thoen.androidstructure.data.remote.TodoApiService
import com.thoen.androidstructure.data.remote.dto.toDomain
import com.thoen.androidstructure.domain.model.Todo
import com.thoen.androidstructure.domain.repository.TodoRepository

class TodoRepositoryImpl(private val api: TodoApiService) : TodoRepository {

    override suspend fun getTodos(): List<Todo> = api.getTodos().map { it.toDomain() }
}

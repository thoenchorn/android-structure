package com.thoen.androidstructure.domain.repository

import com.thoen.androidstructure.domain.model.Todo

interface TodoRepository {
    suspend fun getTodos(): List<Todo>
}

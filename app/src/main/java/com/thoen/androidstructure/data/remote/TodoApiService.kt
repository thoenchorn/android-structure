package com.thoen.androidstructure.data.remote

import com.thoen.androidstructure.data.remote.dto.TodoDto
import retrofit2.http.GET

interface TodoApiService {

    @GET("todos")
    suspend fun getTodos(): List<TodoDto>
}

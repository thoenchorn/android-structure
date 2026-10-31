package com.thoen.androidstructure.di

import com.thoen.androidstructure.data.remote.TodoApiService
import com.thoen.androidstructure.data.repository.TodoRepositoryImpl
import com.thoen.androidstructure.domain.repository.TodoRepository
import com.thoen.androidstructure.domain.usecase.GetTodosUseCase
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual dependency container, created once by [com.thoen.androidstructure.AndroidStructureApp]
 * and scoped to the application. Swap it for Hilt/Koin when the graph grows.
 */
class AppContainer(baseUrl: String) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val todoApiService: TodoApiService = retrofit.create(TodoApiService::class.java)

    private val todoRepository: TodoRepository = TodoRepositoryImpl(todoApiService)

    val getTodosUseCase: GetTodosUseCase = GetTodosUseCase(todoRepository)
}

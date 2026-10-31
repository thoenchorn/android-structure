package com.thoen.androidstructure.domain.usecase

import com.thoen.androidstructure.domain.model.Todo
import com.thoen.androidstructure.domain.repository.TodoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTodosUseCaseTest {

    private val fakeRepository = object : TodoRepository {
        override suspend fun getTodos() = listOf(
            Todo(id = 1, title = "Write tests", completed = true),
            Todo(id = 2, title = "Ship the app", completed = false),
        )
    }

    @Test
    fun `invoke returns todos from the repository`() = runBlocking {
        val result = GetTodosUseCase(fakeRepository)()

        assertEquals(2, result.size)
        assertEquals("Write tests", result.first().title)
        assertEquals(true, result.first().completed)
    }
}

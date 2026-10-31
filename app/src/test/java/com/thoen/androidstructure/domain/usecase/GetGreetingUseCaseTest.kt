package com.thoen.androidstructure.domain.usecase

import com.thoen.androidstructure.domain.model.Greeting
import com.thoen.androidstructure.domain.repository.GreetingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetGreetingUseCaseTest {

    private val fakeRepository = object : GreetingRepository {
        override suspend fun getGreeting() = Greeting(
            message = "Hello from test",
            endpoint = "https://test.example.com/",
        )
    }

    @Test
    fun `invoke returns the repository greeting`() = runBlocking {
        val result = GetGreetingUseCase(fakeRepository)()

        assertEquals("Hello from test", result.message)
        assertEquals("https://test.example.com/", result.endpoint)
    }
}

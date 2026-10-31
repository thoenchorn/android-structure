package com.thoen.androidstructure.data.repository

import com.thoen.androidstructure.domain.model.Greeting
import com.thoen.androidstructure.domain.repository.GreetingRepository
import kotlinx.coroutines.delay

class GreetingRepositoryImpl(
    private val environment: String,
    private val baseUrl: String,
) : GreetingRepository {

    override suspend fun getGreeting(): Greeting {
        // Stand-in for a real data source (Retrofit/Ktor/Room) so the layer wiring is testable.
        delay(300)
        return Greeting(
            message = "Hello from $environment",
            endpoint = baseUrl,
        )
    }
}

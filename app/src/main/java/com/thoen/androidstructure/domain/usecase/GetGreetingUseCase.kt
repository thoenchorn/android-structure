package com.thoen.androidstructure.domain.usecase

import com.thoen.androidstructure.domain.model.Greeting
import com.thoen.androidstructure.domain.repository.GreetingRepository

class GetGreetingUseCase(private val repository: GreetingRepository) {

    suspend operator fun invoke(): Greeting = repository.getGreeting()
}

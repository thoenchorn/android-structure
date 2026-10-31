package com.thoen.androidstructure.di

import com.thoen.androidstructure.data.repository.GreetingRepositoryImpl
import com.thoen.androidstructure.domain.repository.GreetingRepository
import com.thoen.androidstructure.domain.usecase.GetGreetingUseCase

/**
 * Manual dependency container, created once by [com.thoen.androidstructure.AndroidStructureApp]
 * and scoped to the application. Swap it for Hilt/Koin when the graph grows.
 */
class AppContainer(environment: String, baseUrl: String) {

    private val greetingRepository: GreetingRepository =
        GreetingRepositoryImpl(environment = environment, baseUrl = baseUrl)

    val getGreetingUseCase: GetGreetingUseCase = GetGreetingUseCase(greetingRepository)
}

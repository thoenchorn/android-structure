package com.thoen.androidstructure.domain.repository

import com.thoen.androidstructure.domain.model.Greeting

interface GreetingRepository {
    suspend fun getGreeting(): Greeting
}

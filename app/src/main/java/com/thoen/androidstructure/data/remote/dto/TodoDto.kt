package com.thoen.androidstructure.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TodoDto(
    val id: Int,
    val title: String,
    val completed: Boolean,
)

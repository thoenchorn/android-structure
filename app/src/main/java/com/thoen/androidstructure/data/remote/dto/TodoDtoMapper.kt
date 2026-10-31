package com.thoen.androidstructure.data.remote.dto

import com.thoen.androidstructure.domain.model.Todo

fun TodoDto.toDomain(): Todo = Todo(
    id = id,
    title = title,
    completed = completed,
)

package com.thoen.androidstructure.data.remote.dto

import com.thoen.androidstructure.domain.model.Todo
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoDtoMapperTest {

    @Test
    fun `maps dto fields to domain model`() {
        val dto = TodoDto(id = 7, title = "delectus aut autem", completed = true)

        assertEquals(
            Todo(id = 7, title = "delectus aut autem", completed = true),
            dto.toDomain(),
        )
    }
}

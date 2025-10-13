package com.ogaivirt.triviago.domain.model

data class Quiz(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val questions: List<Question> = emptyList(),
    val category: String = "",
    val isPrivate: Boolean = false,
    val subscriberIds: List<String> = emptyList()
)
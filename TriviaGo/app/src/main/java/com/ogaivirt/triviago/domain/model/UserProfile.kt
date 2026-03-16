package com.ogaivirt.triviago.domain.model

data class UserProfile (
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profilePictureUrl: String? = null,
    val description: String? = null,
    val roles: List<String> = listOf("SUBSCRIBIER", "CREATOR"),
    val activeQuizIds: List<String> = emptyList(),
    val subscribedQuizIds: List<String> = emptyList()
)
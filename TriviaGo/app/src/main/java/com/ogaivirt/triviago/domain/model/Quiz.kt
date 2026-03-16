package com.ogaivirt.triviago.domain.model

import com.google.firebase.firestore.PropertyName

enum class QuizDifficulty {
    LAKO,
    SREDNJE,
    TESKO
}

data class Quiz(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val questions: List<Question> = emptyList(),
    val category: String = "",

    @get:PropertyName("private")
    @set:PropertyName("private")
    var isPrivate: Boolean = false,
    val difficulty: QuizDifficulty = QuizDifficulty.SREDNJE,
    val subscriberIds: List<String> = emptyList(),

    val totalRatingSum: Long = 0,
    val ratingCount: Int = 0,
    @get:PropertyName("creatorVerified")
    @set:PropertyName("creatorVerified")
    var isCreatorVerified: Boolean = false
){
    val averageRating: Float
        get() = if (ratingCount > 0) totalRatingSum.toFloat() / ratingCount else 0f
}
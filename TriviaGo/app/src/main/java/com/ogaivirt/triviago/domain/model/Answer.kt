package com.ogaivirt.triviago.domain.model
import com.google.firebase.firestore.PropertyName
data class Answer(
    val text: String = "",

    @get:PropertyName("correct")
    @set:PropertyName("correct")
    var isCorrect: Boolean = false
)
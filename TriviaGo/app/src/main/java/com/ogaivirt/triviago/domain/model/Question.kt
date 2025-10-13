package com.ogaivirt.triviago.domain.model

import com.google.firebase.firestore.GeoPoint

data class Question(
    val id: String = "",
    val text: String = "",
    val answers: List<Answer> = emptyList(),
    val location: GeoPoint? = null
)
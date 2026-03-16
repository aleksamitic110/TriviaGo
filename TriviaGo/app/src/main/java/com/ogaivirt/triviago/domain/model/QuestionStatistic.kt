package com.ogaivirt.triviago.domain.model
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
@Parcelize
data class QuestionStatistic (
    val quizId: String = "",
    val questionId: String = "",
    val repetitions: Int = 0,
    val easinessFactor: Float = 2.5f,
    val interval: Int = 0,
    val nextDueDate: Long = 0
) : Parcelable
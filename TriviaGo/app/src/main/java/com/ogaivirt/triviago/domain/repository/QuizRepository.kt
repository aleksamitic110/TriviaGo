package com.ogaivirt.triviago.domain.repository
import com.ogaivirt.triviago.domain.model.Question
import com.ogaivirt.triviago.domain.model.Quiz
interface QuizRepository {
    suspend fun createQuiz(quiz: Quiz): Result<String>
    suspend fun addQuestionToQuiz(quizId: String, question: Question): Result<Unit>
}
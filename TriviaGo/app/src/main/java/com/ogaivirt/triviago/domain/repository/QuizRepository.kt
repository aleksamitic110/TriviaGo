package com.ogaivirt.triviago.domain.repository
import com.ogaivirt.triviago.domain.model.Question
import com.ogaivirt.triviago.domain.model.Quiz


interface QuizRepository {
    suspend fun createQuiz(quiz: Quiz): Result<String>
    suspend fun addQuestionToQuiz(quizId: String, question: Question): Result<Unit>
    suspend fun getAllPublicQuizzes(): Result<List<Quiz>>

    suspend fun getQuizById(quizId: String): Result<Quiz?>
    suspend fun subscribeToQuiz(quizId: String): Result<Unit>

    suspend fun getMySubscribedQuizzes(): Result<List<Quiz>>

    suspend fun unsubscribeFromQuiz(quizId: String): Result<Unit>



    suspend fun rateQuiz(quizId: String, userId: String, newRating: Int): Result<Unit>

    suspend fun getCreatedQuizzesCount(): Result<Int>

    suspend fun deleteQuiz(quizId: String): Result<Unit>

    suspend fun reportQuiz(quizId: String, reason: String): Result<Unit>

    suspend fun deleteQuestion(quizId: String, question: Question): Result<Unit>

    suspend fun updateQuestion(quizId: String, updatedQuestion: Question): Result<Unit>

    suspend fun deleteAllQuizzesByCreator(userId: String): Result<Unit>

    suspend fun setVerifiedStatusForCreatorQuizzes(creatorId: String, isVerified: Boolean): Result<Unit>
}
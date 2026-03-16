package com.ogaivirt.triviago.domain.repository
import com.ogaivirt.triviago.domain.model.QuestionStatistic
interface StatisticsRepository {
    suspend fun saveQuestionStatistic(quizId: String, statistic: QuestionStatistic): Result<Unit>
    suspend fun getStatisticsForQuiz(quizId: String): Result<List<QuestionStatistic>>

    suspend fun getStatisticForQuestion(quizId: String, questionId: String): Result<QuestionStatistic?>
    suspend fun getAllStatisticsForQuiz(quizId: String): Result<List<QuestionStatistic>>
}
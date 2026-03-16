package com.ogaivirt.triviago.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ogaivirt.triviago.domain.model.QuestionStatistic
import com.ogaivirt.triviago.domain.repository.StatisticsRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : StatisticsRepository {

    override suspend fun saveQuestionStatistic(
        quizId: String,
        statistic: QuestionStatistic
    ): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Korisnik nije ulogovan."))
            db.collection("users").document(userId)
                .collection("statistics").document(quizId)
                .collection("questions").document(statistic.questionId)
                .set(statistic)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatisticsForQuiz(quizId: String): Result<List<QuestionStatistic>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Korisnik nije ulogovan."))

            val querySnapshot = db.collection("users").document(userId)
                .collection("statistics").document(quizId)
                .collection("questions")
                .get()
                .await()

            val stats = querySnapshot.toObjects(QuestionStatistic::class.java)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStatisticForQuestion(quizId: String, questionId: String): Result<QuestionStatistic?> {
        return try {
            val userId =
                auth.currentUser?.uid ?: return Result.failure(Exception("Korisnik nije ulogovan."))
            val document = db.collection("users").document(userId)
                .collection("statistics").document(quizId)
                .collection("questions").document(questionId)
                .get()
                .await()

            val stat = document.toObject(QuestionStatistic::class.java)
            Result.success(stat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllStatisticsForQuiz(quizId: String): Result<List<QuestionStatistic>> {
        return try {
            val snapshot = db.collectionGroup("questions")
                .whereEqualTo("quizId", quizId)
                .get()
                .await()
            val stats = snapshot.toObjects(QuestionStatistic::class.java)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
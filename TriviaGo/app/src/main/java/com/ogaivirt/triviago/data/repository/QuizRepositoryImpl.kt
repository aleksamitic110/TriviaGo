package com.ogaivirt.triviago.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ogaivirt.triviago.domain.model.Quiz
import com.ogaivirt.triviago.domain.repository.QuizRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.firestore.FieldValue
import com.ogaivirt.triviago.domain.model.Question
class QuizRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : QuizRepository {

    override suspend fun createQuiz(quiz: Quiz): Result<String> {
        return try {
            val newQuizRef = db.collection("quizzes").document()


            val creatorId = auth.currentUser?.uid ?: return Result.failure(Exception("Korisnik nije ulogovan."))


            val finalQuiz = quiz.copy(
                id = newQuizRef.id,
                creatorId = creatorId
                // TODO: Dohvatiti i sačuvati creatorName iz 'users' kolekcije
            )


            newQuizRef.set(finalQuiz).await()

            Result.success(newQuizRef.id)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun addQuestionToQuiz(quizId: String, question: Question): Result<Unit> {
        return try {
            val quizRef = db.collection("quizzes").document(quizId)

            quizRef.update("questions", FieldValue.arrayUnion(question)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
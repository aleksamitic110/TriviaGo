package com.ogaivirt.triviago.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.ogaivirt.triviago.domain.model.Quiz
import com.ogaivirt.triviago.domain.repository.QuizRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.firestore.FieldValue
import com.ogaivirt.triviago.domain.model.Question
import com.ogaivirt.triviago.domain.model.Report
import com.ogaivirt.triviago.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class QuizRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val authRepo: AuthRepository
) : QuizRepository {

    override suspend fun createQuiz(quiz: Quiz): Result<String> {
        return try {
            val newQuizRef = db.collection("quizzes").document()


            val creatorProfileResult = authRepo.getUserProfile()
            if (creatorProfileResult.isFailure || creatorProfileResult.getOrNull() == null) {
                return Result.failure(Exception("Nije moguće pronaći profil kreatora."))
            }
            val creatorProfile = creatorProfileResult.getOrThrow()!!

            val isCreatorVerified = creatorProfile.roles.contains("VERIFIED")

            val finalQuiz = quiz.copy(
                id = newQuizRef.id,
                creatorId = creatorProfile.uid,
                creatorName = creatorProfile.username,
                isCreatorVerified = isCreatorVerified
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
            val userProfile = authRepo.getUserProfile().getOrNull()
            val userId = userProfile?.uid
            val isAdmin = userProfile?.roles?.contains("ADMIN") == true
            if (userId == null) return Result.failure(Exception("Korisnik nije prijavljen."))

            val quizDoc = db.collection("quizzes").document(quizId).get().await()
            val creatorId = quizDoc.getString("creatorId")

            if (creatorId != userId && !isAdmin) {
                return Result.failure(Exception("Korisnik nema dozvolu za ovu akciju."))
            }

            val quizRef = db.collection("quizzes").document(quizId)

            quizRef.update("questions", FieldValue.arrayUnion(question)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllPublicQuizzes(): Result<List<Quiz>> {
        return try {
            val currentUserId = auth.currentUser?.uid
            Log.d("QuizRepoDebug", "Pokrenuto dobavljanje. Korisnik: $currentUserId")

            val publicQuizzesQuery = db.collection("quizzes")
                .whereEqualTo("private", false)
                .get()
                .await()
            val publicQuizzes = publicQuizzesQuery.toObjects(Quiz::class.java)
            Log.d("QuizRepoDebug", "Javni kvizovi pronađeni: ${publicQuizzes.size}")

            val myPrivateQuizzes = if (currentUserId != null) {
                val myQuizzesQuery = db.collection("quizzes")
                    .whereEqualTo("creatorId", currentUserId)
                    .whereEqualTo("private", true)
                    .get()
                    .await()
                val privateList = myQuizzesQuery.toObjects(Quiz::class.java)
                Log.d("QuizRepoDebug", "Moji privatni kvizovi pronađeni: ${privateList.size}")
                privateList
            } else {
                Log.d("QuizRepoDebug", "Korisnik nije ulogovan, preskačem privatne.")
                emptyList()
            }

            val allVisibleQuizzes = (publicQuizzes + myPrivateQuizzes).distinctBy { it.id }
            Log.d("QuizRepoDebug", "Ukupno vidljivih kvizova: ${allVisibleQuizzes.size}")

            Result.success(allVisibleQuizzes)
        } catch (e: Exception) {
            Log.e("QuizRepoDebug", "GREŠKA U UPITU!", e)
            Result.failure(e)
        }
    }

    override suspend fun getQuizById(quizId: String): Result<Quiz?> {
        return try {
            val document = db.collection("quizzes").document(quizId).get().await()
            val quiz = document.toObject(Quiz::class.java)
            Result.success(quiz)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun subscribeToQuiz(quizId: String): Result<Unit> {
        val tag = "SubscribeDebug"
        try {
            Log.d(tag, "Pokrenuta pretplata na kviz ID: $quizId")

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(tag, "Greška: Korisnik nije ulogovan (userId je null).")
                return Result.failure(Exception("Korisnik nije ulogovan."))
            }
            Log.d(tag, "Korisnik ID: $userId")

            val quizRef = db.collection("quizzes").document(quizId)
            val userRef = db.collection("users").document(userId)

            Log.d(tag, "Reference kreirane. Započinjem transakciju...")

            db.runTransaction { transaction ->
                Log.d(tag, "Unutar transakcije. Pokušavam da ažuriram quizRef...")
                transaction.update(quizRef, "subscriberIds", FieldValue.arrayUnion(userId))

                Log.d(tag, "QuizRef ažuriran. Pokušavam da ažuriram userRef...")
                transaction.update(userRef, "subscribedQuizIds", FieldValue.arrayUnion(quizId))

                Log.d(tag, "UserRef ažuriran. Transakcija spremna za commit.")
            }.await()

            Log.d(tag, "Transakcija USPEŠNO završena.")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Transkacija NEUSPEŠNA! Greška:", e)
            return Result.failure(e)
        }
    }

    override suspend fun getMySubscribedQuizzes(): Result<List<Quiz>> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Korisnik nije ulogovan."))
            val querySnapshot = db.collection("quizzes")
                .whereArrayContains("subscriberIds", userId)
                .get()
                .await()

            val quizzes = querySnapshot.toObjects(Quiz::class.java)
            Result.success(quizzes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun unsubscribeFromQuiz(quizId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Korisnik nije ulogovan."))
            val quizRef = db.collection("quizzes").document(quizId)
            val userRef = db.collection("users").document(userId)


            db.runTransaction { transaction ->
                transaction.update(quizRef, "subscriberIds", FieldValue.arrayRemove(userId))
                transaction.update(userRef, "subscribedQuizIds", FieldValue.arrayRemove(quizId))
                transaction.update(userRef, "activeQuizIds", FieldValue.arrayRemove(quizId))
            }.await()


            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rateQuiz(quizId: String, userId: String, newRating: Int): Result<Unit> {
        return try {
            val quizRef = db.collection("quizzes").document(quizId)
            val userRatingRef = quizRef.collection("ratings").document(userId)

            db.runTransaction { transaction ->
                val quizSnapshot = transaction.get(quizRef)
                val oldRatingSnapshot = transaction.get(userRatingRef)

                val currentTotalSum = quizSnapshot.getLong("totalRatingSum") ?: 0L
                val currentRatingCount = quizSnapshot.getLong("ratingCount")?.toInt() ?: 0

                val oldRating = if (oldRatingSnapshot.exists()) {
                    oldRatingSnapshot.getLong("rating")?.toInt() ?: 0
                } else {
                    0
                }

                val newTotalSum = currentTotalSum - oldRating + newRating

                val newRatingCount = if (oldRating == 0) {
                    currentRatingCount + 1
                } else {
                    currentRatingCount
                }


                transaction.update(quizRef, "totalRatingSum", newTotalSum)
                transaction.update(quizRef, "ratingCount", newRatingCount)

                transaction.set(userRatingRef, mapOf("rating" to newRating))

            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCreatedQuizzesCount(): Result<Int> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("..."))
            val querySnapshot = db.collection("quizzes")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()
            Result.success(querySnapshot.size())
        } catch (e: Exception)
        {
            Result.failure(e)
        }
    }


    override suspend fun deleteQuiz(quizId: String): Result<Unit> {
        return try {
            val userProfile = authRepo.getUserProfile().getOrNull()
            val userId = userProfile?.uid
            val isAdmin = userProfile?.roles?.contains("ADMIN") == true
            if (userId == null) return Result.failure(Exception("Korisnik nije prijavljen."))

            val quizDoc = db.collection("quizzes").document(quizId).get().await()
            if (quizDoc.getString("creatorId") != userId && !isAdmin) {
                return Result.failure(Exception("Korisnik nema dozvolu da obriše ovaj kviz."))
            }


            db.collection("quizzes").document(quizId).delete().await()


            val usersWithSubscribed = db.collection("users")
                .whereArrayContains("subscribedQuizIds", quizId)
                .get()
                .await()

            usersWithSubscribed.documents.forEach { userDoc ->
                db.collection("users").document(userDoc.id)
                    .update("subscribedQuizIds", FieldValue.arrayRemove(quizId))
                    .await()
            }


            val usersWithActive = db.collection("users")
                .whereArrayContains("activeQuizIds", quizId)
                .get()
                .await()

            usersWithActive.documents.forEach { userDoc ->
                db.collection("users").document(userDoc.id)
                    .update("activeQuizIds", FieldValue.arrayRemove(quizId))
                    .await()
            }


            val statsQuery = db.collection("statistics")
                .whereEqualTo("quizId", quizId)
                .get()
                .await()

            statsQuery.documents.forEach { statDoc ->
                statDoc.reference.delete().await()
            }


            val ratingsQuery = db.collection("ratings")
                .whereEqualTo("quizId", quizId)
                .get()
                .await()

            ratingsQuery.documents.forEach { ratingDoc ->
                ratingDoc.reference.delete().await()
            }

            Log.d("QuizRepository", "Kviz $quizId uspešno obrisan sa svim podacima")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("QuizRepository", "Greška pri brisanju kviza", e)
            Result.failure(e)
        }
    }

    override suspend fun reportQuiz(quizId: String, reason: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Korisnik nije ulogovan."))

            val newReportRef = db.collection("reports").document()

            val report = Report(
                reportId = newReportRef.id,
                reportedByUid = userId,
                quizId = quizId,
                reason = reason
            )

            newReportRef.set(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteQuestion(quizId: String, question: Question): Result<Unit> {
        return try {
            val userProfile = authRepo.getUserProfile().getOrNull()
            val userId = userProfile?.uid
            val isAdmin = userProfile?.roles?.contains("ADMIN") == true
            if (userId == null) return Result.failure(Exception("Korisnik nije prijavljen."))

            val quizDoc = db.collection("quizzes").document(quizId).get().await()
            val creatorId = quizDoc.getString("creatorId")

            if (creatorId != userId && !isAdmin) {
                return Result.failure(Exception("Korisnik nema dozvolu za ovu akciju."))
            }

            val quizRef = db.collection("quizzes").document(quizId)
            quizRef.update("questions", FieldValue.arrayRemove(question)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("QuizRepoImpl", "Greška pri brisanju pitanja", e)
            Result.failure(e)
        }
    }
    override suspend fun updateQuestion(quizId: String, updatedQuestion: Question): Result<Unit> {
        return try {
            val quizRef = db.collection("quizzes").document(quizId)
            val quizDoc = quizRef.get().await()

            val userProfile = authRepo.getUserProfile().getOrNull()
            val userId = userProfile?.uid
            val isAdmin = userProfile?.roles?.contains("ADMIN") == true
            if (userId == null) return Result.failure(Exception("Korisnik nije prijavljen."))

            val creatorId = quizDoc.getString("creatorId")
            if (creatorId != userId && !isAdmin) {
                return Result.failure(Exception("Korisnik nema dozvolu za ovu akciju."))
            }

            val questions = quizDoc.toObject(Quiz::class.java)?.questions.orEmpty().toMutableList()

            val index = questions.indexOfFirst { it.id == updatedQuestion.id }

            if (index == -1) {
                return Result.failure(Exception("Pitanje za ažuriranje nije pronađeno."))
            }

            questions[index] = updatedQuestion

            quizRef.update("questions", questions).await()
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("QuizRepoImpl", "Greška pri ažuriranju pitanja", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAllQuizzesByCreator(userId: String): Result<Unit> {
        return try {
            if (userId.isBlank()) return Result.failure(Exception("User ID ne sme biti prazan."))

            Log.d("QuizRepo", "Započinjanje brisanja svih kvizova za korisnika $userId")
            val querySnapshot = db.collection("quizzes")
                .whereEqualTo("creatorId", userId)
                .get()
                .await()

            val quizzesToDelete = querySnapshot.toObjects(Quiz::class.java)
            Log.d("QuizRepo", "Pronađeno ${quizzesToDelete.size} kvizova za brisanje.")

            val deleteJobs = quizzesToDelete.map { quiz ->
                CoroutineScope(Dispatchers.IO).async {
                    deleteQuiz(quiz.id)
                }
            }

            val results = deleteJobs.awaitAll()

            val firstFailure = results.find { it.isFailure }
            if (firstFailure != null) {
                Log.e("QuizRepo", "Došlo je do greške pri brisanju bar jednog kviza.", firstFailure.exceptionOrNull())
                return Result.failure(firstFailure.exceptionOrNull() ?: Exception("Nepoznata greška pri brisanju kvizova."))
            }

            Log.d("QuizRepo", "Svi kvizovi za korisnika $userId uspešno obrisani.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("QuizRepo", "Greška pri brisanju kvizova za korisnika $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun setVerifiedStatusForCreatorQuizzes(creatorId: String, isVerified: Boolean): Result<Unit> {
        return try {
            if (creatorId.isBlank()) return Result.failure(Exception("Creator ID ne sme biti prazan."))

            Log.d("QuizRepo", "Ažuriranje verified statusa na '$isVerified' za kvizove kreatora $creatorId")
            val querySnapshot = db.collection("quizzes")
                .whereEqualTo("creatorId", creatorId)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.d("QuizRepo", "Kreator $creatorId nema kvizova za ažuriranje.")
                return Result.success(Unit)
            }


            val batch: WriteBatch = db.batch()
            querySnapshot.documents.forEach { document ->
                batch.update(document.reference, "isCreatorVerified", isVerified)
            }

            batch.commit().await()
            Log.d("QuizRepo", "Uspešno ažurirano ${querySnapshot.size()} kvizova za kreatora $creatorId.")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("QuizRepo", "Greška pri ažuriranju verified statusa za kvizove kreatora $creatorId", e)
            Result.failure(e)
        }
    }
}
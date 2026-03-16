package com.ogaivirt.triviago.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ogaivirt.triviago.domain.model.Notification
import com.ogaivirt.triviago.domain.model.NotificationType
import com.ogaivirt.triviago.domain.repository.AuthRepository
import com.ogaivirt.triviago.domain.repository.NotificationRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val authRepo: AuthRepository
) : NotificationRepository {

    override suspend fun createNotification(
        userId: String,
        message: String,
        type: NotificationType,
        relatedQuizId: String?
    ): Result<Unit> {
        return try {
            if (userId.isBlank()) return Result.failure(Exception("UserID ne sme biti prazan za notifikaciju."))

            val newNotifRef = db.collection("notifications").document()
            val notification = Notification(
                id = newNotifRef.id,
                userId = userId,
                message = message,
                type = type,
                relatedQuizId = relatedQuizId,
                isRead = false
            )
            newNotifRef.set(notification).await()
            Log.d("NotificationRepo", "Notifikacija kreirana za $userId: ${newNotifRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Greška pri kreiranju notifikacije za $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun getNotificationsForCurrentUser(): Result<List<Notification>> {
        return try {
            val currentUser = authRepo.getCurrentUser()
            if (currentUser == null) {
                Log.w("NotificationRepo", "Pokušaj dobavljanja notifikacija, ali korisnik nije ulogovan.")
                return Result.success(emptyList())
            }
            val userId = currentUser.uid

            val snapshot = db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val notifications = snapshot.toObjects(Notification::class.java)
            Log.d("NotificationRepo", "Dobavljeno ${notifications.size} notifikacija za $userId")
            Result.success(notifications)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Greška pri dobavljanju notifikacija", e)
            Result.failure(e)
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            if (notificationId.isBlank()) return Result.failure(Exception("Notification ID ne sme biti prazan."))

            db.collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
            Log.d("NotificationRepo", "Notifikacija označena kao pročitana: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Greška pri označavanju notifikacije $notificationId kao pročitane", e)
            Result.failure(e)
        }
    }
}
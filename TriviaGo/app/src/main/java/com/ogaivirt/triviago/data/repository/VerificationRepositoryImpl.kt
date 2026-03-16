package com.ogaivirt.triviago.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.ogaivirt.triviago.domain.model.UserProfile
import com.ogaivirt.triviago.domain.model.VerificationRequest
import com.ogaivirt.triviago.domain.model.VerificationStatus
import com.ogaivirt.triviago.domain.repository.AuthRepository
import com.ogaivirt.triviago.domain.repository.VerificationRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class VerificationRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val authRepo: AuthRepository
) : VerificationRepository {

    override suspend fun requestVerification(imageUri: Uri): Result<Unit> {
        return try {
            val userProfile = authRepo.getUserProfile().getOrNull()
            if (userProfile == null) {
                return Result.failure(Exception("Korisnik nije ulogovan ili profil nije pronađen."))
            }
            val userId = userProfile.uid

            val existingRequest = getMyVerificationStatus().getOrNull()
            if (existingRequest != null && existingRequest != VerificationStatus.REJECTED) {
                return Result.failure(Exception("Zahtev za verifikaciju je već podnet ili u procesu obrade."))
            }

            val storageRef = storage.reference.child("verification_images/${userId}/${System.currentTimeMillis()}.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d("VerificationRepo", "Slika uploadovana: $downloadUrl")

            val requestDocRef = db.collection("verificationRequests").document(userId)

            val request = VerificationRequest(
                id = userId,
                userId = userId,
                username = userProfile.username,
                imageUrl = downloadUrl,
                status = VerificationStatus.PENDING
            )

            requestDocRef.set(request).await()
            Log.d("VerificationRepo", "Zahtev za verifikaciju kreiran za korisnika $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("VerificationRepo", "Greška pri podnošenju zahteva za verifikaciju", e)
            Result.failure(e)
        }
    }

    override suspend fun getMyVerificationStatus(): Result<VerificationStatus?> {
        return try {
            val userId = authRepo.getCurrentUser()?.uid
            if (userId == null) {
                return Result.success(null)
            }
            val document = db.collection("verificationRequests").document(userId).get().await()
            val request = document.toObject(VerificationRequest::class.java)
            Result.success(request?.status)
        } catch (e: Exception) {
            Log.e("VerificationRepo", "Greška pri dobavljanju statusa verifikacije", e)
            Result.failure(e)
        }
    }

    override suspend fun getPendingVerificationRequests(): Result<List<VerificationRequest>> {
        return try {
            val snapshot = db.collection("verificationRequests")
                .whereEqualTo("status", VerificationStatus.PENDING)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            val requests = snapshot.toObjects(VerificationRequest::class.java)
            Log.d("VerificationRepo", "Dobavljeno ${requests.size} PENDING zahteva.")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e("VerificationRepo", "Greška pri dobavljanju PENDING zahteva.", e)
            Result.failure(e)
        }
    }

    override suspend fun getApprovedVerificationRequests(): Result<List<VerificationRequest>> {
        return try {
            val snapshot = db.collection("verificationRequests")
                .whereEqualTo("status", VerificationStatus.APPROVED)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            val requests = snapshot.toObjects(VerificationRequest::class.java)
            Log.d("VerificationRepo", "Dobavljeno ${requests.size} APPROVED zahteva.")
            Result.success(requests)
        } catch (e: Exception) {
            Log.e("VerificationRepo", "Greška pri dobavljanju APPROVED zahteva.", e)
            Result.failure(e)
        }
    }

    override suspend fun updateVerificationStatus(requestId: String, newStatus: VerificationStatus, reason: String?): Result<Unit> {
        return try {
            if (requestId.isBlank()) return Result.failure(Exception("Request ID ne sme biti prazan."))

            val updates = mutableMapOf<String, Any>(
                "status" to newStatus
            )
            if (newStatus == VerificationStatus.REJECTED && !reason.isNullOrBlank()) {
                updates["rejectionReason"] = reason
            } else {
                updates["rejectionReason"] = FieldValue.delete()
            }


            db.collection("verificationRequests").document(requestId)
                .update(updates)
                .await()
            Log.d("VerificationRepo", "Status zahteva $requestId ažuriran na $newStatus.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VerificationRepo", "Greška pri ažuriranju statusa zahteva $requestId.", e)
            Result.failure(e)
        }
    }
}
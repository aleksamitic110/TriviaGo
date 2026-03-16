package com.ogaivirt.triviago.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ogaivirt.triviago.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ogaivirt.triviago.domain.model.UserProfile
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.lang.Exception

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient,
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage
) : AuthRepository {

    override suspend fun createUserWithEmailAndPassword(ime: String, email: String, lozinka: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, lozinka).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Korisnik nije kreiran."))

            firebaseUser.sendEmailVerification().await()

            val userProfile = UserProfile(
                uid = firebaseUser.uid,
                username = ime,
                email = email
            )

            db.collection("users").document(firebaseUser.uid).set(userProfile).await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Google prijava neuspešna."))

            val userDocRef = db.collection("users").document(firebaseUser.uid)
            val document = userDocRef.get().await()

            if (!document.exists()) {
                val userProfile = UserProfile(
                    uid = firebaseUser.uid,
                    username = firebaseUser.displayName ?: "Korisnik",
                    email = firebaseUser.email ?: "",
                    profilePictureUrl = firebaseUser.photoUrl?.toString()
                )
                userDocRef.set(userProfile).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, lozinka: String): Result<Unit> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, lozinka).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Prijava neuspešna."))

            if (firebaseUser.isEmailVerified) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Email nije verifikovan. Molimo vas, proverite svoje sanduče."))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        try {
            auth.signOut()
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    override suspend fun getUserProfile(): Result<UserProfile?> {
        return try {
            val user = auth.currentUser ?: return Result.success(null)
            val document = db.collection("users").document(user.uid).get().await()
            val userProfile = document.toObject(UserProfile::class.java)
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserProfile(username: String, description: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Korisnik nije ulogovan."))
            db.collection("users").document(user.uid).update(
                mapOf(
                    "username" to username,
                    "description" to description
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfilePicture(uri: Uri): Result<String> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Korisnik nije ulogovan."))
            val storageRef = storage.reference.child("profile_images/${user.uid}.jpg")


            storageRef.putFile(uri).await()

            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePictureUrl(url: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Korisnik nije ulogovan."))
            db.collection("users").document(user.uid).update("profilePictureUrl", url).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateActiveQuizzes(activeQuizIds: List<String>): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Korisnik nije ulogovan."))
            db.collection("users").document(user.uid)
                .update("activeQuizIds", activeQuizIds)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeRole(userId: String, role: String): Result<Unit> {
        return try {
            if (userId.isBlank()) return Result.failure(Exception("User ID ne sme biti prazan."))
            db.collection("users").document(userId)
                .update("roles", FieldValue.arrayRemove(role))
                .await()
            Log.d("AuthRepo", "Uloga '$role' uspešno uklonjena za korisnika $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Greška pri uklanjanju uloge '$role' za korisnika $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun addRole(userId: String, role: String): Result<Unit> {
        return try {
            if (userId.isBlank()) return Result.failure(Exception("User ID ne sme biti prazan."))
            db.collection("users").document(userId)
                .update("roles", FieldValue.arrayUnion(role))
                .await()
            Log.d("AuthRepo", "Uloga '$role' uspešno dodata korisniku $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Greška pri dodavanju uloge '$role' za korisnika $userId", e)
            Result.failure(e)
        }
    }
}
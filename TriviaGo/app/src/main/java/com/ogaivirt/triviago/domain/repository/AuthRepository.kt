package com.ogaivirt.triviago.domain.repository
import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.ogaivirt.triviago.domain.model.UserProfile

interface AuthRepository {
    suspend fun createUserWithEmailAndPassword(ime: String, email: String, lozinka: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signInWithEmailAndPassword(email: String, lozinka: String): Result<Unit>
    suspend fun signOut()

    fun isUserAuthenticated(): Boolean
    fun getCurrentUser(): FirebaseUser?
    suspend fun getUserProfile(): Result<UserProfile?>
    suspend fun updateUserProfile(username: String, description: String): Result<Unit>

    suspend fun uploadProfilePicture(uri: Uri): Result<String>
    suspend fun updateProfilePictureUrl(url: String): Result<Unit>

    suspend fun updateActiveQuizzes(activeQuizIds: List<String>): Result<Unit>

    suspend fun removeRole(userId: String, role: String): Result<Unit>

    suspend fun addRole(userId: String, role: String): Result<Unit>
}
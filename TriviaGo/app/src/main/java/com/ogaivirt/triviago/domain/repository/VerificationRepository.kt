package com.ogaivirt.triviago.domain.repository

import android.net.Uri
import com.ogaivirt.triviago.domain.model.VerificationRequest
import com.ogaivirt.triviago.domain.model.VerificationStatus

interface VerificationRepository {

    suspend fun requestVerification(imageUri: Uri): Result<Unit>
    suspend fun getMyVerificationStatus(): Result<VerificationStatus?>

    suspend fun getPendingVerificationRequests(): Result<List<VerificationRequest>>

    suspend fun getApprovedVerificationRequests(): Result<List<VerificationRequest>> // NOVO: Za admina

    suspend fun updateVerificationStatus(requestId: String, newStatus: VerificationStatus, reason: String? = null): Result<Unit>
}
package com.ogaivirt.triviago.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class VerificationStatus {
    PENDING,
    REVIEWING,
    APPROVED,
    REJECTED,
    VERIFIED
}

data class VerificationRequest(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val imageUrl: String = "",
    val status: VerificationStatus = VerificationStatus.PENDING,
    val rejectionReason: String? = null,
    @ServerTimestamp val timestamp: Date? = null
)
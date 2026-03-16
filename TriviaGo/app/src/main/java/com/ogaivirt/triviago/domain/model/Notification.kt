package com.ogaivirt.triviago.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class NotificationType {
    GENERIC,
    QUIZ_MODIFIED,
    CREATOR_BAN

}

data class Notification(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.GENERIC,
    val relatedQuizId: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null
)
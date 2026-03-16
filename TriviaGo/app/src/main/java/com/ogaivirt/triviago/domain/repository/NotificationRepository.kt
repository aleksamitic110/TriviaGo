package com.ogaivirt.triviago.domain.repository

import com.ogaivirt.triviago.domain.model.Notification
import com.ogaivirt.triviago.domain.model.NotificationType

interface NotificationRepository {

    suspend fun createNotification(
        userId: String,
        message: String,
        type: NotificationType = NotificationType.GENERIC,
        relatedQuizId: String? = null
    ): Result<Unit>

    suspend fun getNotificationsForCurrentUser(): Result<List<Notification>>

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>


}
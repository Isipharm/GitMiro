package com.hugodehu.gitmiro.domain.repositories

import com.hugodehu.gitmiro.domain.models.NotificationData
import kotlinx.coroutines.flow.StateFlow

interface NotificationsRepository {
    val notifications: StateFlow<List<NotificationData>>

    suspend fun emitNotification(notificationData: NotificationData)
}
package com.hugodehu.gitmiro.data.repositories

import com.hugodehu.gitmiro.domain.models.NotificationData
import com.hugodehu.gitmiro.domain.repositories.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class InMemoryNotificationsRepository @Inject constructor() : NotificationsRepository {
    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())

    override val notifications: StateFlow<List<NotificationData>> = _notifications

    override suspend fun emitNotification(notificationData: NotificationData) {
        _notifications.value += notificationData
    }
}
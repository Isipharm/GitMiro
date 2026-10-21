package com.hugodehu.gitmiro.domain.repositories

import com.hugodehu.gitmiro.domain.models.ProxySettings

interface SystemProxyRepository {
    suspend fun setProxy(proxySettings: ProxySettings)
    suspend fun clearProxy()
}
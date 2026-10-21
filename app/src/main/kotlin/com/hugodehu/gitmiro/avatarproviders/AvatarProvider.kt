package com.hugodehu.gitmiro.avatarproviders

interface AvatarProvider {
    fun getAvatarUrl(hashedEmail: String): String?
}
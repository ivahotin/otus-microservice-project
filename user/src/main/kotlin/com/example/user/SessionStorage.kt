package com.example.user

import java.time.Duration
import java.util.UUID
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

@Repository
class SessionStorage(private val redisTemplate: RedisTemplate<String, String>) {

    fun createSession(userId: UUID): String {
        while (true) {
            val sessionId = UUID.randomUUID().toString()
            val created = redisTemplate.opsForValue().setIfAbsent(
                sessionId,
                userId.toString(),
                Duration.ofMinutes(60)
            )
            if (created == true) return sessionId
        }
    }

    fun deleteSession(sessionId: String) {
        redisTemplate.delete(sessionId)
    }

    fun getUserIdBySessionId(sessionId: String): String? {
        return redisTemplate.opsForValue().get(sessionId)
    }
}
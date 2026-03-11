package com.parkit.analysis.parking.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.parkit.analysis.parking.repository.ParkingStepStateRepository
import com.parkit.analysis.parking.domain.ParkingStepState
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration

@Repository
class ParkingStepStateRedisRepository(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper
) : ParkingStepStateRepository {
    companion object {
        private const val PREFIX = "parking:state:"
        private val TTL = Duration.ofHours(1) // 주차 세션 정보 1시간 만료
    }

    private fun getKey(sessionId: String) = "$PREFIX$sessionId"

	override fun save(state: ParkingStepState): Mono<Boolean> {
        val key = getKey(state.sessionId)
        return try {
            val json = objectMapper.writeValueAsString(state)
            redisTemplate.opsForValue().set(key, json, TTL)
        } catch (e: Exception) {
            Mono.error(e)
        }
    }

	override fun findById(sessionId: String): Mono<ParkingStepState> {
        val key = getKey(sessionId)
        return redisTemplate.opsForValue().get(key)
            .map { json ->
                objectMapper.readValue(json, ParkingStepState::class.java)
            }
    }

	override fun deleteById(sessionId: String): Mono<Boolean> {
		return redisTemplate.delete(getKey(sessionId)).map { it > 0 }
	}
}
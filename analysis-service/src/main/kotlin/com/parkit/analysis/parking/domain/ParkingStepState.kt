package com.parkit.analysis.parking.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

/**
 * 실시간 주차 진행 상태를 관리하는 Redis/메모리 저장용 모델입니다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
data class ParkingStepState(
    val sessionId: String,
    var currentStep: Int = 1,
    var maxAbsHandleAngleInStep: Double = 0.0,
    var isCompleted: Boolean = false,
    var collisionDetected: Boolean = false,
    var totalScore: Double = 100.0,
    
    // (x, y) 좌표 시계열 배열 (Trajectory 평가용)
    val trajectory: MutableList<Coordinate> = mutableListOf(),
    
    // Yaw 계산을 위한 이전 상태 캐싱 (선택)
    var lastCoordinate: Coordinate? = null,
    
    val startTime: Instant = Instant.now(),
    var lastUpdateTime: Instant = Instant.now()
) {
    /**
     * 새로운 좌표 수신 시, Trajectory 기록 및 Max 조향각 갱신
     */
    fun updateWith(coord: Coordinate, handleAngle: Double) {
        trajectory.add(coord)
        lastCoordinate = coord
        
        val absHandle = Math.abs(handleAngle)
        if (absHandle > maxAbsHandleAngleInStep) {
            maxAbsHandleAngleInStep = absHandle
        }
        
        lastUpdateTime = Instant.now()
    }
    
    /**
     * Step 변경 (승격) 시 호출. 기록 초기화 및 스텝 수 증가
     */
    fun advanceToNextStep() {
        if (currentStep < 4) {
            currentStep++
            maxAbsHandleAngleInStep = 0.0
            trajectory.clear() // Step별 궤적을 평가하려면 비우고 새로 담음 (혹은 전체 보관)
        } else {
            isCompleted = true
        }
    }
}

data class Coordinate(
    val x: Double,
    val y: Double,
    val timestamp: Instant = Instant.now()
)

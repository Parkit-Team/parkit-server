package com.parkit.analysis.parking.domain

/**
 * T자 주차 각 Step의 기준점(Reference) 및 허용 오차(Tolerance)를 정의합니다.
 */
object ParkingReference {

    // 각 Step 별 목표(기준) 상태
    val STEP_1 = StepReference(
        x = 7.568, y = 0.661, z = -0.07,
        handleAngle = 0.0,
        steeringRange = -540.0..540.0,
        targetYaw = null // Step 1은 진입 단계이므로 yaw의 변화량으로 판단
    )

    val STEP_2 = StepReference(
        x = 15.103, y = -0.253, z = -0.07,
        handleAngle = 0.0,
        steeringRange = -540.0..540.0,
        targetYaw = null
    )

    val STEP_3 = StepReference(
        x = 14.568, y = -4.070, z = -0.07,
        handleAngle = 0.0,
        steeringRange = 0.0..540.0,
        targetYaw = null
    )

    val STEP_4 = StepReference(
        x = 13.407, y = -7.358, z = -0.07,
        handleAngle = 0.0,
        steeringRange = -540.0..540.0,
        targetYaw = null
    )

    fun getReferenceForStep(step: Int): StepReference? = when (step) {
        1 -> STEP_1
        2 -> STEP_2
        3 -> STEP_3
        4 -> STEP_4
        else -> null
    }

    // 허용 오차 (Tolerances)
    const val TOLERANCE_X = 1.0 // m
    const val TOLERANCE_Y = 1.0 // m
    const val TOLERANCE_Z = 0.5 // m
    const val TOLERANCE_HANDLE = 50.0 // 도(degree)
    const val TOLERANCE_YAW = 15.0 // 도(degree)

    // 충돌 판정 임계치 (Collision Threshold)
    const val COLLISION_DISTANCE_THRESHOLD = 0.5 // m (이하일 경우 충돌로 간주)
}

data class StepReference(
    val x: Double,
    val y: Double,
    val z: Double,
    val handleAngle: Double,
    val steeringRange: ClosedFloatingPointRange<Double>,
    val targetYaw: Double?
)

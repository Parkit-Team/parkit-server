package com.parkit.analysis.parking.domain

/**
 * T자 주차 각 Step의 기준점(Reference) 및 허용 오차(Tolerance)를 정의합니다.
 */
object ParkingReference {

    // 각 Step 별 목표(기준) 상태 (data/step*.csv 기반 정합성 확보)
    val STEP_1 = StepReference(
        x = 13.2505, y = -1.4371, z = -0.07,
        handleAngle = 0,
        steeringRange = -10.0..10.0,
        targetYaw = null
    )

    val STEP_2 = StepReference(
        x = 17.8162, y = 0.3827, z = -0.07,
        handleAngle = -540,
        steeringRange = -540.0..-530.0,
        targetYaw = null
    )

    val STEP_3 = StepReference(
        x = 13.4130, y = -6.3754, z = -0.07,
        handleAngle = 540,
        steeringRange = 530.0..540.0,
        targetYaw = null
    )

	val STEP_4 = StepReference(
		x = 13.4180, y = -8.3187, z = -0.07,
        handleAngle = 0,
        steeringRange = -10.0..10.0,
		targetYaw = null
	)

	data class StepStart(
		val x: Double,
		val y: Double,
	)

	/**
	 * 코칭용 기준값은 `data/step01.csv` ~ `data/step04.csv`를 기준으로 산출된 고정값입니다.
	 */
	fun coachingStepStart(step: Int): StepStart? = when (step) {
		1 -> StepStart(x = -5.415, y = -1.437)
		2 -> StepStart(x = 13.25, y = -1.437)
		3 -> StepStart(x = 17.81, y = 0.38)
		4 -> StepStart(x = 13.41, y = -6.37)
		else -> null
	}

	/**
	 * 코칭(프론트 표시)용 목표 조향각(deg). step 내에서 고정.
	 */
	fun coachingTargetAngleDeg(step: Int): Int = when (step) {
		1 -> 0
		2 -> -540
		3 -> 540
		4 -> 0
		else -> 0
	}

	/**
	 * 코칭(프론트 표시)용 목표 이동거리(m). step 내에서 고정.
	 */
    fun coachingTargetMoveDistanceM(step: Int, initialX: Double? = null): Double = when (step) {
        1 -> (13.25 - (initialX ?: -5.415)).coerceAtLeast(0.0)
        2 -> kotlin.math.hypot(17.81 - 13.25, 0.38 - (-1.437))
        3 -> kotlin.math.hypot(13.41 - 17.81, -6.37 - 0.38)
        4 -> kotlin.math.hypot(13.41 - 13.41, -8.31 - (-6.37))
		else -> 0.0
	}

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
    const val TOLERANCE_HANDLE = 10.0 // 도(degree)
    const val TOLERANCE_YAW = 15.0 // 도(degree)

    // 충돌 판정 임계치 (Collision Threshold)
    const val COLLISION_DISTANCE_THRESHOLD = 0.5 // m (이하일 경우 충돌로 간주)
}

data class StepReference(
    val x: Double,
    val y: Double,
    val z: Double,
    val handleAngle: Int,
    val steeringRange: ClosedFloatingPointRange<Double>,
    val targetYaw: Double?
)

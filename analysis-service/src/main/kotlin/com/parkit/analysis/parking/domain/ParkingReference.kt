package com.parkit.analysis.parking.domain

/**
 * T자 주차 각 Step의 기준점(Reference) 및 허용 오차(Tolerance)를 정의합니다.
 */
object ParkingReference {

    // 각 Step 별 목표(기준) 상태
    val STEP_1 = StepReference(
        x = 13.06, y = -1.435, z = -0.07,
        handleAngle = 0,
        steeringRange = -10.0..10.0,
        targetYaw = null
    )

    val STEP_2 = StepReference(
        x = 20.595, y = -2.349, z = -0.07,
        handleAngle = -540,
        steeringRange = -540.0..-530.0,
        targetYaw = null
    )

    val STEP_3 = StepReference(
        x = 20.06, y = -6.166, z = -0.07,
        handleAngle = 540,
        steeringRange = 530.0..540.0,
        targetYaw = null
    )

	val STEP_4 = StepReference(
		x = 18.899, y = -9.454, z = -0.07,
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
	 * (각 step 파일의 첫 좌표를 step 시작점으로 보고, 다음 step 파일의 첫 좌표를 종료점으로 봄)
	 */
	fun coachingStepStart(step: Int): StepStart? = when (step) {
		1 -> StepStart(x = -5.405, y = -1.435)
		2 -> StepStart(x = STEP_1.x, y = STEP_1.y)
		3 -> StepStart(x = STEP_2.x, y = STEP_2.y)
		4 -> StepStart(x = STEP_3.x, y = STEP_3.y)
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
        1 -> (STEP_1.x - (initialX ?: -5.405)).coerceAtLeast(0.0)
        2 -> kotlin.math.hypot(STEP_2.x - STEP_1.x, STEP_2.y - STEP_1.y)
        3 -> kotlin.math.hypot(STEP_3.x - STEP_2.x, STEP_3.y - STEP_2.y)
        4 -> kotlin.math.hypot(STEP_4.x - STEP_3.x, STEP_4.y - STEP_3.y)
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

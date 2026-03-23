package com.parkit.analysis.parking.domain

/**
 * T자 주차 각 Step의 기준점(Reference) 및 허용 오차(Tolerance)를 정의합니다.
 */
object ParkingReference {

    // 각 Step 별 목표(기준) 상태
    val STEP_1 = StepReference(
        x = 7.568, y = 0.661, z = -0.07,
        handleAngle = 0,
        steeringRange = 0.0..0.0,
        targetYaw = null // Step 1은 진입 단계이므로 yaw의 변화량으로 판단
    ) //360+180 = 540

    val STEP_2 = StepReference(
        x = 15.103, y = -0.253, z = -0.07,
        handleAngle = -540,
        steeringRange = -540.0..0.0, // 왼쪽
        targetYaw = null
    )

    val STEP_3 = StepReference(
        x = 14.568, y = -4.070, z = -0.07,
        handleAngle = 540, //todo 각도의 임계값
        steeringRange = 0.0..540.0, // 오른쪽
        targetYaw = null
    )

	val STEP_4 = StepReference(
		x = 13.407, y = -7.358, z = -0.07,
        handleAngle = 0,
        steeringRange = 0.0..0.0,
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
		// step1 is straight (x-only) with fixed y
		1 -> StepStart(x = -9.0, y = 1.015761)
		2 -> StepStart(x = 12.991654, y = -0.775463)
		3 -> StepStart(x = 18.481732, y = 1.884388)
		4 -> StepStart(x = 13.403275, y = -6.415625)
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
	 * 코칭(프론트 표시)용 목표 이동거리(cm). step 내에서 고정.
	 */
    fun coachingTargetMoveDistanceCm(step: Int): Int = when (step) {
        1 -> 2643
        2 -> 610
        3 -> 973
        4 -> 174
		else -> 0
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
    const val TOLERANCE_HANDLE = 50.0 // 도(degree)
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

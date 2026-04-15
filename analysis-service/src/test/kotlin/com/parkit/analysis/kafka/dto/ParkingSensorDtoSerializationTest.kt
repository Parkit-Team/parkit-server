package com.parkit.analysis.kafka.dto

import com.parkit.analysis.global.config.JacksonConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParkingSensorDtoSerializationTest {
	private val objectMapper = JacksonConfig().objectMapper()

	@Test
	fun `real sensor json deserializes into ParkingSensorDto`() {
		val json = """
			{
			  "time": 28.19,
			  "x": -5.405197,
			  "y": -1.435728,
			  "z": -0.073432,
			  "steer": 0.0,
			  "wheel_degree": 0,
			  "handle_angle": 0,
			  "speed": -0.0,
			  "front_dist": 6.841736959814851,
			  "left_dist": 6.8549553028926455,
			  "right_dist": 6.819816073111288,
			  "rear_dist": 6.269557738692705
			}
		""".trimIndent()

		val dto = objectMapper.readValue(json, ParkingSensorDto::class.java)

		// Use deltas for floating-point stability across platforms/JSON parsers.
		assertEquals(28.19, dto.time, 1e-12)
		assertEquals(-5.405197, dto.x, 1e-12)
		assertEquals(-1.435728, dto.y, 1e-12)
		assertEquals(-0.073432, dto.z, 1e-12)
		assertEquals(0.0, dto.steer, 1e-12)
		assertEquals(0.0, dto.wheelDegree, 1e-12)
		assertEquals(0.0, dto.handleAngle, 1e-12)
		assertEquals(0.0, dto.speed, 1e-12)
		assertEquals(6.841736959814851, dto.frontDist, 1e-12)
		assertEquals(6.8549553028926455, dto.leftDist, 1e-12)
		assertEquals(6.819816073111288, dto.rightDist, 1e-12)
		assertEquals(6.269557738692705, dto.rearDist, 1e-12)
	}
}

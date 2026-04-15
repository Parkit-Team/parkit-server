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

		assertEquals(28.19, dto.time)
		assertEquals(-5.405197, dto.x)
		assertEquals(-1.435728, dto.y)
		assertEquals(-0.073432, dto.z)
		assertEquals(0.0, dto.steer)
		assertEquals(0.0, dto.wheelDegree)
		assertEquals(0.0, dto.handleAngle)
		assertEquals(-0.0, dto.speed)
		assertEquals(6.841736959814851, dto.frontDist)
		assertEquals(6.8549553028926455, dto.leftDist)
		assertEquals(6.819816073111288, dto.rightDist)
		assertEquals(6.269557738692705, dto.rearDist)
	}
}
